package com.csl.intercom.cslscan.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.enums.ExportQueryStatus;
import com.csl.intercom.cslscan.enums.ImportQueryStatus;
import com.csl.intercom.cslscan.models.EntityHttpConnection;
import com.csl.intercom.cslscan.models.ExportQuery;
import com.csl.intercom.cslscan.models.ImportQuery;
import com.csl.intercom.dbapi.DbapiHandlerForCSLScan;
import com.csl.intercom.dbapi.models.HttpTemplateImportNotification;
import com.csl.logger.LoggerConstants;
import com.csl.logger.LoggerCustomEndpoints;
import com.csl.logger.LoggerInterfaces;
import com.csl.util.FileStorageService;
import com.csl.util.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Class for handling BSON file imports.
 * Should be able to handle multiple imports at the same time *via* scheduled tasks.
 */
public class ImportExportBsonService {
    static private final Logger logger = LoggerFactory.getLogger(ImportExportBsonService.class);
    static private ImportExportBsonService instance = null;
    private Map<Integer, ImportQuery> importTasks = new ConcurrentHashMap<>();
    private Map<Integer, ExportQuery> exportTasks = new ConcurrentHashMap<>();
    private DbapiHandlerForCSLScan dbapiHandler = null;
    private ScanApiHandler scanApiHandler = null;
    private FileStorageService fileStorageService = null;
    private final ScheduledExecutorService periodicHandleExecutor = new ScheduledThreadPoolExecutor(1);
    private final ScheduledExecutorService periodicStartExecutor = new ScheduledThreadPoolExecutor(1);

    private ImportExportBsonService() {
    }

    public static ImportExportBsonService getInstance() {
        if (instance == null) {
            instance = new ImportExportBsonService();
        }
        return instance;
    }

    public void init(DbapiHandlerForCSLScan dbapiHandler, ScanApiHandler scanApiHandler, FileStorageService fileStorageService) {
        if (this.dbapiHandler != null || this.scanApiHandler != null || this.fileStorageService != null) {
            logger.error("ImportBsonService.init: already initialized");
            return;
        }
        this.dbapiHandler = dbapiHandler;
        this.scanApiHandler = scanApiHandler;
        this.fileStorageService = fileStorageService;

        ThreadUtils.uncorrelatedSingleThreadScheduledAtFixedRate(
                this.periodicHandleExecutor,
                () -> {
                    for (int id : exportTasks.keySet()) {
                        handleExportTask(id);
                    }
                    for (int id : importTasks.keySet()) {
                        handleImportTask(id);
                    }
                },
                0, 5, TimeUnit.SECONDS,
                LoggerCustomEndpoints.HANDLE_IMPORT_EXPORT_TASKS, LoggerInterfaces.CSL_CLIENT);

        ThreadUtils.uncorrelatedSingleThreadScheduledAtFixedRate(
                this.periodicStartExecutor,
                () -> this.dbapiHandler.getAvailableImportTasks().forEach(this::startNewImportTask),
                1, 10, TimeUnit.MINUTES,
                LoggerCustomEndpoints.START_IMPORT_BSON_TASK, LoggerInterfaces.CSL_CLIENT);
    }

    /**
     * Create a new import task from a message received from either the HMI or MQTT.
     * Registers the import query in the list and sends the file to CSL-Scan.
     *
     * @param query The import query received from the HMI or MQTT.
     */
    public void startNewImportTask(HttpTemplateImportNotification query) {
        if (this.importTasks.containsKey(query.getId())) {
            logger.debug("startNewImportTask: uuid already exists: " + query.getId());
            return;
        }

        Path downloadedPath;
        ImportQuery importQuery;
        boolean shouldDrop = false;  // For future use
        try {
            logger.debug("startNewImportTask: downloading file: {}", query.getFileName());
            downloadedPath = this.dbapiHandler.downloadHttpTemplatesBsonFile(query);
            logger.debug("startNewImportTask: downloaded file: {}", query.getFileName());
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            logger.error("startNewImportTask: error downloading file: {}", query.getFileName(), e);
            return;
        }
        try {
            if (shouldDrop) {
                logger.debug("Removing all discovery protocols from DB-API");
                List<String> uuids = this.scanApiHandler.getAllEntityHttpConnectionsUuids();
                this.dbapiHandler.deleteDiscoveryProtocolsList(uuids);
            }
            logger.debug("startNewImportTask: importing file: {}", query.getFileName());

            importQuery = this.scanApiHandler.importBsonFile(downloadedPath, shouldDrop);
            logger.debug("startNewImportTask: sent file to CSL-Scan: {}", query.getFileName());
            this.dbapiHandler.notifyImportStarted(query.getId(), importQuery);
            logger.debug("startNewImportTask: notified DB-API: {}", query.getFileName());
        } catch (Exception e) {
            logger.error("startNewImportTask: error importing file: {}", query.getFileName(), e);
            return;
        }
        this.addImportTask(query.getId(), importQuery);
    }

    public int startNewExportTask() throws Exception {
        try {
            ExportQuery exportQuery = this.scanApiHandler.requestExportHttpTemplates();
            int id = this.dbapiHandler.requestBsonExportID(exportQuery);
            this.addExportTask(id, exportQuery);
            return id;
        } catch (Exception e) {
            logger.error("startNewExportTask: error exporting file", e);
            throw e;
        }
    }

    private void startAvailableTasksFromDbapi() {
        List<HttpTemplateImportNotification> notifications = this.dbapiHandler.getAvailableImportTasks();
        notifications.forEach(this::startNewImportTask);
    }

    private void addImportTask(int id, ImportQuery importQuery) {
        importTasks.put(id, importQuery);
    }

    private void addExportTask(int id, ExportQuery exportQuery) {
        exportTasks.put(id, exportQuery);
    }

    /**
     * Handle an import task.
     * If the task is finished, notify the DB-API and remove the task from the list.
     * If the task is still in progress, check the status and update the task in the list.
     *
     * @param id The ID of the import task.
     */
    private void handleImportTask(int id) {
        if (!importTasks.containsKey(id)) {
            logger.debug("handleImportTask: uuid not found: {}", id);
            return;
        }

        ImportQuery importQuery = importTasks.get(id);
        MDC.put(LoggerConstants.X_CORRELATION_ID, importQuery.getXCorrelationId()); // TODO: cleaning
        if (ImportQueryStatus.IN_PROGRESS_STATUSES.contains(importTasks.get(id).getStatus())) {
            logger.debug("handleImportTask: import in progress: {}", importQuery.getStatus());
            ImportQuery updatedImportQuery = this.scanApiHandler.getImportTaskStatus(importQuery.getId());
            if (updatedImportQuery != null) {
                importTasks.put(id, updatedImportQuery);
                importQuery = updatedImportQuery;
            }
        }

        if (ImportQueryStatus.FINISHED_STATUSES.contains(importQuery.getStatus())) {
            logger.info("Import of file finished: {}", importQuery.getStatus());
            this.dbapiHandler.notifyImportFinished(id, importQuery);
            List<EntityHttpConnection> entityHttpConnectionsToSync = this.scanApiHandler.getEntityHttpConnectionsToSync();
            entityHttpConnectionsToSync.forEach(template -> {
                try {
                    this.dbapiHandler.updateDiscoveryProtocol(template);
                } catch (Exception e) {
                    logger.error("handleImportTask: error syncing template: {}", template.getName(), e);
                }
            });
            this.scanApiHandler.notifyEntityHttpConnectionSynchronized(entityHttpConnectionsToSync);
            this.importTasks.remove(id);
        }
        MDC.remove(LoggerConstants.X_CORRELATION_ID); // TODO: cleaning
    }

    private void handleExportTask(int id) {
        if (!exportTasks.containsKey(id)) {
            logger.debug("handleExportTask: uuid not found: {}", id);
            return;
        }

        ExportQuery exportQuery = exportTasks.get(id);
        MDC.put(LoggerConstants.X_CORRELATION_ID, exportQuery.getXCorrelationId()); // TODO: cleaning
        if (ExportQueryStatus.IN_PROGRESS_STATUSES.contains(exportQuery.getStatus())) {
            logger.debug("handleExportTask: export in progress: {}", exportQuery.getStatus());
            ExportQuery updatedExportQuery = this.scanApiHandler.getExportQueryStatus(exportQuery);
            if (updatedExportQuery != null) {
                exportTasks.put(id, updatedExportQuery);
                exportQuery = updatedExportQuery;
            }
        }

        if (ExportQueryStatus.FINISHED_STATUSES.contains(exportQuery.getStatus())) {
            logger.info("Export of file finished: {}", exportQuery.getStatus());
            try {
                this.scanApiHandler.downloadExportFile(exportQuery);
                this.scanApiHandler.deleteExportFile(exportQuery);
                this.dbapiHandler.uploadHttpTemplatesBsonFile(id, exportQuery);
                this.dbapiHandler.notifyExportFinished(id, exportQuery);
                this.fileStorageService.deleteFile(exportQuery.getFilename());
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                logger.error("handleExportTask: error downloading file: {}", exportQuery.getFilename(), e);
            } catch (Exception e) {
                logger.error("handleExportTask: error uploading file: {}", exportQuery.getFilename(), e);
            }
            this.exportTasks.remove(id);
        }
        MDC.remove(LoggerConstants.X_CORRELATION_ID); // TODO: cleaning
    }

    /**
     * Update the status of an import task.
     *
     * @param uuid   The UUID of the import task.
     * @param status The new status of the import task.
     */
    public void updateImportQueryStatus(UUID uuid, ImportQueryStatus status) {
        Integer id = getImportTaskByUuid(uuid);
        if (id != null) {
            ImportQuery importQuery = importTasks.get(id);
            importQuery.setStatus(status);
            handleImportTask(id);
        } else {
            logger.debug("updateQueryStatus: uuid not found: " + uuid);
        }
    }

    /**
     * Update the status of an export task.
     *
     * @param uuid   The UUID of the export task.
     * @param status The new status of the export task.
     */
    public void updateExportQueryStatus(UUID uuid, ExportQueryStatus status) {
        Integer id = getExportTaskByUuid(uuid);
        if (id != null) {
            ExportQuery exportQuery = exportTasks.get(id);
            exportQuery.setStatus(status);
            handleExportTask(id);
        } else {
            logger.debug("updateExportQueryStatus: uuid not found: " + uuid);
        }
    }

    /**
     * Get the ID of an import task from its UUID.
     *
     * @param uuid The UUID of the import task.
     * @return The ID of the import task, or null if the UUID is not found.
     */
    private Integer getImportTaskByUuid(UUID uuid) {
        for (int id : importTasks.keySet()) {
            if (importTasks.get(id).getId().equals(uuid)) {
                return id;
            }
        }
        return null;
    }

    /**
     * Get the ID of an export task from its UUID.
     *
     * @param uuid The UUID of the export task.
     * @return The ID of the export task, or null if the UUID is not found.
     */
    private Integer getExportTaskByUuid(UUID uuid) {
        for (int id : exportTasks.keySet()) {
            if (exportTasks.get(id).getId().equals(uuid)) {
                return id;
            }
        }
        return null;
    }
}
