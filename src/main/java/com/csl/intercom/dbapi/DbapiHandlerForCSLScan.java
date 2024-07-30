package com.csl.intercom.dbapi;

import com.csl.core.CSLContext;
import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.models.*;
import com.csl.intercom.cslscan.models.scans.ExternalScan;
import com.csl.intercom.dbapi.enums.ConnectionProtocolField;
import com.csl.intercom.dbapi.enums.DbapiEndpointForCSLScan;
import com.csl.intercom.dbapi.enums.FileActionStatus;
import com.csl.intercom.dbapi.enums.FinishedScanStatus;
import com.csl.intercom.dbapi.exceptions.DbapiUnexpectedStatusCodeException;
import com.csl.intercom.dbapi.models.*;
import com.csl.util.FileStorageService;
import com.csl.util.Pair;
import com.ucsl.interfaces.IAlertDescriptor;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.JsonApiResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.client.util.MultiPartContentProvider;
import org.eclipse.jetty.client.util.PathContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.csl.intercom.dbapi.enums.StaticConnectionProtocol.*;

/**
 * Manage HTTP communications with DB-API.
 * Provides an interface for retrieving the devices, connections and so on,
 * and to send information to it (CPE Items, a Scan's status, ...).
 */
public class DbapiHandlerForCSLScan extends DbapiHandler {
    private String dbapiUrl;
    private String apiKey;
    private HttpClient dbapiHttpClient = new HttpClient();
    private final int maxPageSize = 1000;
    private static final Logger logger = LoggerFactory.getLogger(DbapiHandler.class);
    private final FileStorageService fileStorageService = new FileStorageService();

//    public DbapiHandlerForCSLScan() {
//        this(CSLContext.instance.getConfig());
//    }
//
//    public DbapiHandlerForCSLScan(Json config) {
//        ensureSSLDbApiHandlerInitialization();
//        Json globalConfig = config.get("global");
//        dbapiUrl = JsonUtil.getBooleanFromJson(globalConfig, "use_ssl", true) ? "https://" : "http://";
////        dbapiUrl += JsonUtil.getStringFromJson(globalConfig, "ip_server_remote", "localhost");
//        dbapiUrl += JsonUtil.getStringFromJson(globalConfig, "ip_server_remote", "localhost");
//        dbapiUrl += "/api";
//        apiKey = JsonUtil.getStringFromJson(globalConfig, "api_key", "");
//        try {
//            dbapiHttpClient.start();
//        } catch (Exception e) {
//            logger.error("Could not start the DB-API HTTP client.", e);
//        }
//    }
//
//    private void ensureSSLDbApiHandlerInitialization(){
//        // Retrieve system properties
//        String trustStorePath = System.getProperty("javax.net.ssl.trustStore");
//        String trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
//
//        // Ensure the properties are set
//        if (trustStorePath == null || trustStorePassword == null) {
//            throw new IllegalStateException("Trust store properties are not set.");
//        }
//
//        // Configure SslContextFactory with the retrieved properties
//        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
//        sslContextFactory.setTrustStorePath(trustStorePath);
//        sslContextFactory.setTrustStorePassword(trustStorePassword);
//        //sslContextFactory.setTrustAll(true);
//
//        dbapiHttpClient = new HttpClient(sslContextFactory);
//    }
//
//    public void close() {
//        try {
//            dbapiHttpClient.stop();
//        } catch (Exception e) {
//            logger.error("Could not stop the DB-API HTTP client.", e);
//        }
//    }


    public DbapiHandlerForCSLScan() {
        this("CSLScan", CSLContext.instance.getConfig());
    }

    public DbapiHandlerForCSLScan(String moduleName, Json config) {
        super(moduleName, CSLContext.instance.getConfig());
    }

    public DbapiHandlerForCSLScan(Json config) {
        this("CSLScan", CSLContext.instance.getConfig());
    }


    /**
     * Insert a new alert into DB
     *
     * @param alert alert to insert
     * @return the content of the response
     */
    public String insertAlert(IAlertDescriptor alert) {
        String res = null;
        Request request = createDbapiRequest(HttpMethod.POST, "/alerts")
                .header(HttpHeader.CONTENT_TYPE, "application/json")
                .content(new StringContentProvider(alert.toJson().toString()));
        try {
            ContentResponse response = request.send();
            res = new String(response.getContent());
        } catch (Exception e) {
            logger.error("Could not delete the CPE Items from DB-API.", e);
        }
        return res;
    }


    /**
     * Remove a list of CPE Items from DB-API.
     *
     * @param deletedItems The list of CPE Items to remove in DB-API.
     */
    private void deleteCpeItemsFromDbapi(List<CpeItem> deletedItems) {
        Json contents = Json.object("mongo_entity_ids", Json.array(deletedItems.stream().map(CpeItem::getMongoEntityId).toArray()));
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.DELETE_CPE_ITEMS.getEndpoint())
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
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.DELETE_MICROSOFT_KBS.getEndpoint())
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
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.CREATE_CPE_ITEMS)
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
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.CREATE_MICROSOFT_KBS)
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
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.CPE_ITEMS_LAST_DATE);
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
        return DbapiUtilsForCSLScan.dbapiDateToLocal(lastUpdatedDateString);
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
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.MICROSOFT_KB_LAST_DATE);
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
        return DbapiUtilsForCSLScan.dbapiDateToLocal(lastUpdatedDateString);
    }

    /**
     * Get the devices from DB-API that were changed since an optional date.
     *
     * @param date The date of start of modifications to fecth. May be null, in wich case fetches all devices.
     * @return The {@link List<Json>} of devices that were changed since date.
     * @throws Exception If the fetching failed.
     */
    public List<Device> getDevicesSince(OffsetDateTime date) throws Exception {
        OffsetDateTime dateUtc = DbapiUtilsForCSLScan.localDateToDbapi(date);
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.DEVICES);
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
        OffsetDateTime dateUtc = DbapiUtilsForCSLScan.localDateToDbapi(date);
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.CONNECTIONS);
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
        OffsetDateTime dateUtc = DbapiUtilsForCSLScan.localDateToDbapi(date);
        List<Pair<String, OffsetDateTime>> deletedDevices = new ArrayList<>();
        boolean hasMore = true;
        int offset = 0;
        while (hasMore) {
            Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.DELETED_DEVICES)
                    .param("offset", String.valueOf(offset))
                    .param("limit", String.valueOf(this.maxPageSize));
            if (dateUtc != null) {
                request.param("deleted_date__gt", dateUtc.toString());
            }
            Json response = Json.read(request.send().getContentAsString());
            for (Json deletedDevice : response.get("results").asJsonList()) {
                String uuid = deletedDevice.get("object_id").asString();
                OffsetDateTime deletedDate = DbapiUtilsForCSLScan.dbapiDateToLocal(deletedDevice.get("deleted_at").asString());
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
        OffsetDateTime dateUtc = DbapiUtilsForCSLScan.localDateToDbapi(date);
        List<Pair<String, OffsetDateTime>> deletedCpeItems = new ArrayList<>();

        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.GET_DELETED_CPE_ITEMS);
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
                .map(json -> new Pair<>(json.get("object_repr").asString(), DbapiUtilsForCSLScan.dbapiDateToLocal(json.get("deleted_at").asString())))
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
        OffsetDateTime dateUtc = DbapiUtilsForCSLScan.localDateToDbapi(date);
        List<Pair<String, OffsetDateTime>> deletedMicrosoftKbs = new ArrayList<>();

        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.GET_DELETED_MICROSOFT_KBS);
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
                .map(json -> new Pair<>(json.get("object_repr").asString(), DbapiUtilsForCSLScan.dbapiDateToLocal(json.get("deleted_at").asString())))
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
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.DEVICES);
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
    public List<Connection> fetchConnections(List<String> ids, List<ConnectionProtocol> protocols) throws ExecutionException, InterruptedException, TimeoutException {
        List<Connection> connections = new ArrayList<>();
        for (String id : ids) {
            Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.CONNECTIONS);
            request.param("id", id);
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
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.DISCOVERY_PROTOCOLS);
        return Json.read(request.send().getContentAsString()).asJsonList().stream()
                .map(ConnectionProtocol::fromJson)
                .collect(Collectors.toList());
    }
    public ConnectionProtocol fetchDiscoveryProtocol(String protocolName) throws ExecutionException, InterruptedException, TimeoutException {
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.DISCOVERY_PROTOCOL_DETAILS_BY_NAME);
        request.param("name", protocolName);
        Json response = Json.read(request.send().getContentAsString());
        return ConnectionProtocol.fromJson(response);
    }

    /**
     * Create a discovery protocol in DB-API. Used when updating an HTTP template.
     *
     * @param entityHttpConnection The created HTTP template.
     * @throws Exception If the creation failed.
     */
    public void createDiscoveryProtocol(EntityHttpConnection entityHttpConnection) throws Exception {
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.DISCOVERY_PROTOCOLS);
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
            Request request = createDbapiRequest(HttpMethod.PUT, String.format(DbapiEndpointForCSLScan.DISCOVERY_PROTOCOLS_DETAILS.getEndpoint(), protocolId));
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
            Request request = createDbapiRequest(HttpMethod.DELETE, String.format(DbapiEndpointForCSLScan.DISCOVERY_PROTOCOLS_DETAILS.getEndpoint(), protocolId));
            ContentResponse response = request.send();
            if (response.getStatus() >= 400) {
                throw new Exception("Error deleting discovery protocol: got unexpected status " + response.getStatus());
            }
        }
    }

    public void deleteDiscoveryProtocolsList(List<String> uuids) {
        uuids.forEach(uuid -> {
            try {
                deleteDiscoveryProtocol(uuid);
            } catch (Exception e) {
                logger.error("Error deleting discovery protocol", e);
            }
        });
    }

    public ConnectionProtocol getDiscoveryProtocolByTemplateId(String id) {
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.DISCOVERY_PROTOCOLS_DETAILS_BY_TEMPLATE_ID);
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

    // region External discovery
    public void createOrUpdateExternalConnectionInfoTemplates(List<ExternalConnectionInfoTemplate> externalConnectionInfoTemplates) throws DbapiUnexpectedStatusCodeException, ExecutionException, InterruptedException, TimeoutException {
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.EXTERNAL_CONNECTION_INFO_TEMPLATES_CREATE_OR_UPDATE);
        Json requestContents = Json.array(externalConnectionInfoTemplates.stream().map(ExternalConnectionInfoTemplate::serializeForDbapi).toArray());
        request.content(new StringContentProvider(requestContents.toString()), "application/json");
        ContentResponse response = request.send();
        if (response.getStatus() >= 400) {
            throw new DbapiUnexpectedStatusCodeException("Could not create or update external connection info templates.", response.getStatus());
        }
    }

    public void createOrUpdateExternalConnectionInfos(List<ExternalConnectionInfo> externalConnectionInfos) throws DbapiUnexpectedStatusCodeException, ExecutionException, InterruptedException, TimeoutException {
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.EXTERNAL_CONNECTION_INFO_CREATE_OR_UPDATE);
        Json requestContents = Json.array(externalConnectionInfos.stream().map(ExternalConnectionInfo::serializeForDbapi).toArray());
        request.content(new StringContentProvider(requestContents.toString()), "application/json");
        ContentResponse response = request.send();
        if (response.getStatus() >= 400) {
            throw new DbapiUnexpectedStatusCodeException("Could not create or update external connection infos.", response.getStatus());
        }
    }

    public void deleteExternalConnectionInfo(String id) throws DbapiUnexpectedStatusCodeException, ExecutionException, InterruptedException, TimeoutException {
        Request request = createDbapiRequest(HttpMethod.DELETE, String.format(DbapiEndpointForCSLScan.EXTERNAL_CONNECTION_INFO_DETAILS.getEndpoint(), id));
        ContentResponse response = request.send();
        if (response.getStatus() >= 400) {
            throw new DbapiUnexpectedStatusCodeException("Could not delete external connection info.", response.getStatus());
        }
    }

    public OffsetDateTime getExternalConnectionInfoTemplatesLastUpdateDate() {
        logger.debug("Fetching external connection info templates last update date from DB-API.");
        logger.warn("NOT IMPLEMENTED YET.");

        return null;
    }

    public void createOrUpdateExternalDiscoveredDevices(List<ExternalDiscoveredDevice> externalDiscoveredDevices) {
        logger.debug("Sending external discovered devices to DB-API.");
        logger.warn("NOT IMPLEMENTED YET.");
    }

    public void createOrUpdateExternalDiscoveryScanEvent(ExternalScan scan) {
        logger.debug("Creating or updating external discovery scan event in DB-API.");
        logger.warn("NOT IMPLEMENTED YET.");
    }

    public OffsetDateTime getExternalDiscoveredDevicesLastUpdateDate() throws ExecutionException, InterruptedException, TimeoutException {
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.EXTERNAL_DISCOVERED_DEVICES_LAST_UPDATED_DATE);
        ContentResponse response = request.send();
        Json responseContents = Json.read(response.getContentAsString());

        return null;
    }

    public int createExternalDeviceScanEvent(ExternalScan scan) throws ExecutionException, InterruptedException, TimeoutException, DbapiUnexpectedStatusCodeException {
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.EXTERNAL_DISCOVERED_DEVICES_CREATE_EVENT);
        Json requestContents = Json.object(
                "started_at", DbapiUtilsForCSLScan.localDateToDbapi(scan.getCreatedAt()).toString()
        );
        request.content(new StringContentProvider(requestContents.toString()), "application/json");
        ContentResponse response = request.send();
        if (response.getStatus() >= 400) {
            throw new DbapiUnexpectedStatusCodeException("Could not create external device scan event.", response.getStatus());
        }
        Json responseContents = Json.read(response.getContentAsString());
        if (responseContents.isObject() && responseContents.has("id") && responseContents.get("id").isNumber()) {
            return responseContents.get("id").asInteger();
        } else if (responseContents.isNumber()) {
            return responseContents.asInteger();
        } else {
            logger.warn("Could not get the id of the created external device scan event.");
            logger.debug("Response contents: {}", responseContents.toString());
            return 0;
        }
    }

    public void sendExternalDiscoveredDevices(List<ExternalDiscoveredDevice> externalDiscoveredDevices, ExternalScan scan) throws ExecutionException, InterruptedException, TimeoutException, DbapiUnexpectedStatusCodeException {
        Json serializedDevices = Json.array();
        externalDiscoveredDevices.stream()
                .map(ExternalDiscoveredDevice::serializeForDbapi)
                .filter(Objects::nonNull)
                .forEach(serializedDevices::add);
        Json requestContents = Json.object(
                "event_id", scan.getDbapiId(),
                "discovered_device_dict_arr", serializedDevices
        );
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.EXTERNAL_DISCOVERED_DEVICES_CREATE);
        request.content(new StringContentProvider(requestContents.toString()), "application/json");
        ContentResponse response = request.send();
        if (response.getStatus() >= 400) {
            throw new DbapiUnexpectedStatusCodeException("Could not send external discovered devices to DB-API.", response.getStatus());
        }
    }

    public void clearExternalDiscoveredDevices() throws ExecutionException, InterruptedException, TimeoutException, DbapiUnexpectedStatusCodeException {
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.EXTERNAL_DISCOVERED_DEVICES_CLEAR);
        ContentResponse response = request.send();
        if (response.getStatus() != 200) {
            throw new DbapiUnexpectedStatusCodeException("Could not clear external discovered devices.", response.getStatus());
        }
    }

    public void clearExternalConnectionInfos() throws ExecutionException, InterruptedException, TimeoutException, DbapiUnexpectedStatusCodeException {
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.EXTERNAL_CONNECTION_INFO_CLEAR);
        ContentResponse response = request.send();
        if (response.getStatus() != 200) {
            throw new DbapiUnexpectedStatusCodeException("Could not clear external connection infos.", response.getStatus());
        }
    }
    // endregion External discovery

    /**
     * Inform DB-API that a scan has started.
     *
     * @param startDate The starting time of the scan.
     * @return The id attributed by DB-API to the scan object.
     */
    public int notifyScanStarted(OffsetDateTime startDate) {
        Json params = Json.object("started_at", DbapiUtilsForCSLScan.localDateToDbapi(startDate).toString());
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.SCAN_EVENT_CREATION)
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

        Request request = createDbapiPatchRequest(String.format(DbapiEndpointForCSLScan.SCAN_EVENT_UPDATE.getEndpoint(), scan.getDbapiId()));
        Json params = Json.object("ended_at", DbapiUtilsForCSLScan.localDateToDbapi(scan.getEndDate()).toString());
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
        Request request = createDbapiPatchRequest(String.format(DbapiEndpointForCSLScan.SCAN_EVENT_UPDATE.getEndpoint(), scan.getDbapiId()));
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
        Request request = this.createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.NO_NEW_CPE_ITEM);
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
        Request request = this.createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.EVENTS_CANCEL_ALL);
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
        Request request = this.createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.GET_ORGANIZATION_NAME);
        try {
            ContentResponse response = request.send();
            return response.getContentAsString();
        } catch (Exception e) {
            logger.warn("Could not get the organization name from DB-API.", e);
            return "None";
        }
    }

    public String getMqttTopicPrefix() {
        Request request = this.createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.GET_MQTT_TOPIC_PREFIX);
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
        return initRequestWithHeaders(method.toString(), endpoint);
    }

    /**
     * Create a DB-API request. Most notably, adds the API key to the request header.
     *
     * @param method   The HTTP method to use (GET, POST, ...)
     * @param endpoint The endpoint to contact in DB-API.
     * @return The crafted {@link Request}.
     */
    private Request createDbapiRequest(HttpMethod method, DbapiEndpointForCSLScan endpoint) {
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
    private Request createDbapiRequest(HttpMethod method, DbapiEndpointForCSLScan endpoint, String id) {
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
        return initRequestWithHeaders("PATCH", endpoint);
    }

    /**
     * Create a DB-API PATCH request. Most notably, adds the API key to the request header.
     * Exists because PATCH is absent from the Jetty methods enum.
     *
     * @param endpoint The endpoint to contact in DB-API.
     * @return The crafted {@link Request}.
     */
    private Request createDbapiPatchRequest(DbapiEndpointForCSLScan endpoint) {
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
    private Request createDbapiPatchRequest(DbapiEndpointForCSLScan endpoint, String id) {
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
    public void sendConnections(List<Connection> items) {
        logger.error("Sending connections to DB-API is not implemented yet.");
    }
    public int getConnectionPortNumberFromConnection(Connection connection) {
        int port = 0;
        if (connection.getProtocol() == SNMPv1){
            port = ((SNMPv1Connection) connection).getPort();
        } else if (connection.getProtocol() == SNMPv2c){
            port = ((SNMPv2cConnection) connection).getPort();
        }  else if (connection.getProtocol() == SNMPv3){
            port = ((SNMPv3Connection) connection).getPort();
        } else if (connection.getProtocol() == SSH){
            port = ((SshConnection) connection).getPort();
        } else if (connection.getProtocol() == RemotePowershell){
            port = ((RemotePowershellConnection) connection).getPort();
        }
        return port;
    }
    public Json getConnectionOtherData(Connection connection) {
        Json otherData = Json.object();
        if (connection.getProtocol() == SNMPv1){
            String community = ((SNMPv1Connection) connection).getCommunity();
            otherData.set("snmp_community", community);
            return otherData;
        } else if (connection.getProtocol() == SNMPv2c) {
            String community = ((SNMPv2cConnection) connection).getCommunity();
            otherData.set("snmp_community", community);
            return otherData;
        } else if (connection.getProtocol() == SNMPv3) {
            String snmpPrivacyAlgorithm = String.valueOf(((SNMPv3Connection) connection).getPrivacyAlgorithm());
            String snmpAuthAlgorithm = String.valueOf(((SNMPv3Connection) connection).getAuthenticationAlgorithm());
            otherData.set("snmp_privacy_algorithm", snmpPrivacyAlgorithm);
            otherData.set("snmp_authentication_algorithm", snmpAuthAlgorithm);
        } else if (connection.getProtocol() == SSH) {
            // TODO: remove them --> to be saved in vault
            String sshKey = ((SshConnection) connection).getPrivateKey();
            String passPhrase = ((SshConnection) connection).getPassphrase();
            otherData.set("ssh_key", sshKey);
            otherData.set("passphrase", passPhrase);
        }
        else {
            return otherData;
        }
        return otherData;
    }
    public void createConnection(Connection connection) throws ExecutionException, InterruptedException, TimeoutException {
        logger.error("Creating connections in DB-API is not implemented yet.");
        String name = connection.getName();
        int portNumber = getConnectionPortNumberFromConnection(connection);
        Json requestContents = Json.object(
                "name", name,
                "discovery_protocol_name", connection.getProtocol().dbapiName(),
                "port_number", portNumber,
                "mongo_entity_id", connection.getUuid(),
                "other_data", getConnectionOtherData(connection),
                "connected_devices", connection.getDevicesIds()
        );
        if (connection.getProtocol() == SNMPv3) {
            requestContents.set("username", ((SNMPv3Connection) connection).getUsername());
        } else if (connection.getProtocol() == RemotePowershell) {
            requestContents.set("username", ((RemotePowershellConnection) connection).getUsername());
        }
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.CONNECTIONS)
                .content(new StringContentProvider(requestContents.toString()), "application/json");
        try {
            ContentResponse response = request.send();
            if (response.getStatus() != 201) {
                logger.error("Could not create connection in DB-API. Got status code " + response.getStatus());
            } else if (response.getStatus() == 201) {
                logger.info("Connection created in DB-API.");
            }
        } catch (Exception e) {
            logger.error("Could not create connection in DB-API.", e);
        }

    }

    public void deleteConnection(String connectionUuid) throws ExecutionException, InterruptedException, TimeoutException, DbapiUnexpectedStatusCodeException {
        Request request = createDbapiRequest(HttpMethod.DELETE, String.format(DbapiEndpointForCSLScan.DELETE_CONNECTION_BY_MONGO_ID.getEndpoint())).param("mongo_entity_id", connectionUuid);
        ContentResponse response = request.send();
        if (response.getStatus() >= 400) {
            throw new DbapiUnexpectedStatusCodeException("Could not delete connection.", response.getStatus());
        }
    }
    public int getDbApiConnectionId(String connectionMongoEntityId) {
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.CONNECTIONS_DETAILS_BY_MONGO_ID);
        request.param("mongo_entity_id", connectionMongoEntityId);
        try {
            ContentResponse response = request.send();
            Json responseContents = Json.read(response.getContentAsString());
            return JsonUtil.getIntFromJson(responseContents, "id", 0);
        } catch (Exception e) {
            logger.error("Could not get connection id from DB-API.", e);
            return 0;
        }
    }
    public void updateConnection(Connection connection) throws ExecutionException, InterruptedException, TimeoutException {
        String connectionUuid = connection.getUuid();
        String connectionDbApiId = String.valueOf(getDbApiConnectionId(connectionUuid));
        Request request = createDbapiRequest(HttpMethod.PUT, String.format(DbapiEndpointForCSLScan.CONNECTIONS.getEndpoint(), connectionDbApiId));
        Json requestContents = Json.object(
                "name", connection.getName(),
                "discovery_protocol_name", connection.getProtocol().dbapiName(),
                "port_number", getConnectionPortNumberFromConnection(connection),
                "mongo_entity_id", connectionUuid,
                "other_data", getConnectionOtherData(connection),
                "connected_devices", connection.getDevicesIds()
        );
        if (connection.getProtocol() == SNMPv3) {
            requestContents.set("username", ((SNMPv3Connection) connection).getUsername());
        } else if (connection.getProtocol() == RemotePowershell) {
            requestContents.set("username", ((RemotePowershellConnection) connection).getUsername());
        }
        request.content(new StringContentProvider(requestContents.toString()), "application/json");
        ContentResponse response = request.send();
        if (response.getStatus() != 200) {
            logger.error("Could not update connection in DB-API. Got status code " + response.getStatus());
        } else if (response.getStatus() == 200) {
            logger.info("Connection updated in DB-API.");
        }
        logger.error("Updating connections in DB-API is not implemented yet.");
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
        List<String> connectionUuidsInDevices = new ArrayList<>();
        List<String> deviceUuidsInConnections = new ArrayList<>();

        for (Device device : devices) {
            List<String> connectionsIds = device.getConnectionsIds();
            connectionUuidsInDevices.addAll(connectionsIds);
        }
        for (Connection connection : connections) {
            deviceUuidsInConnections.addAll(connection.getDevicesIds());
        }
        //endregion List the uuids we have in both list

        //region Check for the ones missing on one side or the other
        List<String> connectionsToGet = new ArrayList<>();
        List<String> devicesToGet = new ArrayList<>();

        for (String connectionId : connectionUuidsInDevices) {
            if (DbapiUtilsForCSLScan.getConnectionById(connections, connectionId) == null) {
                connectionsToGet.add(connectionId);
            }
        }
        for (String deviceId : deviceUuidsInConnections) {
            if (DbapiUtilsForCSLScan.getDeviceById(devices, deviceId) == null) {
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
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.JAVACOMM_SEND_COMMANDS)
                .content(new StringContentProvider(requestContents.toString()), "application/json");
        ContentResponse response = request.send();
        if (response.getStatus() != 200) {
            throw new Exception("Error sending commands to dbapi: got unexpected status " + response.getStatus());
        }
    }

    public List<HttpTemplateImportNotification> getAvailableImportTasks() {
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.FILE_ACTION_STATUS_AVAILABLE);
        try {
            ContentResponse response = request.send();
            if (response.getStatus() >= 400) {
                logger.warn("Unable to fetch available import tasks from DB-API: Unexpected status code: {}", response.getStatus());
                return new ArrayList<>();
            }
            return Json.read(response.getContentAsString()).asJsonList().stream()
                    .map(HttpTemplateImportNotification::fromDbapiJson)
                    .filter(Objects::nonNull)
                    .filter(query -> query.getType() == HttpTemplateImportNotification.Type.FILE_RECEIVED)
                    .collect(Collectors.toList());
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            logger.warn("Error fetching available import tasks");
            logger.debug("Error fetching available import tasks", e);
            return new ArrayList<>();
        }
    }

    public Path downloadHttpTemplatesBsonFile(HttpTemplateImportNotification query) throws ExecutionException, InterruptedException, TimeoutException {
        Json contents = Json.object("file_action_status_id", query.getId());
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.DOWNLOAD_HTTP_TEMPLATES_BSON_FILE.getEndpoint());
        request.content(new StringContentProvider(contents.toString()), "application/json");
        InputStreamResponseListener listener = new InputStreamResponseListener();
        request.send(listener);
        Response response = listener.get(30, TimeUnit.SECONDS);
        if (response.getStatus() == 200) {
            try {
                return fileStorageService.saveFile(listener.getInputStream(), query.getFileName() + ".bson");
            } catch (IOException e) {
                logger.error("Error saving file");
                logger.debug("Error saving file", e);
                return null;
            }
        } else {
            return null;
        }
    }

    public void uploadHttpTemplatesBsonFile(ExportQuery exportQuery) {
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.UPLOAD_HTTP_TEMPLATES_BSON_FILE);
        MultiPartContentProvider multiPart = new MultiPartContentProvider();
        try {
            multiPart.addFilePart("file", exportQuery.getFilename(), new PathContentProvider(fileStorageService.getFilePath(exportQuery.getFilename())), null);
        } catch (IOException e) {
            logger.error("Error adding file to request", e);
            return;
        }
        multiPart.close();
        request.content(multiPart);
        try {
            ContentResponse response = request.send();
            if (response.getStatus() >= 400) {
                logger.warn("Unexpected status code: {}", response.getStatus());
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            logger.error("Error sending file to DB-API", e);
        }
    }

    public void notifyImportStarted(int id, ImportQuery importQuery) {
        Json contents = Json.object("status", FileActionStatus.FILE_PROCESSING.getValue());
        Request request = createDbapiPatchRequest(String.format(DbapiEndpointForCSLScan.FILE_ACTION_STATUS_DETAILS.getEndpoint(), id));
        request.content(new StringContentProvider(contents.toString()), "application/json");
        try {
            ContentResponse response = request.send();
            if (response.getStatus() != 200) {
                logger.warn("Unexpected status code: {}", response.getStatus());
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            logger.error("Error sending import status to DB-API", e);
        }
    }

    public void notifyImportFinished(int id, ImportQuery importQuery) {
        Json contents;
        switch (importQuery.getStatus()) {
            case SUCCESS:
                contents = Json.object("status", FileActionStatus.SUCCEEDED.getValue());
                break;
            case ERROR:
                // Add an error status
                contents = Json.object("status", FileActionStatus.FAILED.getValue());
                break;
            default:
                logger.warn("Unknown import status: {}", importQuery.getStatus());
                return;
        }
        Request request = createDbapiPatchRequest(String.format(DbapiEndpointForCSLScan.FILE_ACTION_STATUS_DETAILS.getEndpoint(), id));
        request.content(new StringContentProvider(contents.toString()), "application/json");
        try {
            ContentResponse response = request.send();
            if (response.getStatus() != 200) {
                logger.warn("Unexpected status code: {}", response.getStatus());
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            logger.error("Error sending import status to DB-API", e);
        }
    }

    /**
     * Request a new BSON export ID from DB-API.
     *
     * @return The new BSON export ID.
     * @throws Exception If the request failed.
     */
    public int requestBsonExportID(ExportQuery exportQuery) throws Exception {
        Json contents = Json.object("file_name", exportQuery.getFilename());
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.FILE_ACTION_STATUS_CREATE_FOR_HTTP_TEMPLATE_EXPORT);
        request.content(new StringContentProvider(contents.toString()), "application/json");
        ContentResponse response = request.send();
        if (response.getStatus() != 200) {
            throw new Exception("Unexpected status code: " + response.getStatus());
        }
        Json responseContents = Json.read(response.getContentAsString());
        if (responseContents.isObject() && responseContents.has("file_action_status_id") && responseContents.get("file_action_status_id").isNumber()) {
            int id = responseContents.get("file_action_status_id").asInteger();
            logger.debug("Got BSON export ID: {}", id);
            return id;
        }
        throw new Exception("Unexpected response: " + responseContents.toString());
    }

    /**
     * Update the status of an export query in DB-API.
     *
     * @param exportQuery The export query to update.
     * @throws Exception  If the request failed.
     */
    public void notifyExportFinished(int id, ExportQuery exportQuery) throws Exception {
        Json contents;
        switch (exportQuery.getStatus()) {
            case SUCCESS:
                contents = Json.object("status", FileActionStatus.SUCCEEDED.getValue());
                break;
            case ERROR:
                // Add an error status
                contents = Json.object("status", FileActionStatus.FAILED.getValue());
                break;
            default:
                logger.warn("Unknown export status: {}", exportQuery.getStatus());
                return;
        }
        Request request = createDbapiPatchRequest(String.format(DbapiEndpointForCSLScan.FILE_ACTION_STATUS_DETAILS.getEndpoint(), id));
        request.content(new StringContentProvider(contents.toString()), "application/json");
        try {
            ContentResponse response = request.send();
            if (response.getStatus() != 200) {
                logger.warn("Unexpected status code: {}", response.getStatus());
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            logger.error("Error sending import status to DB-API", e);
        }
    }
}
