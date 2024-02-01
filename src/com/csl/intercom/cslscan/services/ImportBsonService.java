package com.csl.intercom.cslscan.services;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.enums.ImportQueryStatus;
import com.csl.intercom.cslscan.models.EntityHttpConnection;
import com.csl.intercom.cslscan.models.ImportQuery;
import com.csl.intercom.dbapi.DbapiHandler;
import com.csl.intercom.dbapi.models.HttpTemplateImportNotification;
import com.csl.util.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Class for handling BSON file imports.
 * Should be able to handle multiple imports at the same time *via* scheduled tasks.
 */
public class ImportBsonService {
    static private final Logger logger = LoggerFactory.getLogger(ImportBsonService.class);
    static private ImportBsonService instance = null;
    private Map<Integer, ImportQuery> importTasks = new ConcurrentHashMap<>();
    private DbapiHandler dbapiHandler = null;
    private ScanApiHandler scanApiHandler = null;
    private FileStorageService fileStorageService = null;
    private final ScheduledExecutorService periodicHandleExecutor = new ScheduledThreadPoolExecutor(1);
    private final ScheduledExecutorService periodicStartExecutor = new ScheduledThreadPoolExecutor(1);

    private ImportBsonService() {
    }

    public static ImportBsonService getInstance() {
        if (instance == null) {
            instance = new ImportBsonService();
        }
        return instance;
    }

    public void init(DbapiHandler dbapiHandler, ScanApiHandler scanApiHandler, FileStorageService fileStorageService) {
        if (this.dbapiHandler != null || this.scanApiHandler != null || this.fileStorageService != null) {
            logger.error("ImportBsonService.init: already initialized");
            return;
        }
        this.dbapiHandler = dbapiHandler;
        this.scanApiHandler = scanApiHandler;
        this.fileStorageService = fileStorageService;

        this.periodicHandleExecutor.scheduleAtFixedRate(() -> {
            for (int id : importTasks.keySet()) {
                handleImportTask(id);
            }
        }, 0, 5, TimeUnit.SECONDS);
        this.periodicStartExecutor.scheduleAtFixedRate(() -> this.dbapiHandler.getAvailableImportTasks().forEach(this::startNewImportTask),
                0, 10, TimeUnit.MINUTES);
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
        try {
            logger.debug("startNewImportTask: downloading file: {}", query.getFileName());
            downloadedPath = this.dbapiHandler.downloadHttpTemplatesBsonFile(query);
            logger.debug("startNewImportTask: downloaded file: {}", query.getFileName());
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            logger.error("startNewImportTask: error downloading file: {}", query.getFileName(), e);
            return;
        }
        try {
            logger.debug("startNewImportTask: importing file: {}", query.getFileName());
            importQuery = this.scanApiHandler.importBsonFile(downloadedPath);
            logger.debug("startNewImportTask: sent file to CSL-Scan: {}", query.getFileName());
            this.dbapiHandler.notifyImportStarted(query.getId(), importQuery);
            logger.debug("startNewImportTask: notified DB-API: {}", query.getFileName());
        } catch (Exception e) {
            logger.error("startNewImportTask: error importing file: {}", query.getFileName(), e);
            return;
        }
        this.addImportTask(query.getId(), importQuery);
    }

    private void startAvailableTasksFromDbapi() {
        List<HttpTemplateImportNotification> notifications = this.dbapiHandler.getAvailableImportTasks();
        notifications.forEach(this::startNewImportTask);
    }

    private void addImportTask(int id, ImportQuery importQuery) {
        importTasks.put(id, importQuery);
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
            logger.debug("handleImportTask: uuid not found: " + id);
            return;
        }

        ImportQuery importQuery = importTasks.get(id);
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
    }

    /**
     * Update the status of an import task.
     *
     * @param uuid   The UUID of the import task.
     * @param status The new status of the import task.
     */
    public void updateQueryStatus(UUID uuid, ImportQueryStatus status) {
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
}
