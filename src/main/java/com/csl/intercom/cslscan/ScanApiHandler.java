package com.csl.intercom.cslscan;

import com.csl.core.Config;
import com.csl.intercom.cslscan.enums.DynamicDiscoveryFrequencyOption;
import com.csl.intercom.cslscan.enums.ScanApiEndpoint;
import com.csl.intercom.cslscan.enums.ScanCollection;
import com.csl.intercom.cslscan.models.*;
import com.csl.intercom.cslscan.models.scans.ExternalScan;
import com.csl.intercom.dbapi.models.*;
import com.csl.util.FileStorageService;
import com.csl.util.ListUtils;
import com.csl.util.Pair;
import com.csl.web.apiclient.ApiHandler;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.JsonApiResponse;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.*;
import org.eclipse.jetty.http.HttpMethod;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static com.csl.logger.LoggerConstants.X_CORRELATION_ID;

/**
 * Class to handle communication with CSL-Scan's HTTP API.
 */
public class ScanApiHandler extends ApiHandler {
    private static final Logger logger = LoggerFactory.getLogger(ScanApiHandler.class);
    public static final String CONTENT = "content";
    public static final String STATUS_CODE = "status_code";
    public static final String LIMIT = "limit";
    public static final String VALUE = "value";
    public static final String COULD_NOT_GET_THE_DISCOVERY_CRON_STATUS = "Could not get the discovery cron status: ";
    public static final String FREQUENCY = "frequency";
    private final FileStorageService fileStorageService = new FileStorageService();

    public ScanApiHandler() {
        super("CSL-Scan", Config.INSTANCE.scan.getManagerIp(), Config.INSTANCE.scan.getManagerPort(), Config.INSTANCE.scan.isUseSsl());
        addUriCommonPath("/api");
    }

    /**
     * Create a new device in the scanner
     *
     * @param device A {@link Json} containing the device to add. Fields 'id', 'name' and 'ip' are required.
     * @return A {@link Json} containing the newly created device, as handed by the scanner
     */
    public JsonApiResponse addEntity(Device device) {
        return sendPost(ScanApiEndpoint.ENTITY, device.serializeForScanner());
    }

    /**
     * Add a connection to the scanner.
     *
     * @param connection The connection to add.
     * @return The response from the scanner.
     */
    public JsonApiResponse addConnectionInfo(Connection connection) {
        return sendPost(ScanApiEndpoint.CONNECTIONS, connection.serializeForScanner());
    }

    public JsonApiResponse deleteConnectionInfo(String connectionUuid) {
        return sendDelete(String.format(ScanApiEndpoint.CONNECTIONS_DETAILS.endpoint(), connectionUuid), Json.object());
    }

    public JsonApiResponse updateConnectionInfo(Connection connection) {
        return sendPost(String.format(ScanApiEndpoint.CONNECTIONS_DETAILS.endpoint(), connection.getUuid()), connection.serializeForScanner());
    }

    public JsonApiResponse addListOfConnectionInfoDrafts(List<EntityConnectionInfoDraft> entityConnectionInfoDrafts) {
        Json entityConnectionInfoDraftsJson = Json.array();
        for (EntityConnectionInfoDraft entityConnectionInfoDraft : entityConnectionInfoDrafts) {
            entityConnectionInfoDraftsJson.add(entityConnectionInfoDraft.serializeForScanner());
        }
        return sendPost(ScanApiEndpoint.CREATE_CONNECTIONS_DRAFT, entityConnectionInfoDraftsJson);
    }

    public JsonApiResponse deleteConnectionDraft(String connectionDraftUuid) {
        return sendDelete(String.format(ScanApiEndpoint.CONNECTION_DRAFT_DETAILS.endpoint(), connectionDraftUuid), Json.object());
    }

    public JsonApiResponse updateConnectionDraft(EntityConnectionInfoDraft entityConnectionInfoDraft) {
        return sendPost(String.format(ScanApiEndpoint.CONNECTION_DRAFT_DETAILS.endpoint(), entityConnectionInfoDraft.getUuid()), entityConnectionInfoDraft.serializeForScanner());
    }

    public JsonApiResponse clearFailedConnectionsDraft() {
        return sendDelete(ScanApiEndpoint.CLEAR_FAILED_CONNECTIONS_DRAFT, Json.object());
    }

    public JsonApiResponse clearVerifiedConnectionsDraft() {
        return sendDelete(ScanApiEndpoint.CLEAR_VERIFIED_CONNECTIONS_DRAFT, Json.object());
    }

    public JsonApiResponse clearAllConnectionsDraft() {
        return sendDelete(ScanApiEndpoint.CLEAR_ALL_CONNECTIONS_DRAFT, Json.object());
    }

    public JsonApiResponse publishAllVerifiedConnectionsDraft() {
        return sendPost(ScanApiEndpoint.PUBLISH_ALL_VERIFIED_CONNECTION_DRAFT, Json.object());
    }

    /**
     * Get connections since a specified date.
     * If the date is null, all connections are returned.
     *
     * @param date The date to start receiving notifications.
     * @return A list of connections that have changed since the specified date.
     */
    public List<Connection> getConnectionsSince(OffsetDateTime date, Integer limit, Integer offset, List<ConnectionProtocol> protocols) {
        Json params = Json.object();
        if (date != null) {
            params.set("since", ScanUtils.localTimeToScan(date).toString());
        }
        if (limit != null) {
            params.set(LIMIT, limit);
        }
        if (offset != null) {
            params.set("skip", offset);
        }
        JsonApiResponse response = sendGet(ScanApiEndpoint.CONNECTIONS, params);
        if (!response.isSuccess()) {
            return null;
        }
        return ListUtils.toList(response.getResult().asJsonList().stream()
                .map(connectionJson -> Connection.fromScannerJson(connectionJson, protocols))
                .filter(Objects::nonNull)
        );
    }

    public OffsetDateTime getConnectionLastUpdatedDate() {
        JsonApiResponse response = sendGet(ScanApiEndpoint.CONNECTIONS_LAST_UPDATE, Json.object());
        try {
            String dateString = response.getResult().get(VALUE).asString().replace("\"", "");
            return ScanUtils.scanTimeToLocal(OffsetDateTime.parse(dateString));
        } catch (NullPointerException e) {
            return null;
        }
    }

    public JsonApiResponse clearAllEntityConnections() {
        return sendDelete(ScanApiEndpoint.CLEAR_ALL_ENTITY_CONNECTIONS, Json.object());
    }

    /**
     * Get the list of configured entities in the scanner.
     *
     * @return A {@link Json} array containing the all the configured entities in the scanner.
     */
    public JsonApiResponse listEntities() {
        return sendGet(ScanApiEndpoint.ENTITY, Json.object());
    }

    /**
     * Get a specific entity.
     *
     * @param id The unique identifier created by the scanner, as returned at creation or in a list.
     * @return A {@link Json} containing the specified entity.
     */
    public JsonApiResponse getEntity(String id) {
        JsonApiResponse response = sendGet(
                String.format(ScanApiEndpoint.ENTITY_DETAILS.endpoint(), id), Json.object());
        int statusCode;
        try {
            statusCode = response.getExtra().get(STATUS_CODE).asInteger();
        } catch (Exception e) {
            return response;
        }
        if (statusCode != 200) {
            return JsonApiResponse.error("Could not get the entity" + id);
        } else {
            return response;
        }
    }

    /**
     * Change fields in an already existing device, overwriting every field.
     *
     * @param device A {@link Json} with the device's new information.
     * @return The old version of the device, not reflecting the changes made.
     */
    public JsonApiResponse updateEntity(Device device) {
        return addEntity(device);
    }

    public JsonApiResponse createOrUpdateEntity(Device device) {
        JsonApiResponse response = addEntity(device);
        if (!response.isSuccess()) {
            response = updateEntity(device);
        }
        return response;
    }

    public List<String> deleteMultipleEntities(List<Pair<String, OffsetDateTime>> deletedDevices, boolean hardDelete) throws Exception {
        List<String> failedDevices = new ArrayList<>();
        boolean hasFailed = false;
        OffsetDateTime maxDate = OffsetDateTime.MIN;
        // get the list of uuids from deletedDevices
        List<String> uuids = new ArrayList<>();
        for (Pair<String, OffsetDateTime> deletedDevice : deletedDevices) {
            uuids.add(deletedDevice.getFirst());
        }
        // Delete devices from CSL-Scan
        try {
            JsonApiResponse response = sendDelete(ScanApiEndpoint.ENTITY_DELETE_MULTIPLE_ENTITIES, Json.object("uuids", uuids, "hardDelete", hardDelete));
            if (!response.isSuccess()) {
                throw new Exception("Could not delete the entities from CSL-Scan");
            } else { // just getting the max deletion date based on the deleted devices list
                for (Pair<String, OffsetDateTime> deletedDevice : deletedDevices) {
                    OffsetDateTime deletionDate = deletedDevice.getSecond();
                    if (deletionDate.isAfter(OffsetDateTime.MIN)) {
                        maxDate = deletionDate;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Could not delete the entities from CSL-Scan", e);
            return uuids;
        }
        // If the devices were deleted successfully, and at least one was deleted, update the last entities deletion date
        if (!deletedDevices.isEmpty() && !hasFailed) {
            setLastEntitiesDeletionDate(maxDate);
        }
        return failedDevices;
    }

    public List<String> deleteEntities(List<Pair<String, OffsetDateTime>> deletedDevices,boolean hardDelete) throws Exception {
        OffsetDateTime maxDate = OffsetDateTime.MIN;
        List<String> failedDevices = new ArrayList<>();
        boolean hasFailed = false;

        // Delete devices from CSL-Scan and get the max deletion date
        for (Pair<String, OffsetDateTime> deletedDevice : deletedDevices) {
            String uuid = deletedDevice.getFirst();
            OffsetDateTime deletionDate = deletedDevice.getSecond();
            try {
                deleteEntity(uuid, hardDelete);
                if (deletionDate.isAfter(maxDate)) {
                    maxDate = deletionDate;
                }
            } catch (Exception e) {
                failedDevices.add(uuid);
                hasFailed = true;
            }
        }

        // If the devices were deleted successfully, and at least one was deleted, update the last entities deletion date
        if (!deletedDevices.isEmpty() && !hasFailed) {
            setLastEntitiesDeletionDate(maxDate);
        }
        return failedDevices;
    }

    /**
     * Delete an entity from the scanner.
     *
     * @param id The unique identifier of the entity, as returned at creation or in a list.
     * @param hardDelete: boolean, to hard delete entities in scan or not
     * @return An empty object on success, an error message on failure.
     */
    public JsonApiResponse deleteEntity(String id, Boolean hardDelete) {
        JsonApiResponse response = sendDelete(
                String.format(ScanApiEndpoint.ENTITY_DETAILS.endpoint(), id, hardDelete), Json.object());
        boolean success = response.isSuccess();
        if (success) {
            if (response.getExtra().get(STATUS_CODE).asInteger() == 404) {
                return JsonApiResponse.error("Could not find entity " + id + " in CSL-Scan (got 404)");
            }
            return JsonApiResponse.success();
        } else {
            return JsonApiResponse.error("Could not delete the entity " + id + " from CSL-Scan");
        }
    }

    /**
     * Get all the SNMP objects discovered on a particular entity.
     *
     * @param id The unique identifier of the entity, as returned at creation or in a list.
     * @return A {@link Json} array containing all the SNMP objects discovered so far by the scanner on this entity.
     */
    public JsonApiResponse getEntityCpes(String id) {
        return sendGet(String.format(ScanApiEndpoint.ENTITY_CPE_ITEMS.endpoint(), id), Json.object());
    }

    /**
     * Get the status of a specific scan task.
     *
     * @param id The unique id of the task.
     * @return The status of the scan.
     */
    public JsonApiResponse getScanStatus(String id) {
        return sendGet(String.format(ScanApiEndpoint.DISCOVERY_STATUS_DETAILS.endpoint(), id), Json.object());
    }

    /**
     * Get the status of an entity's scan
     *
     * @param id The entity's unique id.
     * @return The status of the scan.
     */
    public JsonApiResponse getEntityScanStatus(String id) {
        return sendGet(String.format(ScanApiEndpoint.ENTITY_SCAN_STATUS.endpoint(), id), Json.object());
    }

    /**
     * Fetch CSL-Scan's status.
     *
     * @return A {@link JsonApiResponse} with CSL-Scan's status as it was received, or with an error in the 'error' field.
     */
    public JsonApiResponse getStatus() {
        return sendGet(ScanApiEndpoint.DISCOVERY_STATUS.endpoint(), Json.object(),true);
    }

    /**
     * Test if an existing connection is valid.
     *
     * @param deviceUuid     The uuid of the device to test.
     * @param connectionUuid The uuid of the connection to test.
     * @return A {@link JsonApiResponse} with CSL-Scan's response.
     */
    public JsonApiResponse testConnection(String deviceUuid, String connectionUuid, String connectionId) {
        return sendGet(String.format(ScanApiEndpoint.ENTITY_TEST_EXISTING_CONNECTION.endpoint(), deviceUuid),
                Json.object("connection_uuid", connectionUuid, "connection_id", connectionId));
    }

    /**
     * Test if a connection is valid.
     * The connection does not need to exist in CSL-Scan.
     *
     * @param device The device to test. We assume it contains a connection. Only the first connection will be tested.
     * @return A {@link JsonApiResponse} with CSL-Scan's response.
     */
    public JsonApiResponse testConnection(Device device) {
        return sendPost(
                ScanApiEndpoint.ENTITY_TEST_CONNECTION,
                device.serializeForScanner());
    }

    /**
     * Request the deletion of a CPE Item to CSL-Scan.
     *
     * @param id The uuid of the CPE Item to delete.
     */
    public void deleteCpeItemFromScan(String id) {
        sendDelete(
                String.format(ScanApiEndpoint.CPE_ITEM_DETAILS.endpoint(), id), Json.object());
    }

    /**
     * Request multiple deletions of CPE Items to CSL-Scan.
     *
     * @param deletedCpes The list of CPE Items to delete.
     * @param hardDelete  Whether to hard-delete the CPE Items (i.e. actually remove them from the DB)
     *                    or to soft-delete them (i.e. mark them as deleted in the DB).
     */
    public void deleteCpeItemsFromScan(List<Pair<String, OffsetDateTime>> deletedCpes, boolean hardDelete) {
        AtomicReference<OffsetDateTime> maxDate = new AtomicReference<>(OffsetDateTime.MIN);

        Json deletedCpeItemsIdsArray = Json.array();
        deletedCpes.stream().peek(deletedCpe -> {
                    OffsetDateTime date = deletedCpe.getSecond();
                    if (date != null && date.isAfter(maxDate.get())) {
                        maxDate.set(date);
                    }
                })
                .map(Pair::getFirst)
                .forEach(deletedCpeItemsIdsArray::add);

        ScanApiEndpoint endpoint = hardDelete ? ScanApiEndpoint.CPE_ITEM_DELETE_MANY_HARD : ScanApiEndpoint.CPE_ITEM_DELETE_MANY;
        sendPost(endpoint, deletedCpeItemsIdsArray);

        if (!deletedCpes.isEmpty()) {
            try {
                setLastCpeItemsDeletionDate(maxDate.get());
            } catch (Exception e) {
                logger.error("Could not set the last CPE items deletion date", e);
            }
        }
    }

    public void deleteCpeItemsBeforeDate(OffsetDateTime date, boolean deleteAll) {
        sendDelete(ScanApiEndpoint.CPE_ITEM_HARD_DELETE_BEFORE, Json.object("date", date.toString(), "deleteAll", deleteAll));
    }

    /**
     * Request the deletion of a {@link MicrosoftKB} to CSL-Scan.
     *
     * @param id The uuid of the {@link MicrosoftKB} to delete.
     */
    public void deleteMicrosoftKBFromScan(String id) {
        sendDelete(
                String.format(ScanApiEndpoint.MICROSOFT_KB_DETAILS.endpoint(), id), Json.object());
    }

    /**
     * Request multiple deletions of {@link MicrosoftKB} to CSL-Scan.
     *
     * @param deletedMicrosoftKBs The list of {@link MicrosoftKB} to delete.
     * @param hardDelete          Whether to hard-delete the {@link MicrosoftKB} (i.e. actually remove them from the DB)
     *                            or to soft-delete them (i.e. mark them as deleted in the DB).
     */
    public void deleteMicrosoftKBsFromScan(List<Pair<String, OffsetDateTime>> deletedMicrosoftKBs, boolean hardDelete) {
        AtomicReference<OffsetDateTime> maxDate = new AtomicReference<>(OffsetDateTime.MIN);

        Json deletedCpeItemsIdsArray = Json.array();
        deletedMicrosoftKBs.stream().peek(deletedCpe -> {
                    OffsetDateTime date = deletedCpe.getSecond();
                    if (date != null && date.isAfter(maxDate.get())) {
                        maxDate.set(date);
                    }
                })
                .map(Pair::getFirst)
                .forEach(deletedCpeItemsIdsArray::add);

        ScanApiEndpoint endpoint = hardDelete ? ScanApiEndpoint.MICROSOFT_KB_DELETE_MANY_HARD : ScanApiEndpoint.MICROSOFT_KB_DELETE_MANY;
        sendPost(endpoint, deletedCpeItemsIdsArray);

        if (!deletedMicrosoftKBs.isEmpty()) {
            try {
                setLastMicrosoftKbsDeletionDate(maxDate.get());
            } catch (Exception e) {
                logger.error("Could not set the last CPE items deletion date", e);
            }
        }
    }

    public void deleteMicrosoftKBsBeforeDate(OffsetDateTime date, boolean deleteAll) {
        sendDelete(ScanApiEndpoint.MICROSOFT_KB_HARD_DELETE_BEFORE, Json.object("date", date.toString(), "deleteAll", deleteAll));
    }

    /**
     * Get the CPE items that have changed since the specified date.
     *
     * @param date The date to start receiving notifications. May be null to retrieve all the items.
     * @return A {@link List<MicrosoftKB>} array containing the CPE items that have changed since the specified date, or all the items if date was null.
     */
    public List<CpeItem> getCpeItemChangesSince(OffsetDateTime date, int limit, int offset) {
        JsonApiResponse response;
        Json cpeItems = Json.array();
        if (date == null) {
            response = sendGet(ScanApiEndpoint.CPE_ITEM, Json.object(LIMIT, limit, "skip", offset));
        } else {
            response = sendGet(ScanApiEndpoint.CPE_ITEM, Json.object("date", ScanUtils.localTimeToScan(date).toString(), LIMIT, limit, "skip", offset));
        }
        if (response.isSuccess() && response.getExtra().get(STATUS_CODE).asInteger() == 200) {
            cpeItems = response.getResult();

            // Parse the items, filter those whose updated date is *exactly* the last updated date, and return the resulting list.
            return ListUtils.toList(
                    cpeItems.asJsonList().stream()
                    .map(CpeItem::fromScannerJson)
                    .filter(Predicate.not(cpeItem -> cpeItem.getDiscoveredDate().equals(date)))
            );
        } else {
            return null;
        }
    }

    public List<CpeItem> getCpeItemChangesSince(OffsetDateTime date) {
        return getCpeItemChangesSince(date, 0, 0);
    }

    /**
     * Get the KBs that have changed since the specified date.
     *
     * @param date The date to start receiving notifications. May be null to retrieve all the items.
     * @return A {@link List<MicrosoftKB>} containing the KBs that have changed since the specified date, or all the items if date was null.
     */
    public List<MicrosoftKB> getMicrosoftKbChangesSince(OffsetDateTime date, int limit, int offset) {
        JsonApiResponse response;
        if (date == null) {
            response = sendGet(ScanApiEndpoint.MICROSOFT_KB, Json.object(LIMIT, limit, "skip", offset));
        } else {
            response = sendGet(ScanApiEndpoint.MICROSOFT_KB, Json.object("date", ScanUtils.localTimeToScan(date).toString(), LIMIT, limit, "skip", offset));
        }
        if (response.isSuccess() && response.getExtra().get(STATUS_CODE).asInteger() == 200) {
            Json microsoftKbs = response.getResult();
            return ListUtils.toList(microsoftKbs.asJsonList().stream()
                    .map(MicrosoftKB::fromScannerJson)
                    .filter(Predicate.not(microsoftKB -> microsoftKB.getDiscoveredDate().equals(date)))
            );
        } else {
            return null;
        }
    }

    public List<MicrosoftKB> getMicrosoftKbChangesSince(OffsetDateTime date) {
        return getMicrosoftKbChangesSince(date, 0, 0);
    }

    /**
     * Contact the scan service to retrieve an entity through by its name.
     * Stops at the first match, thus ignores extra results if the name is duplicated.
     *
     * @param name The name to seek in the entities
     * @return null if the scan service is down, "" if the name was not found.
     */
    public Json getEntityByName(String name) {
        Json entities = listEntities().getResult();
        if (entities == null || !entities.isArray()) {
            return null;
        }
        Json result = Json.object();
        for (Json entity : entities.asJsonList()) {
            if (JsonUtil.getStringFromJson(entity, "name", "").equals(name)) {
                result = entity;
            }
        }
        return result;
    }

    /**
     * Contact the scan service to retrieve an entity through by its IP address.
     * Stops at the first match, thus ignores extra results if the IP address is duplicated.
     *
     * @param ipAddress The IP address to seek in the entities
     * @return null if the scan service is down, "" if the address was not found.
     */
    public Json getEntityByIp(String ipAddress) {
        Json entities = listEntities().getResult();
        if (entities == null || !entities.isArray()) {
            return null;
        }
        Json result = Json.object();
        for (Json entity : entities.asJsonList()) {
            if (JsonUtil.getStringFromJson(entity, "ipAddress", "").equals(ipAddress)) {
                result = entity;
            }
        }
        return result;
    }

    /**
     * Get the list of EntityHttpConnection objects from CSL-Scan.
     *
     * @return A {@link List<EntityHttpConnection>} containing all the EntityHttpConnection objects from CSL-Scan.
     */
    public List<EntityHttpConnection> getAllEntityHttpConnections(boolean visibleOnly) {
        JsonApiResponse response = sendGet(
                ScanApiEndpoint.ENTITY_HTTP_CONNECTION, Json.object("visibleOnly", visibleOnly));
        if (response.isSuccess() && response.getExtra().get(STATUS_CODE).asInteger() == 200) {
            return ListUtils.toList(
                    response.getResult().asJsonList().stream()
                    .map(EntityHttpConnection::fromScannerJson)
            );
        } else {
            logger.warn("Could not get all entity http connections from CSL-Scan");
            return null;
        }
    }

    public List<String> getAllEntityHttpConnectionsUuids() {
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.GET,
                ScanApiEndpoint.ENTITY_HTTP_CONNECTION_UUIDS, Json.object());
        if (response.isSuccess() && response.getExtra().get(STATUS_CODE).asInteger() == 200) {
            return ListUtils.toList(response.getResult().asJsonList().stream()
                    .map(Json::asString)
            );
        } else {
            return null;
        }
    }

    /**
     * Get an {@link EntityHttpConnection} object from CSL-Scan from its uuid.
     *
     * @param uuid The uuid of the EntityHttpConnection object to retrieve.
     * @return The {@link EntityHttpConnection} object from CSL-Scan with the specified uuid.
     */
    public EntityHttpConnection getEntityHttpConnection(String uuid, boolean visibleOnly) {
        JsonApiResponse response = sendGet(String.format(ScanApiEndpoint.ENTITY_HTTP_CONNECTION_DETAILS.endpoint(), uuid), Json.object("visibleOnly", visibleOnly));
        if (response.isSuccess() && response.getExtra().get(STATUS_CODE).asInteger() == 200) {
            return EntityHttpConnection.fromScannerJson(response.getResult());
        } else {
            logger.warn("Could not get entity http connection {} from CSL-Scan", uuid);
            return null;
        }
    }

    /**
     * Delete an {@link EntityHttpConnection} object from CSL-Scan from its uuid.
     *
     * @param uuid The uuid of the EntityHttpConnection object to delete.
     * @return The {@link JsonApiResponse} from CSL-Scan.
     */
    public JsonApiResponse deleteEntityHttpConnection(String uuid) {
        return sendDelete(
                String.format(ScanApiEndpoint.ENTITY_HTTP_CONNECTION_DETAILS.endpoint(), uuid), Json.object());
    }

    public JsonApiResponse createEntityHttpConnection(EntityHttpConnection entityHttpConnection) {
        JsonApiResponse response = sendPost(ScanApiEndpoint.ENTITY_HTTP_CONNECTION, entityHttpConnection.serializeForScanner());
        if (response.isSuccess() && response.getExtra().get(STATUS_CODE).asInteger() == 200) {
            EntityHttpConnection createdEntityHttpConnection = EntityHttpConnection.fromScannerJson(response.getResult());
            if (createdEntityHttpConnection != null) {
                logger.debug("Created entity http connection {}", createdEntityHttpConnection.getUuid());
                return JsonApiResponse.result(createdEntityHttpConnection.serializeForDbapi(), response.getExtra());
            } else {
                logger.error("Could not create the entity http connection (could not parse response from CSL-Scan)");
                return JsonApiResponse.error("Could not create the entity http connection", Json.object("error", "Could not deserialize the entity http connection"));
            }
        } else {
            logger.error("Could not create the entity http connection {}", response.getError().getDetails());
            return JsonApiResponse.error("Could not create the entity http connection", response.getError().getDetails());
        }
    }

    public JsonApiResponse updateEntityHttpConnection(EntityHttpConnection entityHttpConnection) {
        JsonApiResponse response = sendPut(
                String.format(ScanApiEndpoint.ENTITY_HTTP_CONNECTION_DETAILS.endpoint(), entityHttpConnection.getUuid()),
                entityHttpConnection.serializeForScanner());
        if (response.isSuccess() && response.getExtra().get(STATUS_CODE).asInteger() == 200) {
            EntityHttpConnection createdEntityHttpConnection = EntityHttpConnection.fromScannerJson(response.getResult());
            if (createdEntityHttpConnection != null) {
                logger.info("Updated entity http connection {}", createdEntityHttpConnection.getUuid());
                return JsonApiResponse.result(createdEntityHttpConnection.serializeForDbapi(), response.getExtra());
            } else {
                logger.error("Could not update the entity http connection (could not parse response from CSL-Scan)");
                return JsonApiResponse.error("Could not update the entity http connection", Json.object("error", "Could not deserialize the entity http connection"));
            }
        } else {
            logger.error("Could not update the entity http connection {}", response.getError().getDetails());
            return JsonApiResponse.error("Could not update the entity http connection", response.getError().getDetails());
        }
    }

    private JsonApiResponse sendPut(ScanApiEndpoint endpoint, Json body) {
        return sendPut(endpoint.endpoint(), body);
    }

    private JsonApiResponse sendGet(ScanApiEndpoint endpoint, Json params) {
        return sendGet(endpoint.endpoint(), params);
    }

    private JsonApiResponse sendPost(ScanApiEndpoint endpoint, Json body) {
        return sendPost(endpoint.endpoint(), body);
    }

    private JsonApiResponse sendDelete(ScanApiEndpoint endpoint, Json params) {
        return sendDelete(endpoint.endpoint(), params);
    }

    /**
     * Send an HTTP request to the scanner.
     *
     * @param method   The HTTP method to use (GET, POST, PUT, ...)
     * @param endpoint The endpoint on the API to use.
     * @param params   The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    private JsonApiResponse sendRequestToScanManager(HttpMethod method, ScanApiEndpoint endpoint, Json params) {
        switch (method) {
            case GET:
                return sendGet(endpoint.endpoint(), params);
            case PUT:
                return sendPut(endpoint.endpoint(), params);
            case POST:
                return sendPost(endpoint.endpoint(), params);
            case DELETE:
                return sendDelete(endpoint.endpoint(), params);
            default:
                throw new UnsupportedOperationException("Unsupported HTTP method: " + method.asString());
        }
    }

    /**
     * Get the last updated date of the devices in CSL-Scan.
     *
     * @return The date of the last entities update in CSL-Scan.
     */
    public OffsetDateTime getLastLastEntityUpdateDate() {
        JsonApiResponse response = sendGet(
                ScanApiEndpoint.ENTITY_LAST_UPDATE, Json.object());
        try {
            String dateString = response.getResult().get(VALUE).asString().replace("\"", "");
            return ScanUtils.scanTimeToLocal(OffsetDateTime.parse(dateString));
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * Get the last updated date of the entities in CSL-Scan.
     *
     * @return The date of the last entities update in CSL-Scan.
     */
    public OffsetDateTime getLastMicrosoftKbsUpdateDate() {
        JsonApiResponse response = sendGet(
                ScanApiEndpoint.MICROSOFT_KB_LAST_UPDATE, Json.object());
        try {
            String dateString = response.getResult().get(VALUE).asString().replace("\"", "");
            return ScanUtils.scanTimeToLocal(OffsetDateTime.parse(dateString));
        } catch (NullPointerException e) {
            logger.warn("Could not get last Microsoft KBs update date from CSL-Scan", e);
            return null;
        }
    }

    /**
     * Get the last deletion date of the CPE items in CSL-Scan.
     *
     * @return The date of the last CPE items deletion in CSL-Scan.
     */
    public OffsetDateTime getLastCpeItemsDeletionDate() {
        JsonApiResponse response = sendGet(
                ScanApiEndpoint.CPE_ITEM_LAST_DELETION, Json.object());

        // If the response's status code is not 200, return null
        if (response.getExtra().get(STATUS_CODE).asInteger() != 200) {
            return null;
        }

        try {
            String dateString = response.getResult().get(VALUE).asString().replace("\"", "");
            return ScanUtils.scanTimeToLocal(OffsetDateTime.parse(dateString));
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * Get the last deletion date of the entities in CSL-Scan.
     *
     * @return The date of the last entities deletion in CSL-Scan.
     */
    public OffsetDateTime getLastEntitiesDeletionDate() {
        JsonApiResponse response = sendGet(
                ScanApiEndpoint.ENTITY_LAST_DELETION, Json.object());
        try {
            String dateString = response.getResult().get(VALUE).asString().replace("\"", "");
            return ScanUtils.scanTimeToLocal(OffsetDateTime.parse(dateString));
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * Get the last deletion date of the Microsoft KBs in CSL-Scan.
     *
     * @return The date of the last Microsoft KBs deletion in CSL-Scan.
     */
    public OffsetDateTime getLastMicrosoftKbsDeletionDate() {
        JsonApiResponse response = sendGet(
                ScanApiEndpoint.MICROSOFT_KB_LAST_DELETION, Json.object());
        try {
            String dateString = response.getResult().get(VALUE).asString().replace("\"", "");
            return ScanUtils.scanTimeToLocal(OffsetDateTime.parse(dateString));
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * Set the last updated date of the devices in CSL-Scan.
     *
     * @param date The date of the last entities update in CSL-Scan.
     * @throws Exception If the request failed (ie status code != 200).
     */
    public void setLastCpeItemsDeletionDate(OffsetDateTime date) throws Exception {
        JsonApiResponse response = sendPost(ScanApiEndpoint.CPE_ITEM_LAST_DELETION,
                Json.object("cpeItemsLastDeletion", ScanUtils.localTimeToScan(date).toString())
        );
        if (response.getExtra().get(STATUS_CODE).asInteger() != 200) {
            throw new Exception("Error while setting last CPE items deletion date: " + response.getResult());
        }
    }

    /**
     * Set the last deletion date of the entities in CSL-Scan.
     *
     * @param date The date of the last entities deletion in CSL-Scan.
     * @throws Exception If the request failed (ie status code != 200).
     */
    public void setLastEntitiesDeletionDate(OffsetDateTime date) throws Exception {
        JsonApiResponse response = sendPost(ScanApiEndpoint.ENTITY_LAST_DELETION,
                Json.object("entitiesLastDeletion", ScanUtils.localTimeToScan(date).toString())
        );
        if (response.getExtra().get(STATUS_CODE).asInteger() != 200) {
            throw new Exception("Error while setting last entities deletion date: " + response.getResult());
        }
    }

    /**
     * Set the last deletion date of the Microsoft KBs in CSL-Scan.
     *
     * @param date The date of the last Microsoft KBs deletion in CSL-Scan.
     * @throws Exception If the request failed (ie status code != 200).
     */
    public void setLastMicrosoftKbsDeletionDate(OffsetDateTime date) throws Exception {
        JsonApiResponse response = sendPost(ScanApiEndpoint.MICROSOFT_KB_LAST_DELETION,
                Json.object("microsoftKbsLastDeletion", ScanUtils.localTimeToScan(date).toString())
        );
        if (response.getExtra().get(STATUS_CODE).asInteger() != 200) {
            throw new Exception("Error while setting last Microsoft KBs deletion date: " + response.getResult());
        }
    }

    /**
     * Drop a collection in CSL-Scan.
     */
    public void dropCollection(ScanCollection collection) throws Exception {
        JsonApiResponse response = sendDelete(
                String.format(ScanApiEndpoint.DROP_COLLECTION.endpoint(), collection.getName()), Json.object());
        if (!response.isSuccess() || response.getExtra().get(STATUS_CODE).asInteger() != 200) {
            throw new Exception("Could not drop collection " + collection.getName() + " in CSL-Scan");
        }
    }

    /**
     * Drop all collections in CSL-Scan.
     */
    public void dropAllCollections() throws Exception {
        for (ScanCollection collection : ScanCollection.values()) {
            dropCollection(collection);
        }
    }

    public JsonApiResponse fetchHttpConnectionStage(String ipAddress, String port, String username, String password, String realm, String token, EntityHttpConnection entityHttpConnection, Integer stageIndex) {
        JsonApiResponse response = JsonApiResponse.error("Could not fetch stage page");
        try {
            Device device = Device.fromIpAddress(ipAddress);
            Connection connection = new HttpConnection(
                    "0",
                    port,
                    List.of("0"),
                    "0",
                    null,
                    username,
                    password,
                    realm,
                    token,
                    null,
                    false,
                    null
            );
            device.setConnections(List.of(connection));
            if (stageIndex == null) {
                stageIndex = entityHttpConnection.getStages().size() - 1;
            }

            Json body = Json.object(
                    "entityHttpConnection", entityHttpConnection.serializeForScanner(),
                    "entity", device.serializeForScanner(),
                    "stageIndex", stageIndex
            );
            response = sendPost(ScanApiEndpoint.ENTITY_HTTP_CONNECTION_FETCH_STAGE, body);
        } catch (Exception e) {
            logger.error("Could not fetch stage page", e);
        }
        return response;
    }

    public EntityHttpConnectionTestResult testEntityHttpConnection(
            String entityHttpConnectionId,
            EntityHttpConnection entityHttpConnection,
            String deviceId,
            Device device,
            String connectionId,
            HttpConnection connection
            ) throws Exception {
        Json requestBody = Json.object();
        if (entityHttpConnectionId != null) {
            requestBody.set("entityHttpConnectionId", entityHttpConnectionId);
        }
        if (entityHttpConnection != null) {
            requestBody.set("entityHttpConnection", entityHttpConnection.serializeForScanner());
        }
        if (deviceId != null) {
            requestBody.set("deviceId", deviceId);
            requestBody.set("entityId", deviceId);
        }
        if (device != null) {
            requestBody.set("entity", device.serializeForScanner());
        }
        if (connectionId != null) {
            requestBody.set("connectionInfoId", String.valueOf(connectionId));
        }
        if (connection != null) {
            requestBody.set("connectionInfo", connection.serializeForScanner());
        }
        JsonApiResponse response = sendPost(ScanApiEndpoint.ENTITY_HTTP_CONNECTION_TEST, requestBody);
        if (!response.isSuccess()) {
            throw new Exception("Could not test the entity http connection: " + response.getError().getDetails());
        } else {
            return EntityHttpConnectionTestResult.fromScannerJson(response.getResult());
        }
    }

    public JsonApiResponse getPredefinedHttpVariables() {
        return sendGet(ScanApiEndpoint.ENTITY_HTTP_CONNECTION_FETCH_PREDEFINED_VARIABLES, Json.object());
    }

    public JsonApiResponse getInstalledNpmPackages() {
        return sendRequestToScanManager(HttpMethod.GET, ScanApiEndpoint.ENTITY_HTTP_CONNECTION_GET_INSTALLED_NPM_PACKAGES, Json.object());
    }

    /**
     * Get the current cron expression for the periodic discovery task.
     *
     * @return The cron expression for the periodic discovery task.
     */
    public Json getDiscoveryCron() {
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.GET, ScanApiEndpoint.DISCOVERY_GET_CRON, Json.object());
        if (response.isSuccess() && response.getExtra().get(STATUS_CODE).asInteger() == 200) {
            Json result = response.getResult();
            String cron = null;
            DynamicDiscoveryFrequencyOption frequencyOption = null;
            if (result.has("cron") && result.get("cron").isString()) {
                cron = result.get("cron").asString();
            } else {
                return null;
            }

            if (result.has(FREQUENCY) && result.get(FREQUENCY).isString()) {
                frequencyOption = DynamicDiscoveryFrequencyOption.fromScanName(result.get(FREQUENCY).asString());
            }
            return Json.object("cron", cron, "frequencyOption", frequencyOption.dbapiName());
        } else {
            return null;
        }
    }

    /**
     * Set the cron expression for the periodic discovery task.
     *
     * @param cron The new cron expression for the periodic discovery task.
     * @throws Exception If the request failed (ie status code != 200).
     */
    public void setDiscoveryCron(String cron, DynamicDiscoveryFrequencyOption frequencyOption) throws Exception {
        String endpoint = ScanApiEndpoint.DISCOVERY_UPDATE_CRON.endpoint() + "?cronExpression=" + cron;
        if (frequencyOption != null) {
            endpoint += "&frequencyOption=" + frequencyOption.name();
        }
        JsonApiResponse response = sendPut(endpoint, Json.object());
        if (!response.isSuccess() || response.getExtra().get(STATUS_CODE).asInteger() != 200) {
            throw new Exception("Could not set the discovery cron: " + response.getError().getReason());
        }
    }

    /**
     * Get the status of the periodic discovery task.
     *
     * @return Whether the periodic discovery task is active.
     * @throws Exception If the request failed (ie status code != 200).
     */
    public boolean isDiscoveryCronActive() throws Exception {
        JsonApiResponse response = sendGet(ScanApiEndpoint.DISCOVERY_IS_CRON_ACTIVE, Json.object());
        if (response.isSuccess() && response.getExtra().get(STATUS_CODE).asInteger() == 200) {
            Json result = response.getResult();
            if (result.has(VALUE)) {
                if (result.get(VALUE).isBoolean()) {
                    return result.get(VALUE).asBoolean();
                } else if (result.get(VALUE).isString()) {
                    return Boolean.parseBoolean(result.get(VALUE).asString());
                } else {
                    throw new Exception(COULD_NOT_GET_THE_DISCOVERY_CRON_STATUS + response.getError().getReason());
                }
            } else {
                throw new Exception(COULD_NOT_GET_THE_DISCOVERY_CRON_STATUS + response.getError().getReason());
            }
        } else {
            throw new Exception(COULD_NOT_GET_THE_DISCOVERY_CRON_STATUS + response.getError().getReason());
        }
    }

    /**
     * Set the status of the periodic discovery task.
     *
     * @param active Whether the periodic discovery task should be active.
     * @throws Exception If the request failed (ie status code != 200).
     */
    public void setDiscoveryCronActive(boolean active) throws Exception {
        JsonApiResponse response = sendPut(ScanApiEndpoint.DISCOVERY_SET_CRON_ACTIVE.endpoint() + "?isActive=" + active, Json.object());
        if (!response.isSuccess() || response.getExtra().get(STATUS_CODE).asInteger() != 200) {
            throw new Exception("Could not set the discovery cron status: " + response.getError().getReason());
        }
    }

    /**
     * Cancel the current scan (if any).
     */
    public void cancelScan() throws Exception {
        JsonApiResponse response = sendGet(ScanApiEndpoint.DISCOVERY_CANCEL, Json.object());
        if (!response.isSuccess() || response.getExtra().get(STATUS_CODE).asInteger() != 200) {
            throw new Exception(response.getError().getReason());
        }
    }

    /**
     * Create a new connection certificate in CSL-Scan
     *
     * @param certificateConnection certificate to create
     * @return the response from CSL-Scan
     */
    public JsonApiResponse createConnectionCertificate(EntityConnectionCertificate certificateConnection) {
        JsonApiResponse response = sendPost(ScanApiEndpoint.CERTIFICATE_CONNECTION, certificateConnection.serializeForScanner());
        if (!response.isSuccess()) {
            JsonApiResponse response1 = extractErrorContent(response);
            if (response1 != null) return response1;
        }
        return response;
    }

    /**
     * Update a connection certificate in CSL-Scan
     *
     * @param certificateConnection certificate to update
     * @return the response from CSL-Scan
     */
    public JsonApiResponse updateConnectionCertificate(EntityConnectionCertificate certificateConnection) {
        JsonApiResponse response = sendPut(
                String.format(ScanApiEndpoint.CERTIFICATE_CONNECTION_DETAILS.endpoint(), certificateConnection.getUuid()),
                certificateConnection.serializeForScanner());
        if (!response.isSuccess()) {
            JsonApiResponse response1 = extractErrorContent(response);
            if (response1 != null) {
                return response1;
            }
        }
        return response;
    }

    /**
     * Delete connection certificate in CSL-Scan
     *
     * @param certificateUuid uuid of the certificate to delete
     * @return the response from CSL-Scan
     */
    public JsonApiResponse deleteConnectionCertificate(String certificateUuid) {
        return sendDelete(
                String.format(ScanApiEndpoint.CERTIFICATE_CONNECTION_DETAILS.endpoint(), certificateUuid),
                null);
    }

    /**
     * Get a single connection certificate in CSL-Scan
     *
     * @param uuid the certificate to get
     * @return the response from CSL-Scan
     */
    public JsonApiResponse getConnectionCertificate(String uuid) {
        return sendGet(
                String.format(ScanApiEndpoint.CERTIFICATE_CONNECTION_DETAILS.endpoint(), uuid),
                null);
    }

    /**
     * Get all connection certificates in CSL-Scan
     *
     * @return the response from CSL-Scan
     */
    public JsonApiResponse getAllConnectionCertificates() {
        return sendGet(ScanApiEndpoint.CERTIFICATE_CONNECTION, null);
    }

    private static @Nullable JsonApiResponse extractErrorContent(JsonApiResponse response) {
        Json errorDetails = response.getError().getDetails();
        if (errorDetails.has(CONTENT)) {
            Json errorContents = Json.read(response.getError().getDetails().get(CONTENT).asString());
            errorDetails.set(CONTENT, errorContents);
            return JsonApiResponse.error(response.getError().getReason(), errorDetails);
        }
        return null;
    }

    public List<ExternalConnectionInfoTemplate> getExternalConnectionInfoTemplates() {
        JsonApiResponse response = sendGet(ScanApiEndpoint.EXTERNAL_CONNECTION_INFO_TEMPLATES, Json.object());
        if (response.isSuccess()) {
            return ListUtils.toList(
                    response.getResult().asJsonList().stream()
                    .map(ExternalConnectionInfoTemplate::fromScannerJson)
            );
        } else {
            return null;
        }
    }

    public List<ExternalConnectionInfo> getExternalConnectionInfos(boolean includeDeleted) {
        JsonApiResponse response = sendGet(ScanApiEndpoint.EXTERNAL_CONNECTION_INFOS, Json.object("includeDeleted", includeDeleted));
        if (!response.isSuccess()) {
            return null;
        }
        return ListUtils.toList(response.getResult().asJsonList().stream()
                .map(ExternalConnectionInfo::fromScannerJson)
        );
    }

    public JsonApiResponse createExternalConnectionInfo(ExternalConnectionInfo externalConnectionInfo) {
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.POST, ScanApiEndpoint.EXTERNAL_CONNECTION_INFOS, externalConnectionInfo.serializeForScanner());
        if (!response.isSuccess()) {
            JsonApiResponse response1 = extractErrorContent(response);
            if (response1 != null) return response1;
        }
        return response;
    }

    public JsonApiResponse updateExternalConnectionInfo(ExternalConnectionInfo externalConnectionInfo) {
        String connectionUuid = externalConnectionInfo.getId();
        if (connectionUuid == null) {
            return JsonApiResponse.error("Connection UUID is null", Json.object());
        }
        JsonApiResponse response = sendPost(String.format(ScanApiEndpoint.EXTERNAL_CONNECTION_INFO_DETAILS.endpoint(), connectionUuid), externalConnectionInfo.serializeForScanner());
        if (!response.isSuccess()) {
            JsonApiResponse response1 = extractErrorContent(response);
            if (response1 != null) return response1;
        }
        return response;
    }

    public JsonApiResponse deleteExternalConnectionInfo(String connectionInfoId, boolean hardDelete) {
        return sendDelete(String.format(ScanApiEndpoint.EXTERNAL_CONNECTION_INFO_DETAILS.endpoint(), connectionInfoId), Json.object("hardDelete", hardDelete));
    }

    public JsonApiResponse clearExternalConnectionInfos() {
        return sendRequestToScanManager(HttpMethod.DELETE, ScanApiEndpoint.EXTERNAL_CONNECTION_INFO_CLEAR, Json.object());
    }

    public ExternalScan startExternalDiscoveryScan(String connectionInfoId) {
        JsonApiResponse response = sendGet(String.format(ScanApiEndpoint.EXTERNAL_DISCOVERY_START_SCAN.endpoint(), connectionInfoId), Json.object());
        if (response.isSuccess()) {
            return ExternalScan.fromScannerJson(response.getResult());
        } else {
            return null;
        }
    }

    public List<ExternalDiscoveredDevice> getExternalDiscoveredDevices(OffsetDateTime dateTime, Integer limit, Integer offset) {
        Json requestParams = Json.object();
        if (dateTime != null) {
            requestParams.set("date", ScanUtils.localTimeToScan(dateTime).toString());
        }
        if (limit != null) {
            requestParams.set(LIMIT, limit);
        }
        if (offset != null) {
            requestParams.set("skip", offset);
        }
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.GET, ScanApiEndpoint.EXTERNAL_DISCOVERED_DEVICES, requestParams);

        if (response.isSuccess()) {
            return ListUtils.toList(response.getResult().asJsonList().stream()
                    .map(ExternalDiscoveredDevice::fromScannerJson)
            );
        } else {
            return null;
        }
    }

    public List<ExternalDiscoveredDevice> getExternalDiscoveredDevices(OffsetDateTime dateTime) {
        return getExternalDiscoveredDevices(dateTime, null, null);
    }

    /**
     * Get the external connection information templates from scan.
     * @param dateTime datestamp of the last update. We will recover the templates from this date.
     * @param limit quantity of the batch
     * @param offset offset of the batch
     * @return the list of the retrieved templates
     */
    public List<ExternalConnectionInfoTemplate> getExternalConnectionInfoTemplates(OffsetDateTime dateTime, Integer limit, Integer offset) {
        Json requestParams = Json.object();
        if (dateTime != null) {
            requestParams.set("date", ScanUtils.localTimeToScan(dateTime).toString());
        }
        if (limit != null) {
            requestParams.set(LIMIT, limit);
        }
        if (offset != null) {
            requestParams.set("skip", offset);
        }

        JsonApiResponse response = sendRequestToScanManager(HttpMethod.GET, ScanApiEndpoint.EXTERNAL_CONNECTION_INFO_TEMPLATES, requestParams);

        if (response.isSuccess()) {
            return ListUtils.toList(response.getResult().asJsonList().stream().map(ExternalConnectionInfoTemplate::fromScannerJson));
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Get the external connection information templates from scan.
     * @param dateTime datestamp of the last update. We will recover the templates from this date.
     * @return the list of the retrieved templates
     */
    public List<ExternalConnectionInfoTemplate> getExternalConnectionInfoTemplates(OffsetDateTime dateTime) {
        return getExternalConnectionInfoTemplates(dateTime, null, null);
    }

    public JsonApiResponse clearExternalDiscoveredDevices() {
        return sendRequestToScanManager(HttpMethod.DELETE, ScanApiEndpoint.EXTERNAL_DISCOVERED_DEVICES_CLEAR, Json.object());
    }

    public JsonApiResponse publishExternalDiscoveredDevices(List<UUID> discoveredDeviceUuids) {
        Json body = Json.array(discoveredDeviceUuids.stream().map(UUID::toString).toArray());
        return sendRequestToScanManager(HttpMethod.POST, ScanApiEndpoint.EXTERNAL_PUBLISH_DISCOVERED_DEVICES, body);
    }

    public ExternalScan getScanInfo(String uuid) {
        logger.debug("Getting scan info for {}", uuid);
        logger.warn("NOT IMPLEMENTED YET");
        return null;
    }

    /**
     * Start a new import task in CSL-Scan.
     *
     * @param bsonFilePath The path to the bson file to import.
     * @return The status of the import task.
     * @throws Exception If the request failed.
     */
    public ImportQuery importBsonFile(Path bsonFilePath, boolean shouldDrop) throws Exception {
//        String uri = url + ScanApiEndpoint.ENTITY_HTTP_CONNECTION_IMPORT_BSON.endpoint();
        String uri = createUriFrom(ScanApiEndpoint.ENTITY_HTTP_CONNECTION_IMPORT_BSON.endpoint());
        Request request = httpClient.newRequest(uri);
        request.method(HttpMethod.POST);
        addHeaderTo(request, X_CORRELATION_ID, MDC.get(X_CORRELATION_ID));
        request.param("drop", String.valueOf(shouldDrop));


        try (MultiPartRequestContent multiPart = new MultiPartRequestContent()) {
                multiPart.addFieldPart("file", new PathRequestContent(bsonFilePath), null);
            addBodyTo(request, multiPart);
        }


        try {
            ContentResponse contentResponse = request.send();
            if (contentResponse.getStatus() >= 400) {
                logger.warn("Error while sending request to CSL-Scan: {}", contentResponse.getContentAsString());
                throw new Exception("Error while sending request to CSL-Scan: unexpected status code " + contentResponse.getStatus());
            }
            return ImportQuery.fromScannerJson(Json.read(contentResponse.getContentAsString()));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Error while sending request to CSL-Scan", e);
            throw new Exception("Error while sending request to CSL-Scan", e);
        } catch (IllegalArgumentException e) {
            logger.error("Error while parsing the import id", e);
            throw new Exception("Error while parsing the import id", e);
        }
    }

    /**
     * Request the export of the http templates from the scanner.
     *
     * @return The export query.
     * @throws Exception If the request failed.
     */
    public ExportQuery requestExportHttpTemplates() throws Exception {
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.GET, ScanApiEndpoint.ENTITY_HTTP_CONNECTION_EXPORT_BSON, Json.object());
        if (response.isSuccess()) {
            return ExportQuery.fromScannerJson(response.getResult());
        } else {
            throw new Exception("Could not request the export of the http templates");
        }
    }

    /**
     * Get the status of an export query.
     *
     * @param uuid The uuid of the export query.
     * @return The new export query.
     */
    public ExportQuery getExportQueryStatus(UUID uuid) {
        JsonApiResponse response = sendGet(String.format(ScanApiEndpoint.ENTITY_HTTP_CONNECTION_EXPORT_BSON_STATUS.endpoint(), uuid.toString()), Json.object());
        if (response.isSuccess()) {
            return ExportQuery.fromScannerJson(response.getResult());
        } else {
            return null;
        }
    }

    /**
     * Get the status of an export query.
     *
     * @param exportQuery The export query to get the status of.
     * @return The new export query.
     */
    public ExportQuery getExportQueryStatus(ExportQuery exportQuery) {
        return getExportQueryStatus(exportQuery.getId());
    }

    public void deleteExportFile(UUID uuid) {
        sendDelete(String.format(ScanApiEndpoint.ENTITY_HTTP_CONNECTION_EXPORT_BSON_DELETE.endpoint(), uuid.toString()), Json.object());
    }

    public void deleteExportFile(ExportQuery exportQuery) {
        deleteExportFile(exportQuery.getId());
    }

    public Path downloadExportFile(ExportQuery exportQuery) throws ExecutionException, InterruptedException, TimeoutException {
        URI uri = URI.create(getUrl() + String.format(ScanApiEndpoint.ENTITY_HTTP_CONNECTION_EXPORT_BSON_DOWNLOAD.endpoint(), exportQuery.getId().toString()));
        Request request = httpClient.newRequest(uri);
        InputStreamResponseListener listener = new InputStreamResponseListener();
        request.send(listener);
        Response response = listener.get(30, TimeUnit.SECONDS);
        if (response.getStatus() == 200) {
            try {
                return fileStorageService.saveFile(listener.getInputStream(), exportQuery.getFilename());
            } catch (IOException e) {
                logger.error("Could not save the export file");
                logger.debug("Could not save the export file", e);
                return null;
            }
        } else {
            return null;
        }
    }

    public List<EntityHttpConnection> getEntityHttpConnectionsToSync() {
        JsonApiResponse response = sendGet(ScanApiEndpoint.ENTITY_HTTP_CONNECTION_GET_SYNC_NEEDED, Json.object());
        if (response.isSuccess()) {
            return ListUtils.toList(response.getResult().asJsonList().stream()
                    .map(EntityHttpConnection::fromScannerJson)
            );
        } else {
            return List.of();
        }
    }

    public void notifyEntityHttpConnectionSynchronized(List<EntityHttpConnection> entityHttpConnections) {
        Json body = Json.array(entityHttpConnections.stream().map(EntityHttpConnection::getUuid).toArray());
        sendPost(ScanApiEndpoint.ENTITY_HTTP_CONNECTION_SET_SYNC_NEEDED, body);
    }

    public ImportQuery importBsonFile(Path bsonFilePath) throws Exception {
        return importBsonFile(bsonFilePath, false);
    }

    public ImportQuery getImportTaskStatus(UUID uuid) {
        JsonApiResponse response = sendGet(String.format(ScanApiEndpoint.ENTITY_HTTP_CONNECTION_IMPORT_BSON_STATUS.endpoint(), uuid.toString()), Json.object());
        if (response.isSuccess()) {
            return ImportQuery.fromScannerJson(response.getResult());
        } else {
            return null;
        }
    }
}