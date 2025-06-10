package com.csl.auth;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads client credentials from an environment style file.
 */
public class ClientCredentials {
    private static final String DEFAULT_FILE = "clientConfig.env";
    private String id;
    private String password;
    private String certFile;
    private String authType;

    private static ClientCredentials INSTANCE;

    public static ClientCredentials get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        INSTANCE = loadFromFile(DEFAULT_FILE);
    }

    public static ClientCredentials loadFromFile(String path) {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            return new ClientCredentials();
        }
        ClientCredentials c = new ClientCredentials();
        c.id = props.getProperty("CLIENT_ID", "");
        c.password = props.getProperty("PASSWORD", "");
        c.certFile = props.getProperty("CERT_FILE", "");
        c.authType = props.getProperty("AUTH_TYPE", "password");
        return c;
    }

    public String getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public String getCertFile() {
        return certFile;
    }

    public String getAuthType() {
        return authType;
    }
}
