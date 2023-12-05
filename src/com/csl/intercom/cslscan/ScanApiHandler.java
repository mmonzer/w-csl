package com.csl.intercom.cslscan;

import com.csl.core.CSLContext;
import com.csl.intercom.cslscan.enums.ScanApiEndpoint;
import com.csl.intercom.cslscan.enums.ScanCollection;
import com.csl.intercom.cslscan.models.CpeItem;
import com.csl.intercom.cslscan.models.EntityHttpConnection;
import com.csl.intercom.cslscan.models.EntityHttpConnectionTestResult;
import com.csl.intercom.cslscan.models.MicrosoftKB;
import com.csl.intercom.dbapi.DbapiHandler;
import com.csl.intercom.dbapi.models.Connection;
import com.csl.intercom.dbapi.models.Device;
import com.csl.intercom.dbapi.models.HttpConnection;
import com.csl.util.Pair;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.JsonApiResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Class to handle communication with CSL-Scan's HTTP API.
 */
public class ScanApiHandler implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ScanApiHandler.class);
    private String scanManagerUrl;
    private HttpClient httpClient = new HttpClient();

    public ScanApiHandler() {
        this(ScanUtils.generateScanApiUrlFromConfig(CSLContext.instance.getConfig().get("discovery")));
    }

    public ScanApiHandler(String scanManagerUrl) {
        this.scanManagerUrl = scanManagerUrl;

        try {
            httpClient.start();
        } catch (Exception e) {
            logger.error("Could not start the http client for CSL-Scan API.", e);
        }
    }

    @Override
    public void close() throws Exception {
        this.httpClient.stop();
    }

    /**
     * Create a new device in the scanner
     *
     * @param device A {@link Json} containing the device to add. Fields 'id', 'name' and 'ip' are required.
     * @return A {@link Json} containing the newly created device, as handed by the scanner
     */
    public JsonApiResponse addEntity(Device device) {
        return sendRequestToScanManager(HttpMethod.POST, ScanApiEndpoint.ENTITY, device.serializeForScanner());
    }

    /**
     * Get the list of configured entities in the scanner.
     *
     * @return A {@link Json} array containing the all the configured entities in the scanner.
     */
    public JsonApiResponse listEntities() {
        return sendRequestToScanManager(HttpMethod.GET, ScanApiEndpoint.ENTITY, Json.object());
    }

    /**
     * Get a specific entity.
     *
     * @param id The unique identifier created by the scanner, as returned at creation or in a list.
     * @return A {@link Json} containing the specified entity.
     */
    public JsonApiResponse getEntity(String id) {
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.GET,
                String.format(ScanApiEndpoint.ENTITY_DETAILS.endpoint(), id), Json.object());
        int statusCode;
        try {
            statusCode = response.getExtra().get("status_code").asInteger();
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

    public List<String> deleteEntities(List<Pair<String, OffsetDateTime>> deletedDevices) throws Exception {
        OffsetDateTime maxDate = OffsetDateTime.MIN;
        List<String> failedDevices = new ArrayList<>();
        boolean hasFailed = false;

        // Delete devices from CSL-Scan and get the max deletion date
        for (Pair<String, OffsetDateTime> deletedDevice : deletedDevices) {
            String uuid = deletedDevice.getFirst();
            OffsetDateTime deletionDate = deletedDevice.getSecond();
            try {
                deleteEntity(uuid);
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
     * @return An empty object on success, an error message on failure.
     */
    public JsonApiResponse deleteEntity(String id) {
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.DELETE,
                String.format(ScanApiEndpoint.ENTITY_DETAILS.endpoint(), id), Json.object());
        boolean success = response.isSuccess();
        Json result = response.getResult();
        if (success) {
            if (response.getExtra().get("status_code").asInteger() == 404) {
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
        return sendRequestToScanManager(HttpMethod.GET,
                String.format(ScanApiEndpoint.ENTITY_CPE_ITEMS.endpoint(), id), Json.object());
    }

    /**
     * Get the status of a specific scan task.
     *
     * @param id The unique id of the task.
     * @return The status of the scan.
     */
    public JsonApiResponse getScanStatus(String id) {
        return sendRequestToScanManager(HttpMethod.GET,
                String.format(ScanApiEndpoint.DISCOVERY_STATUS_DETAILS.endpoint(), id), Json.object());
    }

    /**
     * Get the status of an entity's scan
     *
     * @param id The entity's unique id.
     * @return The status of the scan.
     */
    public JsonApiResponse getEntityScanStatus(String id) {
        return sendRequestToScanManager(HttpMethod.GET,
                String.format(ScanApiEndpoint.ENTITY_SCAN_STATUS.endpoint(), id), Json.object());
    }

    /**
     * Fetch CSL-Scan's status.
     *
     * @return A {@link JsonApiResponse} with CSL-Scan's status as it was received, or with an error in the 'error' field.
     */
    public JsonApiResponse getStatus() {
        return sendRequestToScanManager(HttpMethod.GET, ScanApiEndpoint.DISCOVERY_STATUS, Json.object());
    }

    /**
     * Test if an existing connection is valid.
     *
     * @param deviceUuid     The uuid of the device to test.
     * @param connectionUuid The uuid of the connection to test.
     * @return A {@link JsonApiResponse} with CSL-Scan's response.
     */
    public JsonApiResponse testConnection(String deviceUuid, String connectionUuid) {
        return sendRequestToScanManager(HttpMethod.GET,
                String.format(ScanApiEndpoint.ENTITY_TEST_EXISTING_CONNECTION.endpoint(), deviceUuid),
                Json.object("connection_uuid", connectionUuid));
    }

    /**
     * Test if a connection is valid.
     * The connection does not need to exist in CSL-Scan.
     *
     * @param device The device to test. We assume it contains a connection. Only the first connection will be tested.
     * @return A {@link JsonApiResponse} with CSL-Scan's response.
     */
    public JsonApiResponse testConnection(Device device) {
        return sendRequestToScanManager(HttpMethod.POST,
                ScanApiEndpoint.ENTITY_TEST_CONNECTION,
                device.serializeForScanner());
    }

    /**
     * Request the deletion of a CPE Item to CSL-Scan.
     *
     * @param id The uuid of the CPE Item to delete.
     */
    public void deleteCpeItemFromScan(String id) {
        sendRequestToScanManager(HttpMethod.DELETE,
                String.format(ScanApiEndpoint.CPE_ITEM_DETAILS.endpoint(), id), Json.object());
    }

    /**
     * Request multiple deletions of CPE Items to CSL-Scan.
     *
     * @param deletedCpes The list of CPE Items to delete.
     */
    public void deleteCpeItemsFromScan(List<Pair<String, OffsetDateTime>> deletedCpes) {
        OffsetDateTime maxDate = OffsetDateTime.MIN;

        // Delete the CPE items from the scanner and find the latest deletion date
        for (Pair<String, OffsetDateTime> deletedCpe : deletedCpes) {
            String id = deletedCpe.getFirst();
            OffsetDateTime date = deletedCpe.getSecond();
            if (date != null && date.isAfter(maxDate)) {
                maxDate = date;
            }
            deleteCpeItemFromScan(id);
        }

        if (!deletedCpes.isEmpty()) {
            try {
                setLastCpeItemsDeletionDate(maxDate);
            } catch (Exception e) {
                logger.error("Could not set the last CPE items deletion date", e);
            }
        }
    }

    /**
     * Request the deletion of a {@link MicrosoftKB} to CSL-Scan.
     *
     * @param id The uuid of the {@link MicrosoftKB} to delete.
     */
    public void deleteMicrosoftKBFromScan(String id) {
        sendRequestToScanManager(HttpMethod.DELETE,
                String.format(ScanApiEndpoint.MICROSOFT_KB_DETAILS.endpoint(), id), Json.object());
    }

    public void deleteMicrosoftKBsFromScan(List<Pair<String, OffsetDateTime>> deletedMicrosoftKBs) {
        OffsetDateTime maxDate = OffsetDateTime.MIN;

        // Delete the Microsoft KBs from the scanner and find the latest deletion date
        for (Pair<String, OffsetDateTime> deletedMicrosoftKB : deletedMicrosoftKBs) {
            String id = deletedMicrosoftKB.getFirst();
            OffsetDateTime date = deletedMicrosoftKB.getSecond();
            if (date != null && date.isAfter(maxDate)) {
                maxDate = date;
            }
            deleteMicrosoftKBFromScan(id);
        }

        if (!deletedMicrosoftKBs.isEmpty()) {
            try {
                setLastMicrosoftKbsDeletionDate(maxDate);
            } catch (Exception e) {
                logger.error("Could not set the last Microsoft KBs deletion date", e);
            }
        }
    }


    /**
     * Get the CPE items that have changed since the specified date.
     *
     * @param date The date to start receiving notifications. May be null to retrieve all the items.
     * @return A {@link List<MicrosoftKB>} array containing the CPE items that have changed since the specified date, or all the items if date was null.
     */
    public List<CpeItem> getCpeItemChangesSince(OffsetDateTime date) {
        JsonApiResponse response;
        Json cpeItems = Json.array();
        if (date == null) {
            response = sendRequestToScanManager(HttpMethod.GET, ScanApiEndpoint.CPE_ITEM, Json.object());
        } else {
            response = sendRequestToScanManager(HttpMethod.GET, ScanApiEndpoint.CPE_ITEM, Json.object("date", ScanUtils.localTimeToScan(date).toString()));
        }
        if (response.isSuccess() && response.getExtra().get("status_code").asInteger() == 200) {
            cpeItems = response.getResult();

            // Parse the items, filter those whose updated date is *exactly* the last updated date, and return the resulting list.
            return cpeItems.asJsonList().stream()
                    .map(CpeItem::fromScannerJson)
                    .filter(Predicate.not(cpeItem -> cpeItem.getDiscoveredDate().equals(date)))
                    .collect(Collectors.toList());
        } else {
            return null;
        }
    }

    /**
     * Get the KBs that have changed since the specified date.
     *
     * @param date The date to start receiving notifications. May be null to retrieve all the items.
     * @return A {@link List<MicrosoftKB>} containing the KBs that have changed since the specified date, or all the items if date was null.
     */
    public List<MicrosoftKB> getMicrosoftKbChangesSince(OffsetDateTime date) {
        JsonApiResponse response;
        Json microsoftKbs = Json.array();
        if (date == null) {
            response = sendRequestToScanManager(HttpMethod.GET, ScanApiEndpoint.MICROSOFT_KB, Json.object());
        } else {
            response = sendRequestToScanManager(HttpMethod.GET, ScanApiEndpoint.MICROSOFT_KB, Json.object("date", ScanUtils.localTimeToScan(date).toString()));
        }
        if (response.isSuccess() && response.getExtra().get("status_code").asInteger() == 200) {
            microsoftKbs = response.getResult();
            return microsoftKbs.asJsonList().stream()
                    .map(MicrosoftKB::fromScannerJson)
                    .filter(Predicate.not(microsoftKB -> microsoftKB.getDiscoveredDate().equals(date)))
                    .collect(Collectors.toList());
        } else {
            return null;
        }
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
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.GET,
                ScanApiEndpoint.ENTITY_HTTP_CONNECTION, Json.object("visibleOnly", visibleOnly));
        if (response.isSuccess() && response.getExtra().get("status_code").asInteger() == 200) {
            return response.getResult().asJsonList().stream()
                    .map(EntityHttpConnection::fromScannerJson)
                    .collect(Collectors.toList());
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
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.GET,
                String.format(ScanApiEndpoint.ENTITY_HTTP_CONNECTION_DETAILS.endpoint(), uuid), Json.object("visibleOnly", visibleOnly));
        if (response.isSuccess() && response.getExtra().get("status_code").asInteger() == 200) {
            return EntityHttpConnection.fromScannerJson(response.getResult());
        } else {
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
        return sendRequestToScanManager(HttpMethod.DELETE,
                String.format(ScanApiEndpoint.ENTITY_HTTP_CONNECTION_DETAILS.endpoint(), uuid), Json.object());
    }

    public JsonApiResponse createEntityHttpConnection(EntityHttpConnection entityHttpConnection) {
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.POST, ScanApiEndpoint.ENTITY_HTTP_CONNECTION, entityHttpConnection.serializeForScanner());
        if (response.isSuccess() && response.getExtra().get("status_code").asInteger() == 200) {
            EntityHttpConnection createdEntityHttpConnection = EntityHttpConnection.fromScannerJson(response.getResult());
            if (createdEntityHttpConnection != null) {
                return JsonApiResponse.result(createdEntityHttpConnection.serializeForDbapi(), response.getExtra());
            } else {
                return JsonApiResponse.error("Could not create the entity http connection", Json.object("error", "Could not deserialize the entity http connection"));
            }
        } else {
            return JsonApiResponse.error("Could not create the entity http connection", response.getError().getDetails());
        }
    }

    public JsonApiResponse updateEntityHttpConnection(EntityHttpConnection entityHttpConnection) {
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.PUT,
                String.format(ScanApiEndpoint.ENTITY_HTTP_CONNECTION_DETAILS.endpoint(), entityHttpConnection.getUuid()),
                entityHttpConnection.serializeForScanner());
        if (response.isSuccess() && response.getExtra().get("status_code").asInteger() == 200) {
            EntityHttpConnection createdEntityHttpConnection = EntityHttpConnection.fromScannerJson(response.getResult());
            if (createdEntityHttpConnection != null) {
                return JsonApiResponse.result(createdEntityHttpConnection.serializeForDbapi(), response.getExtra());
            } else {
                return JsonApiResponse.error("Could not update the entity http connection", Json.object("error", "Could not deserialize the entity http connection"));
            }
        } else {
            return JsonApiResponse.error("Could not update the entity http connection", response.getError().getDetails());
        }
    }

    /**
     * Send an HTTP request to the scanner.
     *
     * @param method   The HTTP method to use (GET, POST, PUT, ...)
     * @param endpoint The endpoint on the API to use.
     * @param params   The parameters to send, if any (if not, should be an empty {@link Json} object, not null).
     * @return The response to the request.
     */
    private JsonApiResponse sendRequestToScanManager(HttpMethod method, String endpoint, Json params) {
        JsonApiResponse res = JsonApiResponse.error(null);
        Request request;
        String URI = scanManagerUrl + endpoint;

        request = httpClient.newRequest(URI);
        request.method(method);
        try {
            switch (method) {
                case GET:
                case DELETE:
                    for (Map.Entry<String, Json> param : params.asJsonMap().entrySet()) {
                        if (param.getValue().isString()) {
                            request.param(param.getKey(), param.getValue().asString());
                        } else {
                            request.param(param.getKey(), param.getValue().toString());
                        }
                    }
                    break;

                case POST:
                case PUT:
                    request.content(new StringContentProvider(params.toString()), "application/json");
                    break;

                default:
                    throw new UnsupportedOperationException("Unsupported HTTP method: " + method.asString());
            }
            ContentResponse response = request.send();
            if (response.getStatus() >= 400) {
                return JsonApiResponse.error("Error while sending request to CSL-Scan", Json.object("status_code", response.getStatus(), "content", response.getContentAsString()));
            }
            if (response.getContent().length > 0) {
                if (response.getContent()[0] == '{' || response.getContent()[0] == '[') {
                    res = JsonApiResponse.result(
                            Json.read(response.getContentAsString()),
                            Json.object("status_code", response.getStatus())
                    );
                } else {
                    res = JsonApiResponse.result(Json.object("value", response.getContentAsString()),
                            Json.object("status_code", response.getStatus())
                    );
                }
            } else {
                res = JsonApiResponse.result(null,
                        Json.object("status_code", response.getStatus())
                );
            }
        } catch (UnsupportedOperationException e) {
            logger.error("Malformed json", e);
            res = JsonApiResponse.error(e.getMessage());
        } catch (Exception e) {
            logger.error("Error while sending request to CSL-Scan", e);
            if (e.getCause() instanceof ConnectException) {
                res = JsonApiResponse.error("Connection error with CSL-Scan");
            }
        }
        return res;
    }

    private JsonApiResponse sendRequestToScanManager(HttpMethod method, ScanApiEndpoint endpoint, Json params) {
        return sendRequestToScanManager(method, endpoint.endpoint(), params);
    }

    /**
     * Get the last updated date of the devices in CSL-Scan.
     *
     * @return The date of the last entities update in CSL-Scan.
     */
    public OffsetDateTime getLastLastEntityUpdateDate() {
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.GET,
                ScanApiEndpoint.ENTITY_LAST_UPDATE, Json.object());
        try {
            String dateString = response.getResult().get("value").asString().replace("\"", "");
            return ScanUtils.scanTimeToLocal(OffsetDateTime.parse(dateString));
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * The action to perform when a modification is notified on the CpeItems.
     *
     * @param dbapiHandler The interface of DB-API's API.
     */
    public void sendNewCpeItemsToDbapi(DbapiHandler dbapiHandler) {
        OffsetDateTime lastChangesDate = null;
        try {
            lastChangesDate = dbapiHandler.getCpeItemsLastUpdateDate();
        } catch (Exception e) {
            logger.warn("[Discovery] Could not get last update date from dbapi, fetching all CPE Items from CSL-Scan", e);
        }
        List<CpeItem> changes = getCpeItemChangesSince(lastChangesDate);
        if (changes != null) {
            try {
                dbapiHandler.sendCpeItems(changes);
            } catch (Exception e) {
                logger.error("Could not send CPE items to DB-API", e);
                logger.debug("Could not send CPE items to DB-API: {}", changes);
            }
        }
    }

    /**
     * The action to perform when a modification is notified on the MicrosoftKBs.
     */
    public void sendNewMicrosoftKbsToDbapi(DbapiHandler dbapiHandler) {
        OffsetDateTime lastChangesDate = null;
        try {
            lastChangesDate = dbapiHandler.getMicrosoftKbsLastUpdateDate();
        } catch (Exception e) {
            logger.info("Could not get last update date from dbapi, fetching all Microsoft KBs from CSL-Scan");
        }
        List<MicrosoftKB> changes = getMicrosoftKbChangesSince(lastChangesDate);
        if (changes != null) {
            try {
                dbapiHandler.sendMicrosoftKbs(changes);
            } catch (Exception e) {
                logger.error("Could not send Microsoft KBs to DB-API", e);
                logger.debug("Could not send Microsoft KBs to DB-API: {}", changes);
            }
        }
    }

    /**
     * Get the last updated date of the entities in CSL-Scan.
     *
     * @return The date of the last entities update in CSL-Scan.
     */
    public OffsetDateTime getLastMicrosoftKbsUpdateDate() {
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.GET,
                ScanApiEndpoint.MICROSOFT_KB_LAST_UPDATE, Json.object());
        try {
            String dateString = response.getResult().get("value").asString().replace("\"", "");
            return ScanUtils.scanTimeToLocal(OffsetDateTime.parse(dateString));
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * Get the last deletion date of the CPE items in CSL-Scan.
     *
     * @return The date of the last CPE items deletion in CSL-Scan.
     */
    public OffsetDateTime getLastCpeItemsDeletionDate() {
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.GET,
                ScanApiEndpoint.CPE_ITEM_LAST_DELETION, Json.object());

        // If the response's status code is not 200, return null
        if (response.getExtra().get("status_code").asInteger() != 200) {
            return null;
        }

        try {
            String dateString = response.getResult().get("value").asString().replace("\"", "");
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
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.GET,
                ScanApiEndpoint.ENTITY_LAST_DELETION, Json.object());
        try {
            String dateString = response.getResult().get("value").asString().replace("\"", "");
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
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.GET,
                ScanApiEndpoint.MICROSOFT_KB_LAST_DELETION, Json.object());
        try {
            String dateString = response.getResult().get("value").asString().replace("\"", "");
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
        JsonApiResponse response = sendRequestToScanManager(
                HttpMethod.POST, ScanApiEndpoint.CPE_ITEM_LAST_DELETION,
                Json.object("cpeItemsLastDeletion", ScanUtils.localTimeToScan(date).toString())
        );
        if (response.getExtra().get("status_code").asInteger() != 200) {
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
        JsonApiResponse response = sendRequestToScanManager(
                HttpMethod.POST, ScanApiEndpoint.ENTITY_LAST_DELETION,
                Json.object("entitiesLastDeletion", ScanUtils.localTimeToScan(date).toString())
        );
        if (response.getExtra().get("status_code").asInteger() != 200) {
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
        JsonApiResponse response = sendRequestToScanManager(
                HttpMethod.POST, ScanApiEndpoint.MICROSOFT_KB_LAST_DELETION,
                Json.object("microsoftKbsLastDeletion", ScanUtils.localTimeToScan(date).toString())
        );
        if (response.getExtra().get("status_code").asInteger() != 200) {
            throw new Exception("Error while setting last Microsoft KBs deletion date: " + response.getResult());
        }
    }

    /**
     * Drop a collection in CSL-Scan.
     */
    public void dropCollection(ScanCollection collection) throws Exception {
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.DELETE,
                String.format(ScanApiEndpoint.DROP_COLLECTION.endpoint(), collection.getName()), Json.object());
        if (!response.isSuccess() || response.getExtra().get("status_code").asInteger() != 200) {
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
                    0,
                    port,
                    List.of("0"),
                    "0",
                    null,
                    username,
                    password,
                    realm,
                    token,
                    null,
                    null,
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
            response = sendRequestToScanManager(HttpMethod.POST, ScanApiEndpoint.ENTITY_HTTP_CONNECTION_FETCH_STAGE, body);
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
            Integer connectionId) throws Exception {
        Json requestBody = Json.object();
        if (entityHttpConnectionId != null) {
            requestBody.set("entityHttpConnectionId", entityHttpConnectionId);
        }
        if (entityHttpConnection != null) {
            requestBody.set("entityHttpConnection", entityHttpConnection.serializeForScanner());
        }
        if (deviceId != null) {
            requestBody.set("deviceId", deviceId);
        }
        if (device != null) {
            requestBody.set("entity", device.serializeForScanner());
        }
        if (connectionId != null) {
            requestBody.set("connectionInfoId", String.valueOf(connectionId));
        }
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.POST, ScanApiEndpoint.ENTITY_HTTP_CONNECTION_TEST, requestBody);
        if (!response.isSuccess()) {
            throw new Exception("Could not test the entity http connection: " + response.getError().getDetails());
        } else {
            return EntityHttpConnectionTestResult.fromScannerJson(response.getResult());
        }
    }

    public JsonApiResponse getPredefinedHttpVariables() {
        return sendRequestToScanManager(HttpMethod.GET, ScanApiEndpoint.ENTITY_HTTP_CONNECTION_FETCH_PREDEFINED_VARIABLES, Json.object());
    }
}