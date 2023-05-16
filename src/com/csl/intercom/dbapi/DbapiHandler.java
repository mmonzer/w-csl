package com.csl.intercom.dbapi;

import com.csl.core.CSLContext;
import com.csl.intercom.dbapi.enums.DbapiEndpoint;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manage HTTP communications with DB-API.
 * Provides an interface for retrieving the devices, connections and so on,
 * and to send information to it (CPE Items, a Scan's status, ...).
 */
public class DbapiHandler implements AutoCloseable {
    private String dbapiUrl;
    private String apiKey;
    private HttpClient dbapiHttpClient = new HttpClient();
    private ZoneId zoneId;
    static private AtomicInteger lastScanId = new AtomicInteger(0);

    private static final Map<String, String> connectionFieldsDbapiToLocal = new HashMap<>() {{
        put("discovery_protocol", "protocol");
        put("port_number", "port");
        put("connected_devices", "devices");
        put("snmp_community", "community");
        put("username", "user");
        put("read_only_password", "pass");
        put("snmp_privacy_key", "privPassPhrase");
        put("authentication_algorithm", "authProtocolName");
        put("privacy_algorithm", "privProtocolName");
    }};
    private static final Map<String, String> authAlgorithmDbapiToScan = new HashMap<>() {{
        put("SHA-224", "AuthHMAC128SHA224");
        put("SHA-256", "AuthHMAC192SHA256");
        put("SHA-384", "AuthHMAC256SHA384");
        put("SHA-512", "AuthHMAC384SHA512");
        put("SHA", "AuthSHA");
        put("SHA2", "AuthSHA2");
        put("MD5", "AuthMD5");
    }};

    private static final Map<String, String> privAlgorithmeDbapiToScan = new HashMap<>() {{
        put("AES", "PrivAES128");
        put("AES-128", "PrivAES128");
        put("AES-192", "PrivAES192");
        put("AES-256", "PrivAES256");
        put("DES", "PrivDES");
    }};

    public DbapiHandler() {
        this(CSLContext.instance.getConfig());
    }

    public DbapiHandler(Json config) {
        Json globalConfig = config.get("global");
        dbapiUrl = JsonUtil.getBooleanFromJson(globalConfig, "use_ssl", true) ? "https://" : "http://";
        dbapiUrl += JsonUtil.getStringFromJson(globalConfig, "ip_server_remote", "localhost");
        dbapiUrl += "/api";
        apiKey = JsonUtil.getStringFromJson(globalConfig, "api_key", "");
        zoneId = ZoneId.of(JsonUtil.getStringFromJson(globalConfig, "timezone", "Europe/Paris"));
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
     * Send a CPE Item to DB-API
     *
     * @param cpeItem The CPE Item to send
     * @throws Exception If the sending fail
     */
    public void sendCpeItem(Json cpeItem, boolean isLast) throws Exception {
        Json requestContents = Json.object("cpe_data", cpeItem);
        if (cpeItem.has("updatedAt")) {
            requestContents.set("discovered_date", cpeItem.get("updatedAt"));
        }
        if (cpeItem.has("uuid")) {
            requestContents.set("mongo_entity_id", cpeItem.get("uuid"));
        }
        requestContents.set("device", cpeItem.get("entityUuid").asString());
        requestContents.set("event_id", lastScanId.get());
        requestContents.set("is_last_item", isLast);
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpoint.CPE_ITEMS)
                .content(new StringContentProvider(requestContents.toString()), "application/json");
        ContentResponse response = request.send();
        if (response.getStatus() != 201) {
            throw new Exception("Error sending CpeItem to dbapi: got unexpected status " + response.getStatus());
        }
    }

    /**
     * Send a list of CPE Items to DB-API
     *
     * @param cpeItems A {@link List <Json>} with the CPE Items to send
     * @throws Exception If any item failed
     */
    public void sendCpeItems(Json cpeItems) throws Exception {
        Json failedItems = Json.array();
        List<Json> cpeItemsList = cpeItems.asJsonList();
        int cpeItemsNumber = cpeItemsList.size();
        int cpeItemsCount = 0;

        for (Json cpeItem : cpeItemsList) {
            cpeItemsCount++;
            try {
                sendCpeItem(cpeItem, cpeItemsCount == cpeItemsNumber);
            } catch (Exception e) {
                failedItems.add(cpeItem.get("uuid"));
            }
        }
        if (!failedItems.asJsonList().isEmpty()) {
            throw new Exception("Errors sending the following CPE Items: " + failedItems.toString());
        }
    }

    /**
     * Fetch the last updated date of CPE Items in DB-API.
     *
     * @return The last update of CPE-Items in DB-API.
     * @throws Exception If it was not possible to fetch from DB-API or the format was not recognised.
     */
    public LocalDateTime getCpeItemsLastUpdateDate() throws Exception {
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
        return dbapiDateToLocal(lastUpdatedDateString);
    }

    /**
     * Get the devices from DB-API that were changed since an optional date.
     *
     * @param date The date of start of modifications to fecth. May be null, in wich case fetches all devices.
     * @return The {@link List<Json>} of devices that were changed since date.
     * @throws Exception If the fetching failed.
     */
    public List<Json> getDevicesSince(LocalDateTime date) throws Exception {
        OffsetDateTime dateUtc = localDateToDbapi(date);
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.DEVICES);
        if (dateUtc != null) {
            request.param("updated_at__gte", dateUtc.toString());
        }
        Json response = Json.read(request.send().getContentAsString());

        List<Json> devices = new ArrayList<>();
        for (Json jsonDevice : response.asJsonList()) {
            Json device = parseDbapiDevice(jsonDevice);
            devices.add(device);
        }
        return devices;
    }


    /**
     * Get the connections from DB-API that were changed since an optional date.
     *
     * @param date The date of start of modifications to fecth. May be null, in wich case fetches all connections.
     * @return The {@link List<Json>} of connections that were changed since date.
     * @throws Exception If the fetching failed.
     */
    public List<Json> getConnectionsSince(LocalDateTime date) throws Exception {
        OffsetDateTime dateUtc = localDateToDbapi(date);
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.CONNECTIONS);
        if (dateUtc != null) {
            request.param("updated_at__gte", dateUtc.toString());
        }
        Json response = Json.read(request.send().getContentAsString());
        List<Json> connections = new ArrayList<>(response.asList().size());
        for (Json jsonConnection : response.asJsonList()) {
            Json connection = parseDbapiConnection(jsonConnection);
            connections.add(connection);
        }
        return connections;
    }


    /**
     * Get the deleted devices from DB-API that were changed since an optional date.
     *
     * @param date The date of start of deletions to fecth. May be null, in wich case fetches all deletions.
     * @return The {@link List<String>} of device uuids that were deleted since date.
     * @throws Exception If the fetching failed.
     */
    public List<String> getDeletedDevicesSince(LocalDateTime date) throws Exception {
        OffsetDateTime dateUtc = localDateToDbapi(date);
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
    public List<String> getDeletedCpeItemsSince(LocalDateTime date) throws Exception {
        OffsetDateTime dateUtc = localDateToDbapi(date);
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.DELETED_CPE_ITEMS);
        if (dateUtc != null) {
            request.param("deleted_date__gte", dateUtc.toString());
        }
        ContentResponse response = request.send();
        if (response.getStatus() != 200) {
            throw new Exception("Unexpected status code " + response.getStatus());
        }
        List<String> deletedCpeItems = new ArrayList<>();
        for (Json cpeItem : Json.read(response.getContentAsString()).asJsonList()) {
            if (cpeItem.isString()) {
                deletedCpeItems.add(cpeItem.asString());
            } else if (cpeItem.isObject()) {
                deletedCpeItems.add(cpeItem.get("object_repr").asString());
            }
        }
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
    public List<Json> fetchDevices(List<String> uuids) throws ExecutionException, InterruptedException, TimeoutException {
        List<Json> devices = new ArrayList<>();
        if (uuids == null || uuids.isEmpty()) {
            return devices;
        }
        Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.DEVICES);
        request.param("uuid", String.join(",", uuids));
        for (Json device : Json.read(request.send().getContentAsString()).asJsonList()) {
            devices.add(parseDbapiDevice(device));
        }
        return devices;
    }

    /**
     * Parse a device as received from DB-API and return a format suitable for our use.
     *
     * @param dbapiDevice The device as we got it from DB-API.
     * @return A {@link Json} with a format suitable for further processing:
     * <code>
     * {
     * "id": uuid (String),
     * "name": name (String),
     * "ip": IP address (String)
     * }
     * </code>
     */
    private static Json parseDbapiDevice(Json dbapiDevice) {
        Json device = Json.object(
                "id", dbapiDevice.get("uuid"),
                "name", dbapiDevice.get("name")
        );
        if (dbapiDevice.has("ipv4")) {
            device.set("ip", dbapiDevice.get("ipv4"));
        } else if (dbapiDevice.has("ipv6")) {
            device.set("ip", dbapiDevice.get("ipv6"));
        }
        Json connectionsIdJson = dbapiDevice.get("connections");
        Integer connectionId = null;
        if (connectionsIdJson != null) {
            if (connectionsIdJson.isArray() && !connectionsIdJson.asJsonList().isEmpty()) {
                connectionId = connectionsIdJson.asJsonList().get(0).asInteger();
            }
        }
        device.set("connection", connectionId);
        return device;
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
    public List<Json> fetchConnections(List<Integer> ids) throws ExecutionException, InterruptedException, TimeoutException {
        List<Json> connections = new ArrayList<>();
        for (int id : ids) {
            Request request = createDbapiRequest(HttpMethod.GET, DbapiEndpoint.CONNECTIONS);
            request.param("id", String.valueOf(id));
            Json response = Json.read(request.send().getContentAsString());
            if (response.isArray()) {
                connections.add(parseDbapiConnection(response.at(0)));
            } else {
                connections.add(parseDbapiConnection(response));
            }
        }
        return connections;
    }

    /**
     * Get a connection in a {@link List<Json>} from its id.
     *
     * @param connections The list of connections to search
     * @param id          The ID of the connection we seek.
     * @return The {@link Json} of the connection we sought, or null if not found.
     */
    public static Json getConnectionById(List<Json> connections, int id) {
        for (Json connection : connections) {
            if (connection.get("id").asInteger() == id) {
                return connection;
            }
        }
        return null;
    }

    /**
     * Get a device in a {@link List<Json>} from its id.
     *
     * @param devices The list of devices to search
     * @param id      The ID of the device we seek.
     * @return The {@link Json} of the devices we sought, or null if not found.
     */
    public static Json getDeviceById(List<Json> devices, String id) {
        for (Json device : devices) {
            if (device.get("id").asString().equals(id)) {
                return device;
            }
        }
        return null;
    }

    /**
     * Get a protocol in a {@link List<Json>} from its id.
     *
     * @param protocols The list of protocols to search
     * @param id        The ID of the protocol we seek.
     * @return The {@link Json} of the protocols we sought, or null if not found.
     */
    public static String getProtocolById(List<Json> protocols, int id) {
        String name;
        for (Json protocol : protocols) {
            if (protocol.get("id").asInteger() == id) {
                name = protocol.get("name").asString().toLowerCase();
                switch (name) {
                    case "snmpv1":
                        return "SNMPV1";
                    case "snmpv2c":
                        return "SNMPV2c";
                    case "snmpv3":
                        return "SNMPV3";
                }
                return name;
            }
        }
        return null;
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
    public int notifyScanStarted(LocalDateTime startDate) {
        Json params = Json.object("started_at", localDateToDbapi(startDate).toString());
        Request request = createDbapiRequest(HttpMethod.POST, DbapiEndpoint.SCAN_EVENT_CREATION)
                .header(HttpHeader.CONTENT_TYPE, "application/json")
                .content(new StringContentProvider(params.toString()));
        try {
            ContentResponse response = request.send();
            int id = JsonUtil.getIntFromJson(Json.read(response.getContentAsString()), "id", 0);
            this.lastScanId.set(id);
            return id;
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
        return notifyScanStarted(LocalDateTime.now());
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
        Request request = createDbapiPatchRequest(String.format(DbapiEndpoint.SCAN_EVENT_UPDATE.getEndpoint(), scan.getDbapiId()));
        Json params = Json.object(
                "ended_at", localDateToDbapi(scan.getEndDate()).toString(),
                "is_success", String.valueOf(scan.isSuccess())
        );
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
     * Parse a connection as received from DB-API and return a format suitable for our use.
     *
     * @param dbapiConnection The connection as we got it from DB-API.
     * @return A {@link Json} with a format suitable for further processing:
     * <code>
     * {
     * "id": uuid (int),
     * "protocol": id (int),
     * "port": port number (int),
     * "devices": device uuids list ([String]),
     * "community": SNMP community (String)
     * }
     * </code>
     */
    private static Json parseDbapiConnection(Json dbapiConnection) {
        Json connection = Json.object(
                "id", dbapiConnection.get("id")
        );
        for (Map.Entry<String, String> field : connectionFieldsDbapiToLocal.entrySet()) {
            String key = field.getKey();
            String value = field.getValue();
            if (dbapiConnection.has(key) && !dbapiConnection.get(key).isNull()) {
                Json dbapiField = dbapiConnection.get(key);
                if (key.equals("authentication_algorithm")) {
                    connection.set(value, authAlgorithmDbapiToScan.getOrDefault(dbapiField.asString(), dbapiField.asString()));
                } else if (key.equals("privacy_algorithm")) {
                    connection.set(value, privAlgorithmeDbapiToScan.getOrDefault(dbapiField.asString(), dbapiField.asString()));
                } else {
                    connection.set(value, dbapiField);
                }
            }
        }

        Json otherData = dbapiConnection.get("read_only_other_data");
        String authString = "noAuth";
        String privacyString = "NoPriv";
        if (otherData.has("privacy_algorithm")) {
            privacyString = "Priv";
        }
        if (otherData.has("authentication_algorithm")) {
            authString = "auth";
        }
        connection.set("securityLevel", authString + privacyString);
        for (Map.Entry<String, String> field : connectionFieldsDbapiToLocal.entrySet()) {
            String key = field.getKey();
            String value = field.getValue();
            if (otherData.has(key) && !otherData.get(key).isNull()) {
                Json dbapiField = otherData.get(key);
                if (key.equals("authentication_algorithm")) {
                    connection.set(value, authAlgorithmDbapiToScan.getOrDefault(dbapiField.asString(), dbapiField.asString()));
                } else if (key.equals("privacy_algorithm")) {
                    connection.set(value, privAlgorithmeDbapiToScan.getOrDefault(dbapiField.asString(), dbapiField.asString()));
                } else {
                    connection.set(value, dbapiField);
                }
            }
        }

        return connection;
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
     * Translate a date from DB-API's datetime format to LocalDateTime.
     *
     * @param dateTime A serialized date as received from DB-API.
     * @return The {@link LocalDateTime} corresponding to the date provided by DB-API.
     */
    private LocalDateTime dbapiDateToLocal(String dateTime) {
        return dbapiDateToLocal(OffsetDateTime.parse(dateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    /**
     * Translate a date from DB-API's datetime format to LocalDateTime.
     *
     * @param dateTime An {@link OffsetDateTime} date as received from DB-API.
     * @return The {@link LocalDateTime} corresponding to the date provided by DB-API.
     */
    private LocalDateTime dbapiDateToLocal(OffsetDateTime dateTime) {
        return dateTime.atZoneSameInstant(zoneId).toLocalDateTime();
    }

    /**
     * Translate local date to UTC, as used by CSL-Scan.
     *
     * @param localDateTime The local date time.
     * @return The same date time in UTC.
     */
    private OffsetDateTime localDateToDbapi(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        OffsetDateTime utcDateTime = OffsetDateTime.parse(localDateTime.atZone(zoneId).toInstant().toString());
        return utcDateTime;
    }
}
