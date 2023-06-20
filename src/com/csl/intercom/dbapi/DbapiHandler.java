package com.csl.intercom.dbapi;

import com.csl.core.CSLContext;
import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.models.CpeItem;
import com.csl.intercom.dbapi.enums.DbapiEndpoint;
import com.csl.intercom.dbapi.enums.FinishedScanStatus;
import com.csl.intercom.dbapi.models.Connection;
import com.csl.intercom.dbapi.models.Device;
import com.csl.intercom.dbapi.models.ScanEntity;
import com.csl.intercom.dbapi.models.ScansList;
import com.csl.util.Pair;
import com.google.common.util.concurrent.AtomicDouble;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.JsonApiResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
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
    static private AtomicInteger lastScanId = new AtomicInteger(0);
    static private AtomicDouble lastScanProgress = new AtomicDouble(0);

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
            e.printStackTrace(System.err);
        }
    }

    public void close() {
        try {
            dbapiHttpClient.stop();
        } catch (Exception e) {
            e.printStackTrace(System.err);
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
            System.err.println("[" + LocalDateTime.now().toString() + "] Could not delete the CPE Items from DB-API.");
        }
    }

    /**
     * Takes a list of {@link CpeItem} and regroups them by device id.
     *
     * @param cpeItems The list of {@link CpeItem} to classify.
     * @return A Map that associates a device id with the list of {@link CpeItem}s that have this id.
     */
    private Map<String, List<CpeItem>> classifyCpeItemsByDevice(List<CpeItem> cpeItems) {
        Map<String, List<CpeItem>> result = new HashMap<>();
        for (CpeItem cpeItem : cpeItems) {
            String deviceId = cpeItem.getDeviceId();
            if (!result.containsKey(deviceId)) {
                result.put(deviceId, new ArrayList<>());
            }
            result.get(deviceId).add(cpeItem);
        }
        return result;
    }

    /**
     * Send a CPE Item to DB-API
     *
     * @param cpeItems The CPE Items to send in a Json array
     * @throws Exception If the sending fail
     */
    private void sendCpeItemsBatch(List<CpeItem> cpeItems) throws Exception {
        ScansList scansList = ScansList.instance;
        ScanEntity scan = scansList.getRunningScan();
        if (scan == null) {
            scan = scansList.getFinishedScan();
            // If we found no running scans and no finished scan, we do not send the CPE Items
            if (scan == null) return;
        }

        Map<String, List<CpeItem>> classifiedCpeItems = classifyCpeItemsByDevice(cpeItems);
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
                "discovered_cpe_dict_arr", cpeItemsArray
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
     * @param cpeItems A {@link List <Json>} with the CPE Items to send
     * @throws Exception If any item failed
     */
    public void sendCpeItems(List<CpeItem> cpeItems) throws Exception {
        Json failedItems = Json.array();
        List<CpeItem> newItems = cpeItems.stream().filter(Predicate.not(CpeItem::isDeleted)).collect(Collectors.toList());
        List<CpeItem> deletedItems = cpeItems.stream().filter(CpeItem::isDeleted).collect(Collectors.toList());

        try {
            if (!deletedItems.isEmpty()) {
                deleteCpeItemsFromDbapi(deletedItems);
            }
            sendCpeItemsBatch(newItems);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            cpeItems.stream().map(CpeItem::getMongoEntityId).forEach(failedItems::add);
            throw new Exception("Error sending the following CPE Items: " + failedItems.toString());
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
            request.param("updated_at__gte", dateUtc.toString());
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
    public List<Connection> getConnectionsSince(OffsetDateTime date) throws Exception {
        OffsetDateTime dateUtc = DbapiUtils.localDateToDbapi(date);
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.CONNECTIONS);
        if (dateUtc != null) {
            request.param("updated_at__gte", dateUtc.toString());
        }
        Json response = Json.read(request.send().getContentAsString());
        return response.asJsonList().stream()
                .map(Connection::fromJson)
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
    public List<String> getDeletedDevicesSince(OffsetDateTime date) throws Exception {
        OffsetDateTime dateUtc = DbapiUtils.localDateToDbapi(date);
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.DELETED_DEVICES);
        if (dateUtc != null) {
            request.param("deleted_date__gte", dateUtc.toString());
        }
        Json response = Json.read(request.send().getContentAsString());
        List<String> deletedDevices = new ArrayList<>(response.asList().size());
        for (Json deletedDevice : response) {
            if (deletedDevice.isString()) {
                deletedDevices.add(deletedDevice.asString());
            } else if (deletedDevice.isObject()) {
                deletedDevices.add(deletedDevice.get("object_id").asString());
            }
        }
        return deletedDevices;
    }

    /**
     * Get the deleted devices from DB-API that were changed since an optional date.
     *
     * @param date The date of start of deletions to fecth. May be null, in wich case fetches all deletions.
     * @return The {@link List<String>} of CPE Item uuids that were deleted since date.
     * @throws Exception If the fetching failed.
     */
    public List<Pair<String, OffsetDateTime>> getDeletedCpeItemsSince(OffsetDateTime date) throws Exception {
        OffsetDateTime dateUtc = DbapiUtils.localDateToDbapi(date);
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.GET_DELETED_CPE_ITEMS);
        if (dateUtc != null) {
            request.param("deleted_date__gte", dateUtc.toString());
        }
        ContentResponse response = request.send();
        if (response.getStatus() != 200) {
            throw new Exception("Unexpected status code " + response.getStatus());
        }
        List<Pair<String, OffsetDateTime>> deletedCpeItems = Json.read(response.getContentAsString()).asJsonList().stream()
                .map(json -> new Pair<>(json.get("object_repr").asString(), DbapiUtils.dbapiDateToLocal(json.get("deleted_at").asString())))
                .collect(Collectors.toList());
//        for (Json cpeItem : Json.read(response.getContentAsString()).asJsonList()) {
//            Pair<String, OffsetDateTime> deletedCpeItem = new Pair<>(cpeItem.get("object_repr").asString(), DbapiUtils.dbapiDateToLocal(cpeItem.get("deleted_date").asString()));
//            deletedCpeItems.add(deletedCpeItem);
//        }
        return deletedCpeItems;
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
    public List<Connection> fetchConnections(List<Integer> ids) throws ExecutionException, InterruptedException, TimeoutException {
        List<Connection> connections = new ArrayList<>();
        for (int id : ids) {
            Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.CONNECTIONS);
            request.param("id", String.valueOf(id));
            Json response = Json.read(request.send().getContentAsString());
            Connection connection;
            if (response.isArray()) {
                connection = Connection.fromJson(response.at(0));
            } else {
                connection = Connection.fromJson(response);
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
    public List<Json> fetchDiscoveryProtocols() throws ExecutionException, InterruptedException, TimeoutException {
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.DISCOVERY_PROTOCOLS);
        return Json.read(request.send().getContentAsString()).asJsonList();
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
        // Do nothing if the scan is not actually finished.
        if (!scan.isFinished()) return;
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
            e.printStackTrace(System.err);
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
        List<String> deletedDevices = new ArrayList<>();
        List<String> failedDevices = new LinkedList<>();
        try {
            OffsetDateTime lastDeviceModification = scanApiHandler.getLastLastEntityUpdateDate();
            Pair<List<Device>, List<String>> buildResult = buildNewDevices(
                    getDevicesSince(lastDeviceModification),
                    getConnectionsSince(lastDeviceModification)
            );
            newDevices = buildResult.getFirst();
            deletedDevices.addAll(buildResult.getSecond());
            deletedDevices.addAll(getDeletedDevicesSince(lastDeviceModification));
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return JsonApiResponse.error("Could not get changes from DBAPI");
        }
//        lastDeviceModificationVerification = currentTime;
        for (Device newDevice : newDevices) {
            try {
                sendNewDeviceToScanner(newDevice, scanApiHandler);
            } catch (Exception e) {
                failedDevices.add(newDevice.getId());
            }
        }

        for (String deletedDevice : deletedDevices) {
            try {
                scanApiHandler.deleteEntity(deletedDevice);
            } catch (Exception e) {
                failedDevices.add(deletedDevice);
            }
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
    private Pair<List<Device>, List<String>> buildNewDevices(List<Device> devices, List<Connection> connections) {
        //region List the uuids we have in both list
        List<String> devicesToDelete = new ArrayList<>();
        List<Integer> connectionUuidsInDevices = new ArrayList<>();
        List<String> deviceUuidsInConnections = new ArrayList<>();

        for (Device device : devices) {
            List<Integer> connectionsIds = device.getConnectionsIds();
//            if (connectionsIds.isEmpty()) {
//                devicesToDelete.add(device.getId());
//            } else {
            connectionUuidsInDevices.addAll(connectionsIds);
//            }
        }
        for (Connection connection : connections) {
            deviceUuidsInConnections.addAll(connection.getDevicesIds());
        }
        //endregion

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
        //endregion

        //region Get the missing parts
        try {
            connections.addAll(fetchConnections(connectionsToGet));
            devices.addAll(fetchDevices(devicesToGet));
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace(System.err);
        }
        //endregion

        // Fill the connections into devices
        devices.forEach(device -> device.setConnections(connections));

        return new Pair<>(devices, devicesToDelete);
    }
}
