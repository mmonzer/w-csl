package com.csl.auth;

import com.ucsl.json.Json;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds the list of supported clients loaded from a JSON file.
 */
public class SupportedClients {
    private static final String RESOURCE = "supported_clients.json";
    private static final Map<String, ClientInfo> clients = new HashMap<>();

    static {
        load();
    }

    private static class ClientInfo {
        final String password;
        final String authType;
        final String certFile;

        ClientInfo(String password, String certFile, String authType) {
            this.password = password;
            this.certFile = certFile;
            this.authType = authType;
        }
    }

    private static void load() {
        try (InputStream in = SupportedClients.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) return;
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Json root = Json.read(content);
            for (Json j : root.at("clients").asJsonList()) {
                String id = j.at("id").asString();
                String password = j.at("password").asString();
                String cert = j.has("certificate") ? j.at("certificate").asString() : "";
                String authType = j.has("auth_type") ? j.at("auth_type").asString() : "password";
                clients.put(id, new ClientInfo(password, cert, authType));
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Validates provided credentials against the loaded configuration.
     */
    public static boolean authenticate(String id, String password) {
        if (!clients.containsKey(id)) return false;
        ClientInfo info = clients.get(id);
        if (!"password".equalsIgnoreCase(info.authType)) return false;
        return info.password != null && info.password.equals(password);
    }

    public static boolean isKnown(String id) {
        return clients.containsKey(id);
    }
}
