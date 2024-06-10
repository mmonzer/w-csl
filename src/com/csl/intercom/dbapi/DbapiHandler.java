package com.csl.intercom.dbapi;

import com.csl.core.CSLContext;
import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.models.CpeItem;
import com.csl.intercom.cslscan.models.EntityHttpConnection;
import com.csl.intercom.cslscan.models.MicrosoftKB;
import com.csl.intercom.dbapi.enums.ConnectionProtocolField;
import com.csl.intercom.dbapi.enums.DbapiEndpoint;
import com.csl.intercom.dbapi.enums.FinishedScanStatus;
import com.csl.intercom.dbapi.models.*;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.JsonCmdPrivilegeFamily;
import com.csl.util.Pair;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.JsonApiResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Manage HTTP communications with DB-API.
 * Provides an interface for retrieving the devices, connections and so on,
 * and to send information to it (CPE Items, a Scan's status, ...).
 */
public class DbapiHandler implements AutoCloseable {
    private String dbapiUrl;
    private String apiKey;
    private HttpClient dbapiHttpClient = new HttpClient();
    private final int maxPageSize = 1000;
    private static final Logger logger = LoggerFactory.getLogger(DbapiHandler.class);

    public DbapiHandler() {
        this(CSLContext.instance.getConfig());
    }

    public DbapiHandler(Json config) {
        Json globalConfig = config.get("global");
        dbapiUrl = JsonUtil.getBooleanFromJson(globalConfig, "use_ssl", true) ? "https://" : "http://";
        dbapiUrl += JsonUtil.getStringFromJson(globalConfig, "ip_server_remote", "localhost");
        dbapiUrl += "/api";
        apiKey = JsonUtil.getStringFromJson(globalConfig, "api_key", "");
        try {
            dbapiHttpClient.start();
        } catch (Exception e) {
            logger.error("Could not start the DB-API HTTP client.", e);
        }
    }

    public void close() {
        try {
            dbapiHttpClient.stop();
        } catch (Exception e) {
            logger.error("Could not stop the DB-API HTTP client.", e);
        }
    }

    /**
     * Remove a list of CPE Items from DB-API.
     *
     * @param deletedItems The list of CPE Items to remove in DB-API.
     */
    private void deleteCpeItemsFromDbapi(List<CpeItem> deletedItems) {
        Json contents = Json.object("mongo_entity_ids", Json.array(deletedItems.stream().map(CpeItem::getMongoEntityId).toArray()));
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpoint.DELETE_CPE_ITEMS.getEndpoint())
                .header(HttpHeader.CONTENT_TYPE, "application/json")
                .content(new StringContentProvider(contents.toString()));
        try {
            request.send();
        } catch (Exception e) {
            logger.error("Could not delete the CPE Items from DB-API.", e);
        }
    }

    private void deleteMicrosoftKbsFromDbapi(List<MicrosoftKB> deletedItems) {
        Json contents = Json.object("mongo_entity_ids", Json.array(deletedItems.stream().map(MicrosoftKB::getMongoEntityId).toArray()));
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpoint.DELETE_MICROSOFT_KBS.getEndpoint())
                .header(HttpHeader.CONTENT_TYPE, "application/json")
                .content(new StringContentProvider(contents.toString()));
        try {
            request.send();
        } catch (Exception e) {
            logger.error("Could not delete the Microsoft KBs from DB-API.", e);
        }
    }

    /**
     * Takes a list of {@link CpeItem} and regroups them by device id.
     *
     * @param cpeItems The list of {@link CpeItem} to classify.
     * @return A Map that associates a device id with the list of {@link CpeItem}s that have this id.
     */
    static private <T, Id> Map<Id, List<T>> classifyItemsById(List<T> cpeItems, Function<T, Id> idGetter) {
        Map<Id, List<T>> result = new HashMap<>();
        for (T cpeItem : cpeItems) {
            Id deviceId = idGetter.apply(cpeItem);
            if (!result.containsKey(deviceId)) {
                result.put(deviceId, new ArrayList<>());
            }
            result.get(deviceId).add(cpeItem);
        }
        return result;
    }

    /**
     * Send a CPE Items batch to DB-API
     *
     * @param cpeItems The CPE Items to send
     * @throws Exception If the sending fail
     */
    private void sendCpeItemsBatch(List<CpeItem> cpeItems, ScanEntity scan, boolean hasMore) throws Exception {
        Map<String, List<CpeItem>> classifiedCpeItems = classifyItemsById(cpeItems, CpeItem::getDeviceId);
        Json cpeItemsArray = Json.array();
        for (Map.Entry<String, List<CpeItem>> deviceCpeItems : classifiedCpeItems.entrySet()) {
            Json deviceCpeItemsArray = Json.array(
                    deviceCpeItems.getValue().stream().map(CpeItem::serializeForDbapi).toArray()
            );
            cpeItemsArray.add(Json.object(
                    "device", deviceCpeItems.getKey(),
                    "discovered_cpe_list", deviceCpeItemsArray
            ));
        }

        Json requestContents = Json.object(
                "progress", scan.getProgress(),
                "event_id", scan.getDbapiId(),
                "discovered_cpe_dict_arr", cpeItemsArray,
                "has_more", hasMore
        );
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpoint.CREATE_CPE_ITEMS)
                .content(new StringContentProvider(requestContents.toString()), "application/json");
        ContentResponse response = request.send();
        if (response.getStatus() != 200) {
            throw new Exception("Error sending CpeItem Batch to dbapi: got unexpected status " + response.getStatus());
        }
    }

    /**
     * Send a list of CPE Items to DB-API
     *
     * @param cpeItems A {@link List <CpeItem>} with the CPE Items to send
     * @throws Exception If any item failed
     */
    public void sendCpeItems(List<CpeItem> cpeItems, ScanEntity scan, boolean hasMore) throws Exception {
        Json failedItems = Json.array();
        List<CpeItem> newItems = cpeItems.stream().filter(Predicate.not(CpeItem::isDeleted)).collect(Collectors.toList());
        List<CpeItem> deletedItems = cpeItems.stream().filter(CpeItem::isDeleted).collect(Collectors.toList());

        try {
            if (!deletedItems.isEmpty()) {
                deleteCpeItemsFromDbapi(deletedItems);
            }
            sendCpeItemsBatch(newItems, scan, hasMore);
        } catch (Exception e) {
            logger.warn("Error sending CPE Items to DB-API.", e);
            cpeItems.stream().map(CpeItem::getMongoEntityId).forEach(failedItems::add);
            throw new Exception("Error sending the following CPE Items: " + failedItems.toString());
        }
    }

    /**
     * Send a batch of KBs to DB-API
     *
     * @param KBs A {@link List<MicrosoftKB>} with the KBs to send
     * @throws Exception If any item failed
     */
    private void sendMicrosoftKbsBatch(List<MicrosoftKB> KBs, ScanEntity scan) throws Exception {
        Map<String, List<MicrosoftKB>> classifiedKBs = classifyItemsById(KBs, MicrosoftKB::getDeviceId);
        Json KBsArray = Json.array();
        for (Map.Entry<String, List<MicrosoftKB>> deviceKBs : classifiedKBs.entrySet()) {
            Json deviceKBsArray = Json.array(
                    deviceKBs.getValue().stream().map(MicrosoftKB::serializeForDbapi).toArray()
            );
            KBsArray.add(Json.object(
                    "device", deviceKBs.getKey(),
                    "discovered_kb_list", deviceKBsArray
            ));
        }

        Json requestContents = Json.object(
                "progress", scan.getProgress(),
                "event_id", scan.getDbapiId(),
                "discovered_kb_dict_arr", KBsArray
        );
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpoint.CREATE_MICROSOFT_KBS)
                .content(new StringContentProvider(requestContents.toString()), "application/json");
        ContentResponse response = request.send();
        if (response.getStatus() != 200) {
            throw new Exception("Error sending KBs Batch to dbapi: got unexpected status " + response.getStatus());
        }
    }

    public void sendMicrosoftKbs(List<MicrosoftKB> KBs, ScanEntity scan) throws Exception {
        Json failedItems = Json.array();
        List<MicrosoftKB> newItems = KBs.stream().filter(Predicate.not(MicrosoftKB::isDeleted)).collect(Collectors.toList());
        List<MicrosoftKB> deletedItems = KBs.stream().filter(MicrosoftKB::isDeleted).collect(Collectors.toList());

        try {
            if (!deletedItems.isEmpty()) {
                deleteMicrosoftKbsFromDbapi(deletedItems);
            }
            sendMicrosoftKbsBatch(newItems, scan);
        } catch (Exception e) {
            logger.warn("Error sending Microsoft KBs to DB-API.", e);
            KBs.stream().map(MicrosoftKB::getMongoEntityId).forEach(failedItems::add);
            throw new Exception("Error sending the following KBs: " + failedItems.toString());
        }
    }


    /**
     * Fetch the last updated date of CPE Items in DB-API.
     *
     * @return The last update of CPE-Items in DB-API.
     * @throws Exception If it was not possible to fetch from DB-API or the format was not recognised.
     */
    public OffsetDateTime getCpeItemsLastUpdateDate() throws Exception {
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.CPE_ITEMS_LAST_DATE);
        ContentResponse response = request.send();
        Json responseContents = Json.read(response.getContentAsString());
        String lastUpdatedDateString;
        if (responseContents.isString()) {
            lastUpdatedDateString = responseContents.asString();
        } else if (responseContents.isObject()) {
            lastUpdatedDateString = responseContents.get("updatedAt").asString();
        } else {
            lastUpdatedDateString = responseContents.toString();
        }
        return DbapiUtils.dbapiDateToLocal(lastUpdatedDateString);
    }

    /**
     * Fetch the last updated date of Microsoft KBs in DB-API.
     *
     * @return The last update of Microsoft KBs in DB-API.
     * @throws ExecutionException   If the fetch failed.
     * @throws InterruptedException If the connection with DB-API was interrupted.
     * @throws TimeoutException     If the connection with DB-API times out.
     */
    public OffsetDateTime getMicrosoftKbsLastUpdateDate() throws ExecutionException, InterruptedException, TimeoutException {
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.MICROSOFT_KB_LAST_DATE);
        ContentResponse response = request.send();
        Json responseContents = Json.read(response.getContentAsString());
        String lastUpdatedDateString;
        if (responseContents.isString()) {
            lastUpdatedDateString = responseContents.asString();
        } else if (responseContents.isObject()) {
            lastUpdatedDateString = responseContents.get("updatedAt").asString();
        } else {
            lastUpdatedDateString = responseContents.toString();
        }
        return DbapiUtils.dbapiDateToLocal(lastUpdatedDateString);
    }

    /**
     * Get the devices from DB-API that were changed since an optional date.
     *
     * @param date The date of start of modifications to fecth. May be null, in wich case fetches all devices.
     * @return The {@link List<Json>} of devices that were changed since date.
     * @throws Exception If the fetching failed.
     */
    public List<Device> getDevicesSince(OffsetDateTime date) throws Exception {
        OffsetDateTime dateUtc = DbapiUtils.localDateToDbapi(date);
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.DEVICES);
        if (dateUtc != null) {
            request.param("updated_at__gt", dateUtc.toString());
        }
        Json response = Json.read(request.send().getContentAsString());

        List<Device> devices = response.asJsonList().stream()
                .map(Device::fromJson)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return devices;
    }


    /**
     * Get the connections from DB-API that were changed since an optional date.
     *
     * @param date The date of start of modifications to fecth. May be null, in wich case fetches all connections.
     * @return The {@link List<Json>} of connections that were changed since date.
     * @throws Exception If the fetching failed.
     */
    public List<Connection> getConnectionsSince(OffsetDateTime date, List<ConnectionProtocol> protocols) throws Exception {
        OffsetDateTime dateUtc = DbapiUtils.localDateToDbapi(date);
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.CONNECTIONS);
        if (dateUtc != null) {
            request.param("updated_at__gt", dateUtc.toString());
        }
        Json response = Json.read(request.send().getContentAsString());
        return response.asJsonList().stream()
                .map(json -> Connection.fromDbapiJson(json, protocols))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    /**
     * Get the deleted devices from DB-API that were changed since an optional date.
     *
     * @param date The date of start of deletions to fecth. May be null, in wich case fetches all deletions.
     * @return The {@link List<String>} of device uuids that were deleted since date.
     * @throws Exception If the fetching failed.
     */
    public List<Pair<String, OffsetDateTime>> getDeletedDevicesSince(OffsetDateTime date) throws Exception {
        OffsetDateTime dateUtc = DbapiUtils.localDateToDbapi(date);
        List<Pair<String, OffsetDateTime>> deletedDevices = new ArrayList<>();
        boolean hasMore = true;
        int offset = 0;
        while (hasMore) {
            Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.DELETED_DEVICES)
                    .param("offset", String.valueOf(offset))
                    .param("limit", String.valueOf(this.maxPageSize));
            if (dateUtc != null) {
                request.param("deleted_date__gt", dateUtc.toString());
            }
            Json response = Json.read(request.send().getContentAsString());
            for (Json deletedDevice : response.get("results").asJsonList()) {
                String uuid = deletedDevice.get("object_id").asString();
                OffsetDateTime deletedDate = DbapiUtils.dbapiDateToLocal(deletedDevice.get("deleted_at").asString());
                deletedDevices.add(new Pair<>(uuid, deletedDate));
            }
            hasMore = !response.get("next").isNull();
            offset += this.maxPageSize;
        }
        return deletedDevices;
    }

    /**
     * Get the deleted CPE Items from DB-API that were changed since an optional date.
     *
     * @param date The date of start of deletions to fecth. May be null, in wich case fetches all deletions.
     * @return The {@link List<String>} of CPE Item uuids that were deleted since date.
     * @throws Exception If the fetching failed.
     */
    public List<Pair<String, OffsetDateTime>> getDeletedCpeItemsSince(OffsetDateTime date, int limit, int offset) throws Exception {
        OffsetDateTime dateUtc = DbapiUtils.localDateToDbapi(date);
        List<Pair<String, OffsetDateTime>> deletedCpeItems = new ArrayList<>();

        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.GET_DELETED_CPE_ITEMS);
        if (offset > 0) {
            request.param("offset", String.valueOf(offset));
        }
        if (limit > 0) {
            request.param("limit", String.valueOf(limit));
        }
        if (dateUtc != null) {
            request.param("deleted_date__gt", dateUtc.toString());
        }

        ContentResponse response = request.send();
        if (response.getStatus() != 200) {
            throw new Exception("Unexpected status code " + response.getStatus());
        }

        Json responseContents = Json.read(response.getContentAsString());
        List<Json> deletedCpeItemsPageJson = responseContents.get("results").asJsonList();

        // If the list is smaller than the max page size, there are no more pages
//            hasMore = deletedCpeItemsPageJson.size() == this.maxPageSize;

        deletedCpeItemsPageJson.stream()
                .map(json -> new Pair<>(json.get("object_repr").asString(), DbapiUtils.dbapiDateToLocal(json.get("deleted_at").asString())))
                .forEach(deletedCpeItems::add);

        return deletedCpeItems;
    }

    /**
     * Get the deleted Microsoft KBs from DB-API that were changed since an optional date.
     *
     * @param date The date of start of deletions to fecth. May be null, in wich case fetches all deletions.
     * @return The {@link List<String>} of Microsoft KB uuids that were deleted since date.
     * @throws Exception If the fetching failed.
     */
    public List<Pair<String, OffsetDateTime>> getDeletedMicrosoftKbsSince(OffsetDateTime date, int limit, int offset) throws Exception {
        OffsetDateTime dateUtc = DbapiUtils.localDateToDbapi(date);
        List<Pair<String, OffsetDateTime>> deletedMicrosoftKbs = new ArrayList<>();

        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.GET_DELETED_MICROSOFT_KBS);
        if (offset > 0) {
            request.param("offset", String.valueOf(offset));
        }
        if (limit > 0) {
            request.param("limit", String.valueOf(limit));
        }
        if (dateUtc != null) {
            request.param("deleted_date__gt", dateUtc.toString());
        }

        ContentResponse response = request.send();
        if (response.getStatus() != 200) {
            throw new Exception("Unexpected status code " + response.getStatus());
        }

        Json responseContents = Json.read(response.getContentAsString());
        List<Json> deletedMicrosoftKbsPageJson = responseContents.get("results").asJsonList();

        // If the list is smaller than the max page size, there are no more pages

        deletedMicrosoftKbsPageJson.stream()
                .map(json -> new Pair<>(json.get("object_repr").asString(), DbapiUtils.dbapiDateToLocal(json.get("deleted_at").asString())))
                .forEach(deletedMicrosoftKbs::add);

        return deletedMicrosoftKbs;
    }

    /**
     * Fetch a list of devices from DB-API.
     *
     * @param uuids The {@link List<Integer>} of IDs of devices to retrieve from DB-API
     * @return The {@link List<Json>} of devices fetched from DB-API
     * @throws ExecutionException   If the fetch failed.
     * @throws InterruptedException If the connection with DB-API was interrupted.
     * @throws TimeoutException     If the connection with DB-API times out.
     */
    public List<Device> fetchDevices(List<String> uuids) throws ExecutionException, InterruptedException, TimeoutException {
        List<Device> devices = new ArrayList<>();
        if (uuids == null || uuids.isEmpty()) {
            return devices;
        }
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.DEVICES);
        request.param("uuid", String.join(",", uuids));
        Json response = Json.read(request.send().getContentAsString());
        response.asJsonList().stream()
                .map(Device::fromJson)
                .filter(Objects::nonNull)
                .forEach(devices::add);
        return devices;
    }

    /**
     * Fetch a list of connections from DB-API.
     *
     * @param ids The {@link List<Integer>} of IDs of connections to retrieve from DB-API
     * @return The {@link List<Json>} of connections fetched from DB-API
     * @throws ExecutionException   If the fetch failed.
     * @throws InterruptedException If the connection with DB-API was interrupted.
     * @throws TimeoutException     If the connection with DB-API times out.
     */
    public List<Connection> fetchConnections(List<Integer> ids, List<ConnectionProtocol> protocols) throws ExecutionException, InterruptedException, TimeoutException {
        List<Connection> connections = new ArrayList<>();
        for (int id : ids) {
            Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.CONNECTIONS);
            request.param("id", String.valueOf(id));
            Json response = Json.read(request.send().getContentAsString());
            Connection connection;
            if (response.isArray()) {
                connection = Connection.fromDbapiJson(response.at(0), protocols);
            } else {
                connection = Connection.fromDbapiJson(response, protocols);
            }
            if (connection != null) {
                connections.add(connection);
            }
        }
        return connections;
    }

    /**
     * Fetch the list discovery protocols from DB-API.
     *
     * @return The {@link List<Json>} of protocols fetched from DB-API
     * @throws ExecutionException   If the fetch failed.
     * @throws InterruptedException If the connection with DB-API was interrupted.
     * @throws TimeoutException     If the connection with DB-API times out.
     */
    public List<ConnectionProtocol> fetchDiscoveryProtocols() throws ExecutionException, InterruptedException, TimeoutException {
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.DISCOVERY_PROTOCOLS);
        return Json.read(request.send().getContentAsString()).asJsonList().stream()
                .map(ConnectionProtocol::fromJson)
                .collect(Collectors.toList());
    }

    /**
     * Create a discovery protocol in DB-API. Used when updating an HTTP template.
     *
     * @param entityHttpConnection The created HTTP template.
     * @throws Exception If the creation failed.
     */
    public void createDiscoveryProtocol(EntityHttpConnection entityHttpConnection) throws Exception {
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpoint.DISCOVERY_PROTOCOLS);
        Json requestContents = Json.object(
                ConnectionProtocolField.NAME.dbapiName(), entityHttpConnection.getName(),
                ConnectionProtocolField.IS_DYNAMIC.dbapiName(), true,
                ConnectionProtocolField.DEFAULT_PORT.dbapiName(), 443,
                ConnectionProtocolField.CONNECTION_TEMPLATE_ID.dbapiName(), entityHttpConnection.getUuid(),
                ConnectionProtocolField.CONNECTION_TEMPLATE_DETAILS.dbapiName(), entityHttpConnection.serializeForDbapi()
        );
        request.content(new StringContentProvider(requestContents.toString()), "application/json");
        ContentResponse response = request.send();
        if (response.getStatus() >= 400) {
            throw new Exception("Error creating discovery protocol: got unexpected status " + response.getStatus());
        }
    }

    /**
     * Update a discovery protocol in DB-API. Used when updating an HTTP template.
     *
     * @param entityHttpConnection The updated HTTP template.
     * @throws Exception If the update failed.
     */
    public void updateDiscoveryProtocol(EntityHttpConnection entityHttpConnection) throws Exception {
        String templateId = entityHttpConnection.getUuid();
        ConnectionProtocol protocol = getDiscoveryProtocolByTemplateId(templateId);
        if (protocol == null) {
            createDiscoveryProtocol(entityHttpConnection);
        } else {
            int protocolId = protocol.getId();
            Request request = createDbapiRequest(HttpMethod.PUT, String.format(DbapiEndpoint.DISCOVERY_PROTOCOLS_DETAILS.getEndpoint(), protocolId));
            Json requestContents = Json.object(
                    ConnectionProtocolField.NAME.dbapiName(), entityHttpConnection.getName(),
                    ConnectionProtocolField.IS_DYNAMIC.dbapiName(), true,
                    ConnectionProtocolField.DEFAULT_PORT.dbapiName(), 443,
                    ConnectionProtocolField.CONNECTION_TEMPLATE_ID.dbapiName(), entityHttpConnection.getUuid(),
                    ConnectionProtocolField.CONNECTION_TEMPLATE_DETAILS.dbapiName(), entityHttpConnection.serializeForDbapi()
            );
            request.content(new StringContentProvider(requestContents.toString()), "application/json");
            ContentResponse response = request.send();
            if (response.getStatus() >= 400) {
                throw new Exception("Error updating discovery protocol: got unexpected status " + response.getStatus());
            }
        }
    }

    /**
     * Delete a discovery protocol in DB-API. Used when deleting an HTTP template.
     *
     * @param uuid The UUID of the HTTP template to delete.
     * @throws Exception If the deletion failed.
     */
    public void deleteDiscoveryProtocol(String uuid) throws Exception {
        ConnectionProtocol protocol = getDiscoveryProtocolByTemplateId(uuid);
        if (protocol != null) {
            int protocolId = protocol.getId();
            Request request = createDbapiRequest(HttpMethod.DELETE, String.format(DbapiEndpoint.DISCOVERY_PROTOCOLS_DETAILS.getEndpoint(), protocolId));
            ContentResponse response = request.send();
            if (response.getStatus() >= 400) {
                throw new Exception("Error deleting discovery protocol: got unexpected status " + response.getStatus());
            }
        }
    }

    public ConnectionProtocol getDiscoveryProtocolByTemplateId(String id) {
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.DISCOVERY_PROTOCOLS_DETAILS_BY_TEMPLATE_ID);
        request.param("connection_template_id", id);
        try {
            Json response = Json.read(request.send().getContentAsString());
            if (response.isArray()) {
                return ConnectionProtocol.fromJson(response.at(0));
            } else {
                return ConnectionProtocol.fromJson(response);
            }
        } catch (Exception e) {
            logger.error("Could not get discovery protocol by template id.", e);
            return null;
        }
    }

    /**
     * Inform DB-API that a scan has started.
     *
     * @param startDate The starting time of the scan.
     * @return The id attributed by DB-API to the scan object.
     */
    public int notifyScanStarted(OffsetDateTime startDate) {
        Json params = Json.object("started_at", DbapiUtils.localDateToDbapi(startDate).toString());
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpoint.SCAN_EVENT_CREATION)
                .header(HttpHeader.CONTENT_TYPE, "application/json")
                .content(new StringContentProvider(params.toString()));
        try {
            ContentResponse response = request.send();
            return JsonUtil.getIntFromJson(Json.read(response.getContentAsString()), "id", 0);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            return 0;
        }
    }

    /**
     * Inform DB-API that a scan just started.
     *
     * @return The id attributed by DB-API to the scan object.
     */
    public int notifyScanStarted() {
        return notifyScanStarted(OffsetDateTime.now());
    }

    /**
     * Notify DB-API that a scan is finished.
     *
     * @param scan The scan that ended.
     * @throws ExecutionException   If the sending failed.
     * @throws InterruptedException If the sending was interrupted.
     * @throws TimeoutException     If the sending timed out.
     * @throws Exception            If received an unexpected HTTP status code (ie. != 200) or if the JSON was malformed.
     */
    public void notifyScanFinished(ScanEntity scan) throws Exception {
        // Do nothing if the scan is not actually finished or was not registered in DB-API.
        if (!scan.isFinished()) return;
        if (scan.getDbapiId() <= 0) return;

        Request request = createDbapiPatchRequest(String.format(DbapiEndpoint.SCAN_EVENT_UPDATE.getEndpoint(), scan.getDbapiId()));
        Json params = Json.object("ended_at", DbapiUtils.localDateToDbapi(scan.getEndDate()).toString());
        FinishedScanStatus status;
        if (scan.isSuccess()) {
            status = FinishedScanStatus.FINISHED_SUCCESS;
        } else if (scan.isFailure()) {
            status = FinishedScanStatus.FINISHED_ERROR;
        } else {
            status = FinishedScanStatus.DISCARDED;
        }
        params.set("status", status.getDbapiCode());

        if (scan.getDescription() != null) {
            params.set("description", scan.getDescription());
        }
        request.content(new StringContentProvider(params.toString()));
        ContentResponse response = request.send();
        if (response.getStatus() != 200) {
            throw new Exception("Unexpected status code: " + response.getStatus());
        }
    }

    /**
     * Notify DB-API we have sent all the CPE Items discovered by a scan.
     *
     * @param scan The scan that ended.
     * @throws ExecutionException   If the sending failed.
     * @throws InterruptedException If the sending was interrupted.
     * @throws TimeoutException     If the sending timed out.
     * @throws Exception            If received an unexpected HTTP status code (i.e. != 200) or if the JSON was malformed.
     */
    public void notifySynchronisationEnded(ScanEntity scan) throws Exception {
        Request request = createDbapiPatchRequest(String.format(DbapiEndpoint.SCAN_EVENT_UPDATE.getEndpoint(), scan.getDbapiId()));
        Json params = Json.object(
                "is_synchronization_ended", true
        );

        request.content(new StringContentProvider(params.toString()));
        ContentResponse response = request.send();
        if (response.getStatus() != 200) {
            throw new Exception("Unexpected status code: " + response.getStatus());
        }
    }

    /**
     * Send a request to DB-API to inform it the current scan provided no new CPE Item.
     */
    public void notifyNoNewCpe() {
        Request request = this.createDbapiRequest(HttpMethod.GET, DbapiEndpoint.NO_NEW_CPE_ITEM);
        try {
            request.send();
        } catch (Exception e) {
            logger.error("Could not send the no new CPE Item notification to DB-API.", e);
        }
    }

    /**
     * Cancel all scan events in DB-API.
     */
    public void cancelAllScans() {
        Request request = this.createDbapiRequest(HttpMethod.GET, DbapiEndpoint.EVENTS_CANCEL_ALL);
        try {
            request.send();
        } catch (Exception e) {
            logger.error("Could not send the cancel all scans notification to DB-API.", e);
        }
    }

    /**
     * Get the organization name from DB-API.
     *
     * @return The organization name. Defaults to "None" if the request failed.
     */
    public String getOrganizationName() {
        Request request = this.createDbapiRequest(HttpMethod.GET, DbapiEndpoint.GET_ORGANIZATION_NAME);
        try {
            ContentResponse response = request.send();
            return response.getContentAsString();
        } catch (Exception e) {
            logger.warn("Could not get the organization name from DB-API.", e);
            return "None";
        }
    }

    public String getMqttTopicPrefix() {
        Request request = this.createDbapiRequest(HttpMethod.GET, DbapiEndpoint.GET_MQTT_TOPIC_PREFIX);
        try {
            ContentResponse response = request.send();
            String result = response.getContentAsString();
            if (result == null || result.isEmpty()) {
                return "None";
            } else {
                if (result.startsWith("\"") && result.endsWith("\""))
                    return result.substring(1, result.length() - 1);
                return result;
            }
        } catch (Exception e) {
            logger.warn("Could not get the MQTT topic prefix from DB-API.", e);
            return "None";
        }
    }

    /**
     * Create a DB-API request. Most notably, adds the API key to the request header.
     *
     * @param method   The HTTP method to use (GET, POST, ...)
     * @param endpoint The endpoint to contact in DB-API.
     * @return The crafted {@link Request}.
     */
    private Request createDbapiRequest(HttpMethod method, String endpoint) {
        return dbapiHttpClient.newRequest(dbapiUrl + endpoint)
                .method(method)
                .header(HttpHeader.AUTHORIZATION, "Api-Key " + apiKey);
    }

    /**
     * Create a DB-API request. Most notably, adds the API key to the request header.
     *
     * @param method   The HTTP method to use (GET, POST, ...)
     * @param endpoint The endpoint to contact in DB-API.
     * @return The crafted {@link Request}.
     */
    private Request createDbapiRequest(HttpMethod method, DbapiEndpoint endpoint) {
        return createDbapiRequest(method, endpoint.getEndpoint());
    }

    /**
     * Create a DB-API request. Most notably, adds the API key to the request header.
     *
     * @param method   The HTTP method to use (GET, POST, ...)
     * @param endpoint The endpoint to contact in DB-API.
     * @param id       Additional argument.
     * @return The crafted {@link Request}, pointing to the endpoint <code>endpoint/id</code>.
     */
    private Request createDbapiRequest(HttpMethod method, DbapiEndpoint endpoint, String id) {
        return createDbapiRequest(method, endpoint.getEndpoint() + "/" + id);
    }

    /**
     * Create a DB-API PATCH request. Most notably, adds the API key to the request header.
     * Exists because PATCH is absent from the Jetty methods enum.
     *
     * @param endpoint The endpoint to contact in DB-API.
     * @return The crafted {@link Request}.
     */
    private Request createDbapiPatchRequest(String endpoint) {
        return dbapiHttpClient.newRequest(dbapiUrl + endpoint)
                .method("PATCH")
                .header(HttpHeader.CONTENT_TYPE, "application/json")
                .header(HttpHeader.AUTHORIZATION, "Api-Key " + apiKey);
    }

    /**
     * Create a DB-API PATCH request. Most notably, adds the API key to the request header.
     * Exists because PATCH is absent from the Jetty methods enum.
     *
     * @param endpoint The endpoint to contact in DB-API.
     * @return The crafted {@link Request}.
     */
    private Request createDbapiPatchRequest(DbapiEndpoint endpoint) {
        return createDbapiPatchRequest(endpoint.getEndpoint());
    }

    /**
     * Create a DB-API PATCH request. Most notably, adds the API key to the request header.
     * Exists because PATCH is absent from the Jetty methods enum.
     *
     * @param endpoint The endpoint to contact in DB-API.
     * @param id       Additional argument.
     * @return The crafted {@link Request}, pointing to the endpoint <code>endpoint/id</code>.
     */
    private Request createDbapiPatchRequest(DbapiEndpoint endpoint, String id) {
        return createDbapiPatchRequest(endpoint.getEndpoint() + '/' + id);
    }

    /**
     * Send a device to CSL-Scan.
     * First tries to create a new one, and on failure tries to modify the device (assuming it already exists).
     *
     * @param newDevice      The device to send.
     * @param scanApiHandler The interface of the scanner's API.
     * @throws Exception If we were not able to send the device to CSL-Scan, that is neither creating a new one nor modify an existing one worked.
     */
    private void sendNewDeviceToScanner(Device newDevice, ScanApiHandler scanApiHandler) throws Exception {
        JsonApiResponse result = scanApiHandler.createOrUpdateEntity(newDevice);
        if (!result.isSuccess()) {
            throw new Exception("Could not push the entity " + newDevice.getId() + " to CSL-Scan.");
        }
    }

    /**
     * Handle the changes in the devices on DB-API.
     *
     * @param scanApiHandler The interface of the scanner's API.
     * @return A {@link Json} containing the result (success or failure).
     */
    public JsonApiResponse sendNewDevicesToScanner(ScanApiHandler scanApiHandler) {
        List<Device> newDevices;
        List<Pair<String, OffsetDateTime>> deletedDevices;
        List<String> failedDevices = new ArrayList<>();
        //region Get changes from DB-API
        try {
            OffsetDateTime lastDeviceModification = scanApiHandler.getLastLastEntityUpdateDate();
            List<ConnectionProtocol> protocols = fetchDiscoveryProtocols();
            newDevices = buildNewDevices(
                    getDevicesSince(lastDeviceModification),
                    getConnectionsSince(lastDeviceModification, protocols),
                    protocols
            );
            OffsetDateTime lastEntitiesDeletionDate = scanApiHandler.getLastEntitiesDeletionDate();
            deletedDevices = new ArrayList<>(getDeletedDevicesSince(lastEntitiesDeletionDate));
        } catch (Exception e) {
            logger.error("Could not get changes from DB-API.", e);
            return JsonApiResponse.error("Could not get changes from DBAPI");
        }
        //endregion Get changes from DB-API

        //region Send changed devices to CSL-Scan
        for (Device newDevice : newDevices) {
            try {
                sendNewDeviceToScanner(newDevice, scanApiHandler);
            } catch (Exception e) {
                failedDevices.add(newDevice.getId());
            }
        }
        //endregion Send changed devices to CSL-Scan

        // Delete devices from CSL-Scan
        try {
            failedDevices.addAll(scanApiHandler.deleteEntities(deletedDevices));
        } catch (Exception e) {
            return JsonApiResponse.error("Could not delete devices from CSL-Scan" + e.getMessage());
        }

//        if (failedDevices.isEmpty()) {
//            scanApiHandler.sendNewCpeItemsToDbapi(this);
//            scanApiHandler.sendNewMicrosoftKbsToDbapi(this);
//        }

        return failedDevices.isEmpty()
                ? JsonApiResponse.success()
                : JsonApiResponse.error(
                "Failed to send updated devices to CSL-Scan",
                Json.object("failed_devices", Json.array(failedDevices.toArray()))
        );
    }

    /**
     * Create the list of entities to update on CSL-Scan.
     *
     * @param devices     The list of devices that were created or modified.
     * @param connections The list of connections that were created or modified.
     * @return A {@link Json} with the entities to send to CSL-Scan and the ones to delete, in the format
     * <code>
     * {
     * "scan_entities": [Json objects],
     * "devices_to_delete": [id (strings)]
     * }
     * </code>
     */
    private List<Device> buildNewDevices(List<Device> devices, List<Connection> connections, List<ConnectionProtocol> protocols) {
        //region List the uuids we have in both list
        List<Integer> connectionUuidsInDevices = new ArrayList<>();
        List<String> deviceUuidsInConnections = new ArrayList<>();

        for (Device device : devices) {
            List<Integer> connectionsIds = device.getConnectionsIds();
            connectionUuidsInDevices.addAll(connectionsIds);
        }
        for (Connection connection : connections) {
            deviceUuidsInConnections.addAll(connection.getDevicesIds());
        }
        //endregion List the uuids we have in both list

        //region Check for the ones missing on one side or the other
        List<Integer> connectionsToGet = new ArrayList<>();
        List<String> devicesToGet = new ArrayList<>();

        for (int connectionId : connectionUuidsInDevices) {
            if (DbapiUtils.getConnectionById(connections, connectionId) == null) {
                connectionsToGet.add(connectionId);
            }
        }
        for (String deviceId : deviceUuidsInConnections) {
            if (DbapiUtils.getDeviceById(devices, deviceId) == null) {
                devicesToGet.add(deviceId);
            }
        }
        //endregion Check for the ones missing on one side or the other

        //region Get the missing parts
        try {
            connections.addAll(fetchConnections(connectionsToGet, protocols));
            devices.addAll(fetchDevices(devicesToGet));
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            logger.error("Could not fetch missing parts from DB-API.", e);
        }
        //endregion Get the missing parts

        // Fill the connections into devices
        devices.forEach(device -> device.setConnections(connections));

        return devices;
    }

    public void sendCommandsList(List<IApiCommands> apiCommandsList) throws Exception {
        Json requestContents = Json.object();
        apiCommandsList.stream()
                .map(apiCommands -> new Pair<>(apiCommands.getName(), apiCommands.getListOfCommandPrivileges()))
                .filter(Predicate.not(pair -> pair.getSecond().isEmpty()))
                .map(pair -> pair.map((name, map) -> {
                    Json result = Json.object();
                    map.forEach((key, value) -> result.set(key, value.toString()));
                    return new Pair<>(name, result);
                }))
                .forEach(pair -> requestContents.set(pair.getFirst(), pair.getSecond()));
        logger.debug("Sending commands to DB-API: " + requestContents.toString());
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpoint.JAVACOMM_SEND_COMMANDS)
                .content(new StringContentProvider(requestContents.toString()), "application/json");
        ContentResponse response = request.send();
        if (response.getStatus() != 200) {
            throw new Exception("Error sending commands to dbapi: got unexpected status " + response.getStatus());
        }
    }
}
