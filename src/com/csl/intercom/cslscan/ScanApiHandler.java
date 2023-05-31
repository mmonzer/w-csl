package com.csl.intercom.cslscan;

import com.csl.intercom.cslscan.models.CpeItem;
import com.csl.intercom.dbapi.models.Connection;
import com.csl.intercom.dbapi.models.Device;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.JsonApiResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;

import java.net.ConnectException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class to handle communication with CSL-Scan's HTTP API.
 */
public class ScanApiHandler implements AutoCloseable {
    private String scanManagerUrl;
    private HttpClient httpClient = new HttpClient();
    private static ZoneId zoneId = ZoneId.of("Europe/Paris");

    public ScanApiHandler(String scanManagerUrl) {
        this.scanManagerUrl = scanManagerUrl;

        try {
            httpClient.start();
        } catch (Exception e) {
            System.err.println("Could not start the http client for CSL-Scan API.");
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
        return sendRequestToScanManager(HttpMethod.POST, "/entity/", device.serializeForScanner());
//        String protocol = JsonUtil.getStringFromJson(device, "protocol", "");
//        switch (protocol.toLowerCase()) {
//            case "snmpv2c":
//                return addSnmpv2cEntity(
//                        JsonUtil.getStringFromJson(device, "id", null),
//                        JsonUtil.getStringFromJson(device, "name", null),
//                        JsonUtil.getStringFromJson(device, "ip", null),
//                        JsonUtil.getIntFromJson(device, "port", 161),
//                        JsonUtil.getStringFromJson(device, "community", "public")
//                );
//
//            case "snmpv3":
//                return addSnmpv3Entity(
//                        JsonUtil.getStringFromJson(device, "id", null),
//                        JsonUtil.getStringFromJson(device, "name", null),
//                        JsonUtil.getStringFromJson(device, "ip", null),
//                        JsonUtil.getIntFromJson(device, "port", 161),
//                        JsonUtil.getStringFromJson(device, "user", null),
//                        JsonUtil.getStringFromJson(device, "pass", null),
//                        JsonUtil.getStringFromJson(device, "privPassPhrase", null),
//                        JsonUtil.getStringFromJson(device, "securityLevel", null),
//                        JsonUtil.getStringFromJson(device, "authProtocolName", null),
//                        JsonUtil.getStringFromJson(device, "privProtocolName", null)
//                );
//
//            default:
//                return JsonApiResponse.error("Unsupported protocol: " + protocol);
//        }
    }

    /**
     * Send a new SNMPv2c entity to CSL-Scan.
     *
     * @param uuid      The unique id of the new entity.
     * @param name      The name of the entity.
     * @param ip        The IP address of the entity.
     * @param port      The SNMP port on which to contact the entity.
     * @param community The SNMP community of the entity.
     * @return The result from the scanner.
     */
    private JsonApiResponse addSnmpv2cEntity(String uuid, String name, String ip, int port, String community) {
        if (uuid == null || name == null || ip == null) {
            return JsonApiResponse.error("The fields 'id', 'name' and 'ip' are required");
        } else {
            return sendRequestToScanManager(HttpMethod.POST, "/entity/", Json.object(
                    "uuid", uuid,
                    "name", name,
                    "ipAddress", ip,
                    "port", port,
                    "connectionInfo", Json.object(
                            "queryProtocol", "SNMPV2c",
                            "community", community
                    ),
                    "isDeleted", false
            ));
        }
    }

    /**
     * Send a new SNMPv3 entity to CSL-Scan.
     *
     * @param uuid             The unique id of the new entity.
     * @param name             The name of the entity.
     * @param ip               The IP address of the entity.
     * @param port             The SNMP port on which to contact the entity.
     * @param user             The username for the entity.
     * @param pass             The SNMP password of the entity.
     * @param privPassPhrase   The privacy pass phrase of the entity.
     * @param securityLevel    The security level (authPriv, noAuthNoPriv or authNoPriv).
     * @param authProtocolName The authentication protocol (AuthMD5 or SHA-256).
     * @param privProtocolName The privacy protocol (PrivAES128 or PrivDES).
     * @return The result from the scanner.
     */
    private JsonApiResponse addSnmpv3Entity(String uuid, String name, String ip, int port, String user, String pass, String privPassPhrase, String securityLevel, String authProtocolName, String privProtocolName) {
        if (uuid == null || name == null || ip == null) {
            return JsonApiResponse.error("The fields 'id', 'name' and 'ip' are required");
        } else {
            return sendRequestToScanManager(HttpMethod.POST, "/entity/", Json.object(
                    "uuid", uuid,
                    "name", name,
                    "ipAddress", ip,
                    "port", port,
                    "connectionInfo", Json.object(
                            "queryProtocol", "SNMPV3",
                            "user", user,
                            "pass", pass,
                            "privPassPhrase", privPassPhrase,
                            "securityLevel", securityLevel,
                            "authProtocolName", authProtocolName,
                            "privProtocolName", privProtocolName
                    ),
                    "isDeleted", false
            ));
        }
    }

    /**
     * Get the list of configured entities in the scanner.
     *
     * @return A {@link Json} array containing the all the configured entities in the scanner.
     */
    public JsonApiResponse listEntities() {
        return sendRequestToScanManager(HttpMethod.GET, "/entity/", Json.object());
    }

    /**
     * Get a specific entity.
     *
     * @param id The unique identifier created by the scanner, as returned at creation or in a list.
     * @return A {@link Json} containing the specified entity.
     */
    public JsonApiResponse getEntity(String id) {
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.GET, "/entity/" + id, Json.object());
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

    /**
     * Delete an entity from the scanner.
     *
     * @param id The unique identifier of the entity, as returned at creation or in a list.
     * @return An empty object on success, an error message on failure.
     */
    public JsonApiResponse deleteEntity(String id) {
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.DELETE, "/entity/" + id, Json.object());
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
        return sendRequestToScanManager(HttpMethod.GET, "/cpeItem/entity/" + id, Json.object());
    }

    /**
     * Get the status of a specific scan task.
     *
     * @param id The unique id of the task.
     * @return The status of the scan.
     */
    public JsonApiResponse getScanStatus(String id) {
        return sendRequestToScanManager(HttpMethod.GET, "/discovery/status/" + id, Json.object());
    }

    /**
     * Get the status of an entity's scan
     *
     * @param id The entity's unique id.
     * @return The status of the scan.
     */
    public JsonApiResponse getEntityScanStatus(String id) {
        return sendRequestToScanManager(HttpMethod.GET, "/status/entity/" + id, Json.object());
    }

    /**
     * Fetch CSL-Scan's status.
     *
     * @return A {@link JsonApiResponse} with CSL-Scan's status as it was received, or with an error in the 'error' field.
     */
    public JsonApiResponse getStatus() {
        return sendRequestToScanManager(HttpMethod.GET, "/discovery/status", Json.object());
    }

    /**
     * Request the deletion of a CPE Item to CSL-Scan.
     *
     * @param id The uuid of the CPE Item to delete.
     */
    public void deleteCpeItemFromScan(String id) {
        sendRequestToScanManager(HttpMethod.DELETE, "/cpeItem/entity/" + id, Json.object());
    }

    /**
     * Request multiple deletions of CPE Items to CSL-Scan.
     *
     * @param ids The list of CPE Items to delete.
     */
    public void deleteCpeItemsFromScan(List<String> ids) {
        for (String id : ids) {
            deleteCpeItemFromScan(id);
        }
    }

    /**
     * Get the CPE items that have changed since the specified date.
     *
     * @param date The date to start receiving notifications. May be null to retrieve all the items.
     * @return A {@link Json} array containing the CPE items that have changed since the specified date, or all the items if date was null.
     */
    public List<CpeItem> getCpeItemChangesSince(OffsetDateTime date) {
        JsonApiResponse response;
        Json cpeItems = Json.array();
        if (date == null) {
            response = sendRequestToScanManager(HttpMethod.GET, "/cpeItem/", Json.object());
        } else {
            response = sendRequestToScanManager(HttpMethod.GET, "/cpeItem/", Json.object("date", ScanUtils.localTimeToScan(date).toString()));
        }
        if (response.isSuccess() && response.getExtra().get("status_code").asInteger() == 200) {
            cpeItems = response.getResult();
        } else {
            return null;
        }
        List<CpeItem> cpeItemsList = new ArrayList<>(cpeItems.asJsonList().size());
        for (Json cpeItem : cpeItems.asJsonList()) {
            CpeItem parsedCpeItem = CpeItem.fromScanCpeItem(cpeItem);
            // Remove the items that have the *exact* same date as whe previously had
            if (!parsedCpeItem.getDiscoveredDate().equals(date)) {
                cpeItemsList.add(parsedCpeItem);
            }
        }
        return cpeItemsList;
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
//        System.out.println(method.asString() + " " + URI);
//        System.out.println("Payload: " + params.toString());
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
            res = JsonApiResponse.error(e.getMessage());
        } catch (Exception e) {
            if (e.getCause() instanceof ConnectException) {
                res = JsonApiResponse.error("Connection error with CSL-Scan");
            }
//            e.printStackTrace(System.err);
        }
        return res;
    }

    public OffsetDateTime getLastLastEntityUpdateDate() {
        JsonApiResponse response = sendRequestToScanManager(HttpMethod.GET, "/entity/last_update", Json.object());
        try {
            String dateString = response.getResult().get("value").asString().replace("\"", "");
            return ScanUtils.scanTimeToLocal(LocalDateTime.parse(dateString));
        } catch (NullPointerException e) {
            return null;
        }
    }
}
