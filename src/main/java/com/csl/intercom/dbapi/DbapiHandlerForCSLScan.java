package com.csl.intercom.dbapi;

import com.csl.autocrypt.enums.AutocryptConstants.Common;
import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.logger.CSLApplicativeLogger;
import com.csl.web.ApiHandler;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.JsonApiResponse;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.client.util.MultiPartContentProvider;
import org.eclipse.jetty.client.util.PathContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
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

import static com.ucsl.json.Json.object;

import static com.csl.intercom.dbapi.enums.StaticConnectionProtocol.*;

/**
 * Manage HTTP communications with DB-API.
 * Provides an interface for retrieving the devices, connections and so on,
 * and to send information to it (CPE Items, a Scan's status, ...).
 */
public class DbapiHandlerForCSLScan extends DbapiHandler {
    private final int maxPageSize = 1000;
    private static final CSLApplicativeLogger logger = CSLApplicativeLogger.getLogger(DbapiHandler.class);
    private final FileStorageService fileStorageService = new FileStorageService();

    public DbapiHandlerForCSLScan() {
        this("CSLScan", CSLContext.instance.getConfig());
    }

    public DbapiHandlerForCSLScan(String moduleName, Config config) {
        super(moduleName, config);
    }

    public DbapiHandlerForCSLScan(Config config) {
        this("CSLScan", config);
    }

    /**
     * Remove a list of CPE Items from DB-API.
     *
     * @param deletedItems The list of CPE Items to remove in DB-API.
     */
    private void deleteCpeItemsFromDbapi(List<CpeItem> deletedItems) {
        Json contents = object("mongo_entity_ids", Json.array(deletedItems.stream().map(CpeItem::getMongoEntityId).toArray()));
        try {
            createAndSendRequest(HttpMethod.POST.toString(),  DbapiEndpointForCSLScan.DELETE_CPE_ITEMS.getEndpoint(), null, contents);
        } catch (Exception e) {
            logger.error("Could not delete the CPE Items from DB-API.", e);
        }
    }

    private void deleteMicrosoftKbsFromDbapi(List<MicrosoftKB> deletedItems) {
        Json contents = object("mongo_entity_ids", Json.array(deletedItems.stream().map(MicrosoftKB::getMongoEntityId).toArray()));
        try {
            createAndSendRequest(HttpMethod.POST.toString(), DbapiEndpointForCSLScan.DELETE_MICROSOFT_KBS.getEndpoint(), null, contents);
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
            cpeItemsArray.add(object(
                    "device", deviceCpeItems.getKey(),
                    "discovered_cpe_list", deviceCpeItemsArray
            ));
        }

        Json requestContents = object(
                "progress", scan.getProgress(),
                "event_id", scan.getDbapiId(),
                "discovered_cpe_dict_arr", cpeItemsArray,
                "has_more", hasMore
        );
//        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.CREATE_CPE_ITEMS)
//                .content(new StringContentProvider(requestContents.toString()), "application/json");
//        ContentResponse response = request.send();
        ContentResponse response = createAndSendRequest(HttpMethod.POST.toString(), DbapiEndpointForCSLScan.CREATE_CPE_ITEMS.getEndpoint(), null, requestContents );
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
            KBsArray.add(object(
                    "device", deviceKBs.getKey(),
                    "discovered_kb_list", deviceKBsArray
            ));
        }

        Json requestContents = object(
                "progress", scan.getProgress(),
                "event_id", scan.getDbapiId(),
                "discovered_kb_dict_arr", KBsArray
        );
//        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.CREATE_MICROSOFT_KBS)
//                .content(new StringContentProvider(requestContents.toString()), "application/json");
//        ContentResponse response = request.send();
        ContentResponse response = createAndSendRequest(HttpMethod.POST.toString(), DbapiEndpointForCSLScan.CREATE_MICROSOFT_KBS.getEndpoint(), null, requestContents );
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
        return getLastUpdateDateAt(DbapiEndpointForCSLScan.CPE_ITEMS_LAST_DATE);
    }

    /**
     * Fetch the last updated date at given endpoint in DB-API.
     *
     * @return The last update in DB-API.
     * @throws Exception If it was not possible to fetch from DB-API or the format was not recognised.
     */
    private OffsetDateTime getLastUpdateDateAt(DbapiEndpointForCSLScan endpoint) throws ExecutionException, InterruptedException, TimeoutException {
        ContentResponse response = createAndSendRequest(HttpMethod.GET.toString(), endpoint.getEndpoint(), null, null );
        if (response.getContentAsString().isEmpty()) { return DbapiUtilsForCSLScan.dbapiDateToLocal(Common.MIN_DATE);}
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
        return getLastUpdateDateAt(DbapiEndpointForCSLScan.MICROSOFT_KB_LAST_DATE);
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
        Json params = object();
        if (dateUtc != null) {
            params.set("updated_at__gt", dateUtc.toString());
        }
        ContentResponse response = createAndSendRequest(HttpMethod.GET.toString(), DbapiEndpointForCSLScan.DEVICES.getEndpoint(), params, null);
        Json responseJson = Json.read(response.getContentAsString());

        List<Device> devices = responseJson.asJsonList().stream()
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

        Json params = object();
        if (dateUtc != null) {
            params.set("updated_at__gt", dateUtc.toString());
        }
        ContentResponse response = createAndSendRequest(HttpMethod.GET.toString(), DbapiEndpointForCSLScan.CONNECTIONS.getEndpoint(), params, null);
        Json responseJson = Json.read(response.getContentAsString());
        return responseJson.asJsonList().stream()
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
            Json params = object("offset", String.valueOf(offset), "limit", String.valueOf(this.maxPageSize));
            if (dateUtc != null) {
                params.set("deleted_date__gt", dateUtc.toString());
            }
            ContentResponse response = createAndSendRequest(HttpMethod.GET.toString(), DbapiEndpointForCSLScan.DELETED_DEVICES.getEndpoint(), params, null);
            Json responseJson = Json.read(response.getContentAsString());
            for (Json deletedDevice : responseJson.get("results").asJsonList()) {
                String uuid = deletedDevice.get("object_id").asString();
                OffsetDateTime deletedDate = DbapiUtilsForCSLScan.dbapiDateToLocal(deletedDevice.get("deleted_at").asString());
                deletedDevices.add(new Pair<>(uuid, deletedDate));
            }
            hasMore = !responseJson.get("next").isNull();
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

        Json params = object();
        if (offset > 0) {
            params.set("offset", String.valueOf(offset));
        }
        if (limit > 0) {
            params.set("limit", String.valueOf(limit));
        }
        if (dateUtc != null) {
            params.set("deleted_date__gt", dateUtc.toString());
        }
        ContentResponse response = createAndSendRequest(HttpMethod.GET.toString(), DbapiEndpointForCSLScan.GET_DELETED_CPE_ITEMS.getEndpoint(), params, null );

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

        Json params = object();
        if (offset > 0) {
            params.set("offset", String.valueOf(offset));
        }
        if (limit > 0) {
            params.set("limit", String.valueOf(limit));
        }
        if (dateUtc != null) {
            params.set("deleted_date__gt", dateUtc.toString());
        }
        ContentResponse response = createAndSendRequest(HttpMethod.GET.toString(), DbapiEndpointForCSLScan.GET_DELETED_MICROSOFT_KBS.getEndpoint(),params, null );

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

        ContentResponse response = createAndSendRequest(HttpMethod.GET.toString(), DbapiEndpointForCSLScan.DEVICES.getEndpoint(), object("uuid", String.join(",", uuids)), null );
        Json responseJson = Json.read(response.getContentAsString());
        responseJson.asJsonList().stream()
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
            ContentResponse response = createAndSendRequest(HttpMethod.GET.toString(), DbapiEndpointForCSLScan.CONNECTIONS.getEndpoint(), object("id", String.valueOf(id)), null);
            Json responseJson = Json.read(response.getContentAsString());
            Connection connection;
            if (responseJson.isArray()) {
                connection = Connection.fromDbapiJson(responseJson.at(0), protocols);
            } else {
                connection = Connection.fromDbapiJson(responseJson, protocols);
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

        ContentResponse response = createAndSendRequest(HttpMethod.GET.toString(), DbapiEndpointForCSLScan.DISCOVERY_PROTOCOLS.getEndpoint(), null, null );
        return Json.read(response.getContentAsString()).asJsonList().stream()
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
        Json requestContents = object(
                ConnectionProtocolField.NAME.dbapiName(), entityHttpConnection.getName(),
                ConnectionProtocolField.IS_DYNAMIC.dbapiName(), true,
                ConnectionProtocolField.DEFAULT_PORT.dbapiName(), 443,
                ConnectionProtocolField.CONNECTION_TEMPLATE_ID.dbapiName(), entityHttpConnection.getUuid(),
                ConnectionProtocolField.CONNECTION_TEMPLATE_DETAILS.dbapiName(), entityHttpConnection.serializeForDbapi()
        );

        ContentResponse response = createAndSendRequest(HttpMethod.POST.toString(), DbapiEndpointForCSLScan.DISCOVERY_PROTOCOLS.getEndpoint(), null, requestContents );
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
            Json requestContents = object(
                    ConnectionProtocolField.NAME.dbapiName(), entityHttpConnection.getName(),
                    ConnectionProtocolField.IS_DYNAMIC.dbapiName(), true,
                    ConnectionProtocolField.DEFAULT_PORT.dbapiName(), 443,
                    ConnectionProtocolField.CONNECTION_TEMPLATE_ID.dbapiName(), entityHttpConnection.getUuid(),
                    ConnectionProtocolField.CONNECTION_TEMPLATE_DETAILS.dbapiName(), entityHttpConnection.serializeForDbapi()
            );

            ContentResponse response = createAndSendRequest(HttpMethod.PUT.toString(), String.format(DbapiEndpointForCSLScan.DISCOVERY_PROTOCOLS_DETAILS.getEndpoint(), protocolId), null, requestContents );
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

            ContentResponse response = createAndSendRequest(HttpMethod.DELETE.toString(), String.format(DbapiEndpointForCSLScan.DISCOVERY_PROTOCOLS_DETAILS.getEndpoint(), protocolId), null, null );
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
        try {
            ContentResponse response = createAndSendRequest(HttpMethod.GET.toString(), DbapiEndpointForCSLScan.DISCOVERY_PROTOCOLS_DETAILS_BY_TEMPLATE_ID.getEndpoint(), Json.object("connection_template_id", id), null);
            Json responseJson = Json.read(response.getContentAsString());
            if (responseJson.isArray()) {
                return ConnectionProtocol.fromJson(responseJson.at(0));
            } else {
                return ConnectionProtocol.fromJson(responseJson);
            }
        } catch (Exception e) {
            logger.error("Could not get discovery protocol by template id.", e);
            return null;
        }
    }

    // region External discovery
    public void createOrUpdateExternalConnectionInfoTemplates(List<ExternalConnectionInfoTemplate> externalConnectionInfoTemplates) throws DbapiUnexpectedStatusCodeException, ExecutionException, InterruptedException, TimeoutException {
        Json requestContents = Json.array(externalConnectionInfoTemplates.stream().map(ExternalConnectionInfoTemplate::serializeForDbapi).toArray());

        ContentResponse response = createAndSendRequest(HttpMethod.POST.toString(), DbapiEndpointForCSLScan.EXTERNAL_CONNECTION_INFO_TEMPLATES_CREATE_OR_UPDATE.getEndpoint(), null, requestContents );
        if (response.getStatus() >= 400) {
            throw new DbapiUnexpectedStatusCodeException("Could not create or update external connection info templates.", response.getStatus());
        }
    }

    public void createOrUpdateExternalConnectionInfos(List<ExternalConnectionInfo> externalConnectionInfos) throws DbapiUnexpectedStatusCodeException, ExecutionException, InterruptedException, TimeoutException {
        Json requestContents = Json.array(externalConnectionInfos.stream().map(ExternalConnectionInfo::serializeForDbapi).toArray());

        ContentResponse response = createAndSendRequest(HttpMethod.POST.toString(), DbapiEndpointForCSLScan.EXTERNAL_CONNECTION_INFO_CREATE_OR_UPDATE.getEndpoint(), null, requestContents );
        if (response.getStatus() >= 400) {
            throw new DbapiUnexpectedStatusCodeException("Could not create or update external connection infos.", response.getStatus());
        }
    }

    public void deleteExternalConnectionInfo(String id) throws DbapiUnexpectedStatusCodeException, ExecutionException, InterruptedException, TimeoutException {
        ContentResponse response = createAndSendRequest(HttpMethod.DELETE.toString(), String.format(DbapiEndpointForCSLScan.EXTERNAL_CONNECTION_INFO_DETAILS.getEndpoint(), id), null, null );
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
        return getLastUpdateDateAt(DbapiEndpointForCSLScan.EXTERNAL_DISCOVERED_DEVICES_LAST_UPDATED_DATE);
    }

    public int createExternalDeviceScanEvent(ExternalScan scan) throws ExecutionException, InterruptedException, TimeoutException, DbapiUnexpectedStatusCodeException {
        Json requestContents = object(
                "started_at", DbapiUtilsForCSLScan.localDateToDbapi(scan.getCreatedAt()).toString()
        );

        ContentResponse response = createAndSendRequest(HttpMethod.POST.toString(), DbapiEndpointForCSLScan.EXTERNAL_DISCOVERED_DEVICES_CREATE_EVENT.getEndpoint(), null, requestContents );

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
        Json requestContents = object(
                "event_id", scan.getDbapiId(),
                "discovered_device_dict_arr", serializedDevices
        );

        ContentResponse response = createAndSendRequest(HttpMethod.POST.toString(), DbapiEndpointForCSLScan.EXTERNAL_DISCOVERED_DEVICES_CREATE.getEndpoint(), null, requestContents );

        if (response.getStatus() >= 400) {
            throw new DbapiUnexpectedStatusCodeException("Could not send external discovered devices to DB-API.", response.getStatus());
        }
    }

    public void clearExternalDiscoveredDevices() throws ExecutionException, InterruptedException, TimeoutException, DbapiUnexpectedStatusCodeException {
        ContentResponse response = createAndSendRequest(HttpMethod.GET.toString(), DbapiEndpointForCSLScan.EXTERNAL_DISCOVERED_DEVICES_CLEAR.getEndpoint(), null, null );

        if (response.getStatus() != 200) {
            throw new DbapiUnexpectedStatusCodeException("Could not clear external discovered devices.", response.getStatus());
        }
    }

    public void clearExternalConnectionInfos() throws ExecutionException, InterruptedException, TimeoutException, DbapiUnexpectedStatusCodeException {
        ContentResponse response = createAndSendRequest(HttpMethod.GET.toString(), DbapiEndpointForCSLScan.EXTERNAL_CONNECTION_INFO_CLEAR.getEndpoint(), null, null );

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
        Json params = object("started_at", DbapiUtilsForCSLScan.localDateToDbapi(startDate).toString());
        try {
            ContentResponse response = createAndSendRequest(HttpMethod.POST.toString(), DbapiEndpointForCSLScan.SCAN_EVENT_CREATION.getEndpoint(), null, params );

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

        Json params = object("ended_at", DbapiUtilsForCSLScan.localDateToDbapi(scan.getEndDate()).toString());
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

        ContentResponse response = createAndSendRequest(ApiHandler.PATCH, String.format(DbapiEndpointForCSLScan.SCAN_EVENT_UPDATE.getEndpoint(), scan.getDbapiId()), null, params);
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
        Json params = object(
                "is_synchronization_ended", true
        );

        ContentResponse response = createAndSendRequest(ApiHandler.PATCH, String.format(DbapiEndpointForCSLScan.SCAN_EVENT_UPDATE.getEndpoint(), scan.getDbapiId()), null, params);

        if (response.getStatus() != 200) {
            throw new Exception("Unexpected status code: " + response.getStatus());
        }
    }

    /**
     * Send a request to DB-API to inform it the current scan provided no new CPE Item.
     */
    public void notifyNoNewCpe() {
        try {
            createAndSendRequest(HttpMethod.GET.toString(), DbapiEndpointForCSLScan.NO_NEW_CPE_ITEM.getEndpoint(), null, null);
        } catch (Exception e) {
            logger.error("Could not send the no new CPE Item notification to DB-API.", e);
        }
    }

    /**
     * Cancel all scan events in DB-API.
     */
    public void cancelAllScans() {
        try {
            createAndSendRequest(HttpMethod.GET.toString(), DbapiEndpointForCSLScan.EVENTS_CANCEL_ALL.getEndpoint(), null, null);
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
        try {
//            ContentResponse response = request.send();
            ContentResponse response = createAndSendRequest(HttpMethod.GET.toString(), DbapiEndpointForCSLScan.GET_ORGANIZATION_NAME.getEndpoint(), null, null );

            return response.getContentAsString();
        } catch (Exception e) {
            logger.warn("Could not get the organization name from DB-API.", e);
            return "None";
        }
    }

    public String getMqttTopicPrefix() {
        try {
            ContentResponse response = createAndSendRequest(HttpMethod.GET.toString(), DbapiEndpointForCSLScan.GET_MQTT_TOPIC_PREFIX.getEndpoint(), null, null );

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

    private Request createDbApiRequestWithCustomContentType(String method, String endpoint, String contentType) {
        Request request =  initRequest(method, createUriFrom(endpoint), httpClient);

        addHeadersToRequest(headers, request);
        request.header(HttpHeader.CONTENT_TYPE, contentType);

        return request;
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
        return initRequestWithHeaders(ApiHandler.PATCH, endpoint);
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
        } else if (connection.getProtocol() == HTTP){
            port = Integer.parseInt(((HttpConnection) connection).getPort());
        }
        return port;
    }
    public Json getConnectionOtherData(Connection connection, Json connectionJson) throws JsonProcessingException {
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
        }
        else if (connection.getProtocol() == SSH) {
            // TODO: remove them --> to be saved in vault
            //            String sshKey = ((SshConnection) connection).getPrivateKey();
            //            String passPhrase = ((SshConnection) connection).getPassphrase();
            //            otherData.set("ssh_key", sshKey);
            //            otherData.set("passphrase", passPhrase);
        }
        else if (connection.getProtocol() == HTTP) {
//            otherData.set("inputs", ((HttpConnection) connection).getInputs());
            ObjectMapper objectMapper = new ObjectMapper();
            Json stageConfigJson = connectionJson.get("other_data").get("stagesConfig");

            otherData.set("stagesConfig", stageConfigJson);
            otherData.set("headers", Json.object());
            otherData.set("queryParams", Json.object());
        }
        else {
            return otherData;
        }
        return otherData;
    }
    public void createConnection(Connection connection, String discoveryProtocolName, Json connectionJson) throws ExecutionException, InterruptedException, TimeoutException, JsonProcessingException {
        String name = connection.getName();
        int portNumber = getConnectionPortNumberFromConnection(connection);
        Json requestContents = Json.object(
                "name", name,
                "discovery_protocol_name", connection.getProtocol().dbapiName(),
                "port_number", portNumber,
                "mongo_entity_id", connection.getUuid(),
                "other_data", getConnectionOtherData(connection, connectionJson),
                "connected_devices", connection.getDevicesIds()
        );
        if(connection.getProtocol() == HTTP) {
            requestContents.set("discovery_protocol_name", discoveryProtocolName);
        }
        if (connection.getProtocol() == SNMPv3) {
            requestContents.set("username", ((SNMPv3Connection) connection).getUsername());
        } else if (connection.getProtocol() == RemotePowershell) {
            requestContents.set("username", ((RemotePowershellConnection) connection).getUsername());
        } else if(connection.getProtocol() == SSH) {
            requestContents.set("username", ((SshConnection) connection).getUsername());
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
    public void clearAllConnections() {
        Request request = createDbapiRequest(HttpMethod.DELETE, DbapiEndpointForCSLScan.CLEAR_ALL_CONNECTIONS);
        try {
            ContentResponse response = request.send();
            if (response.getStatus() != 200) {
                logger.error("Could not clear all connections in DB-API. Got status code " + response.getStatus());
            } else if (response.getStatus() == 200) {
                logger.info("All connections cleared in DB-API.");
            }
        } catch (Exception e) {
            logger.error("Could not clear all connections in DB-API.", e);
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
    public int getDbApiConnectionDraftId(String connectionDraftEntityId) {
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.CONNECTIONS_DRAFT_DETAILS_BY_MONGO_ID);
        request.param("mongo_entity_id", connectionDraftEntityId);
        try {
            ContentResponse response = request.send();
            Json responseContents = Json.read(response.getContentAsString());
            return JsonUtil.getIntFromJson(responseContents, "id", 0);
        } catch (Exception e) {
            logger.error("Could not get connection draft id from DB-API.", e);
            return 0;
        }
    }
    public void updateConnection(Connection connection, Json connectionJson) throws ExecutionException, InterruptedException, TimeoutException, JsonProcessingException {
        String connectionUuid = connection.getUuid();
        String connectionDbApiId = String.valueOf(getDbApiConnectionId(connectionUuid));
        int connectionId = Integer.parseInt(connectionDbApiId);
        String endpoint = String.format(DbapiEndpointForCSLScan.CONNECTIONS.getEndpoint(), connectionId) + '/' + connectionId;
        Request request = createDbapiRequest(HttpMethod.PUT, endpoint);
        Json requestContents = Json.object(
                "name", connection.getName(),
                "discovery_protocol_name", connection.getProtocol().dbapiName(),
                "port_number", getConnectionPortNumberFromConnection(connection),
                "mongo_entity_id", connectionUuid,
                "other_data", getConnectionOtherData(connection, connectionJson),
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
                object("failed_devices", Json.array(failedDevices.toArray()))
        );
    }

    public void createListOfConnectionDrafts(List<EntityConnectionInfoDraft> entityConnectionInfoDrafts, int fileActionStatusIdInDbApi) {
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.CREATE_CONNECTIONS_DRAFT);
        Json requestContents = Json.array(entityConnectionInfoDrafts.stream().map(EntityConnectionInfoDraft::serializeForDbapi).toArray());
        request.content(new StringContentProvider(requestContents.toString()), "application/json");
        try {
            ContentResponse response = request.send();
            if (response.getStatus() != 201) {
                logger.error("Could not create connection drafts in DB-API. Got status code {}", response.getStatus());
            } else if (response.getStatus() == 201) {
                logger.info("Connection drafts created in DB-API.");
            }
        } catch (Exception e) {
            logger.error("Could not create connection drafts in DB-API.", e);
        }
    }

    public int createFileActionStatusForImportConnectionDraftAndReturnCreatedId(){
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.CREATE_FILE_ACTION_STATUS_FOR_IMPORT_CONNECTION_DRAFT);
        try {
            ContentResponse response = request.send();
            if (response.getStatus() != 201) {
                logger.error("Could not create file action status for import connection draft in DB-API. Got status code {}", response.getStatus());
            } else if (response.getStatus() == 201) {
                logger.info("File action status for import connection draft created in DB-API.");
                Json responseContents = Json.read(response.getContentAsString());
                return JsonUtil.getIntFromJson(responseContents, "id", 0);
            }
        } catch (Exception e) {
            logger.error("Could not create file action status for import connection draft in DB-API.", e);
        }
        return 0;
    }
    public void updateFileActionStatusForImportSucceededConnectionDraft(int fileActionStatusId) {
        Request request = createDbapiRequest(HttpMethod.PUT, DbapiEndpointForCSLScan.UPDATE_FILE_ACTION_STATUS_FOR_IMPORT_CONNECTION_DRAFT);
        request.param("file_action_status_id", String.valueOf(fileActionStatusId));
        request.param("status", String.valueOf(2));
        try {
            ContentResponse response = request.send();
            if (response.getStatus() != 200) {
                logger.error("Could not update file action status for import connection draft in DB-API. Got status code {}", response.getStatus());
            } else if (response.getStatus() == 200) {
                logger.info("File action status for import connection draft updated in DB-API.");
            }
        } catch (Exception e) {
            logger.error("Could not update file action status for import connection draft in DB-API.", e);
        }
    }

    public Json getConnectionDraftOtherData(EntityConnectionInfoDraft connectionInfoDraft) {
        Json otherData = Json.object();
        if (connectionInfoDraft.getProtocol().equals("SNMPv1")) {
            String community = connectionInfoDraft.getSnmpCommunity();
            otherData.set("snmp_community", community);
            return otherData;
        } else if (connectionInfoDraft.getProtocol().equals("SNMPv2c")) {
            String community = connectionInfoDraft.getSnmpCommunity();
            otherData.set("snmp_community", community);
            return otherData;
        } else if (connectionInfoDraft.getProtocol().equals("SNMPv3")) {
            String snmpPrivacyAlgorithm = String.valueOf(connectionInfoDraft.getSnmpPrivacyAlgorithm());
            String snmpAuthAlgorithm = String.valueOf(connectionInfoDraft.getSnmpAuthenticationAlgorithm());
            otherData.set("snmp_privacy_algorithm", snmpPrivacyAlgorithm);
            otherData.set("snmp_authentication_algorithm", snmpAuthAlgorithm);
        } else if (connectionInfoDraft.getProtocol().equals("SSH")) {
            String sshKey = connectionInfoDraft.getSshKey();
            otherData.set("ssh_key", sshKey);
        }
        return otherData;
    }
    public void updateConnectionDraft( EntityConnectionInfoDraft connectionInfoDraft ,String mongoEntityId) {
        String connectionInfoDraftDbApiId = String.valueOf(getDbApiConnectionDraftId(mongoEntityId));
        int connectionInfoDraftId = Integer.parseInt(connectionInfoDraftDbApiId);
        String endpoint = String.format(DbapiEndpointForCSLScan.CONNECTIONS_DRAFT.getEndpoint(), connectionInfoDraftId) + '/' + connectionInfoDraftId;
        Request request = createDbapiRequest(HttpMethod.PUT, endpoint);
        Json requestContents = Json.object(
               "name_draft", connectionInfoDraft.getName(),
                "discovery_protocol_draft", connectionInfoDraft.getProtocol(),
                "port_number_draft", connectionInfoDraft.getPort(),
                "username_draft", connectionInfoDraft.getUsername(),
                "mongo_entity_id", mongoEntityId,
                "other_data_draft", getConnectionDraftOtherData(connectionInfoDraft)
        );
        request.content(new StringContentProvider(requestContents.toString()), "application/json");
        try {
            ContentResponse response = request.send();
            if (response.getStatus() != 200) {
                logger.error("Could not update connection draft in DB-API. Got status code {}", response.getStatus());
            } else if (response.getStatus() == 200) {
                logger.info("Connection draft updated in DB-API.");
            }
        } catch (Exception e) {
            logger.error("Could not update connection draft in DB-API.", e);
        }
    }
    public void deleteConnectionDraft(String mongoEntityId){
        Request request = createDbapiRequest(HttpMethod.DELETE, DbapiEndpointForCSLScan.DELETE_CONNECTION_DRAFT_BY_MONGO_ENTITY_ID);
        request.param("mongo_entity_id", mongoEntityId);
        try {
            ContentResponse response = request.send();
            if (response.getStatus() != 200) {
                logger.error("Could not delete connection draft in DB-API. Got status code {}", response.getStatus());
            } else if (response.getStatus() == 200) {
                logger.info("Connection draft deleted in DB-API.");
            }
        } catch (Exception e) {
            logger.error("Could not delete connection draft in DB-API.", e);
        }
    }
    public void clearVerifiedConnectionsDraft() {
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.CLEAR_VERIFIED_CONNECTIONS_DRAFT);
        try {
            ContentResponse response = request.send();
            if (response.getStatus() != 200) {
                logger.error("Could not clear verified connections drafts in DB-API. Got status code {}", response.getStatus());
            } else if (response.getStatus() == 200) {
                logger.info("Verified connections drafts cleared in DB-API.");
            }
        } catch (Exception e) {
            logger.error("Could not clear verified connections drafts in DB-API.", e);
        }
    }
    public void clearFailedConnectionsDraft() {
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.CLEAR_FAILED_CONNECTIONS_DRAFT);
        try {
            ContentResponse response = request.send();
            if (response.getStatus() != 200) {
                logger.error("Could not clear failed connections drafts in DB-API. Got status code {}", response.getStatus());
            } else if (response.getStatus() == 200) {
                logger.info("Failed connections drafts cleared in DB-API.");
            }
        } catch (Exception e) {
            logger.error("Could not clear failed connections drafts in DB-API.", e);
        }
    }
    public void publishVerifiedConnectionsDraft() {
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.PUBLISH_VERIFIED_CONNECTION_DRAFT);
        try {
            ContentResponse response = request.send();
            if (response.getStatus() != 200) {
                logger.error("Could not publish verified connections drafts in DB-API. Got status code {}", response.getStatus());
            } else if (response.getStatus() == 200) {
                logger.info("Verified connections drafts published in DB-API.");
            }
        } catch (Exception e) {
            logger.error("Could not publish verified connections drafts in DB-API.", e);
        }
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
        Json requestContents = object();
        apiCommandsList.stream()
                .map(apiCommands -> new Pair<>(apiCommands.getName(), apiCommands.getListOfCommandPrivileges()))
                .filter(Predicate.not(pair -> pair.getSecond().isEmpty()))
                .map(pair -> pair.map((name, map) -> {
                    Json result = object();
                    map.forEach((key, value) -> result.set(key, value.toString()));
                    return new Pair<>(name, result);
                }))
                .forEach(pair -> requestContents.set(pair.getFirst(), pair.getSecond()));
        logger.debug("Sending commands to DB-API: " + requestContents.toString());
//        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.JAVACOMM_SEND_COMMANDS)
//                .content(new StringContentProvider(requestContents.toString()), "application/json");
//        ContentResponse response = request.send();
        ContentResponse response = createAndSendRequest(HttpMethod.POST.toString(), DbapiEndpointForCSLScan.JAVACOMM_SEND_COMMANDS.getEndpoint(), null, requestContents );

        if (response.getStatus() != 200) {
            throw new Exception("Error sending commands to dbapi: got unexpected status " + response.getStatus());
        }
    }

    public List<HttpTemplateImportNotification> getAvailableImportTasks() {
//        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpointForCSLScan.FILE_ACTION_STATUS_AVAILABLE);
        try {
//            ContentResponse response = request.send();
            ContentResponse response = createAndSendRequest(HttpMethod.GET.toString(), DbapiEndpointForCSLScan.FILE_ACTION_STATUS_AVAILABLE.getEndpoint(), null, null );
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

    synchronized public Path downloadHttpTemplatesBsonFile(HttpTemplateImportNotification query) throws ExecutionException, InterruptedException, TimeoutException {
        Json contents = object("file_action_status_id", query.getId());
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.DOWNLOAD_HTTP_TEMPLATES_BSON_FILE.getEndpoint());
        // create contentProvider with the json contents

        StringContentProvider contentProvider = new StringContentProvider(contents.toString());
        request.content(contentProvider);
        request.header(HttpHeader.CONTENT_TYPE, "application/json");

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

    synchronized public void uploadHttpTemplatesBsonFile(ExportQuery exportQuery) {
        MultiPartContentProvider multiPart = new MultiPartContentProvider();
        try {
            Path filePath = fileStorageService.getFilePath(exportQuery.getFilename());
            logger.info("Preparing to upload file: {}, Path: {}, Size: {} bytes",
                    exportQuery.getFilename(), filePath, java.nio.file.Files.size(filePath));

            multiPart.addFilePart("file", exportQuery.getFilename(), new PathContentProvider(filePath), null);
        } catch (IOException e) {
            logger.error("Error adding file to request", e);
            return;
        }
        multiPart.close();
        Request request = createDbApiRequestWithCustomContentType("POST", DbapiEndpointForCSLScan.UPLOAD_HTTP_TEMPLATES_BSON_FILE.getEndpoint(), multiPart.getContentType());
        request.content(multiPart);
        // TODO : this multipart request must be sent with a synchronized method, because the x-correlation-id may cause problems
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
        Json contents = object("status", FileActionStatus.FILE_PROCESSING.getValue());
//        Request request = createDbapiPatchRequest(String.format(DbapiEndpointForCSLScan.FILE_ACTION_STATUS_DETAILS.getEndpoint(), id));
//        request.content(new StringContentProvider(contents.toString()), "application/json");
        try {
//            ContentResponse response = request.send();
            ContentResponse response = createAndSendRequest(ApiHandler.PATCH,String.format(DbapiEndpointForCSLScan.FILE_ACTION_STATUS_DETAILS.getEndpoint(), id), null, contents);

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
                contents = object("status", FileActionStatus.SUCCEEDED.getValue());
                break;
            case ERROR:
                // Add an error status
                contents = object("status", FileActionStatus.FAILED.getValue());
                break;
            default:
                logger.warn("Unknown import status: {}", importQuery.getStatus());
                return;
        }
//        Request request = createDbapiPatchRequest(String.format(DbapiEndpointForCSLScan.FILE_ACTION_STATUS_DETAILS.getEndpoint(), id));
//        request.content(new StringContentProvider(contents.toString()), "application/json");
        try {
//            ContentResponse response = request.send();
            ContentResponse response = createAndSendRequest(ApiHandler.PATCH,String.format(DbapiEndpointForCSLScan.FILE_ACTION_STATUS_DETAILS.getEndpoint(), id), null, contents);
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
        Json contents = object("file_name", exportQuery.getFilename());
//        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpointForCSLScan.FILE_ACTION_STATUS_CREATE_FOR_HTTP_TEMPLATE_EXPORT);
//        request.content(new StringContentProvider(contents.toString()), "application/json");
//        ContentResponse response = request.send();
        ContentResponse response = createAndSendRequest(HttpMethod.POST.toString(), DbapiEndpointForCSLScan.FILE_ACTION_STATUS_CREATE_FOR_HTTP_TEMPLATE_EXPORT.getEndpoint(), null, contents);
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
     * @throws Exception If the request failed.
     */
    public void notifyExportFinished(int id, ExportQuery exportQuery) throws Exception {
        Json contents;
        switch (exportQuery.getStatus()) {
            case SUCCESS:
                contents = object("status", FileActionStatus.SUCCEEDED.getValue());
                break;
            case ERROR:
                // Add an error status
                contents = object("status", FileActionStatus.FAILED.getValue());
                break;
            default:
                logger.warn("Unknown export status: {}", exportQuery.getStatus());
                return;
        }
//        Request request = createDbapiPatchRequest(String.format(DbapiEndpointForCSLScan.FILE_ACTION_STATUS_DETAILS.getEndpoint(), id));
//        request.content(new StringContentProvider(contents.toString()), "application/json");
        try {
//            ContentResponse response = request.send();
            ContentResponse response = createAndSendRequest(ApiHandler.PATCH,String.format(DbapiEndpointForCSLScan.FILE_ACTION_STATUS_DETAILS.getEndpoint(), id), null, contents);

            if (response.getStatus() != 200) {
                logger.warn("Unexpected status code: {}", response.getStatus());
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            logger.error("Error sending import status to DB-API", e);
        }
    }
}
