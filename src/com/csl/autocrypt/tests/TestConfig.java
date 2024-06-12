package com.csl.autocrypt.tests;

import com.csl.core.CSLContext;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.ucsl.json.Json;
import main.services.AutoCryptService;

public class TestConfig {

    protected AutoCryptService service;
    protected static final Json configObj = CSLContext.instance.getConfig();

    static {
        Json autoCrypt = configObj.get("auto_crypt");
        configObj.delAt("auto_crypt");
        autoCrypt.delAt("ip");
        autoCrypt.at("ip", "localhost");
        autoCrypt.delAt("port");
        autoCrypt.at("port", 8002);
        configObj.at("auto_crypt", autoCrypt);
    }

    // API module
    protected static final int PORT_MODULE = configObj.get("auto_crypt").get("port").asInteger(); // Change this to your actual base URL
    protected static String BASE_URL_MODULE = "http://"+configObj.get("auto_crypt").get("ip").asString()+":" + PORT_MODULE;
    protected static final String ENDPOINT_MODULE = "/api";
    // API db
    protected static final int PORT_DBAPI = 8787; // Change this to your actual base URL
    protected static final String BASE_URL_DBAPI = "http://localhost:" + PORT_DBAPI; // Change this to your actual base URL
    protected static final String ENDPOINT_DBAPI = "/api/autocrypt";
    // API client
    protected static final int PORT_CLIENT = 9900; // Change this to your actual base URL
    protected static final String BASE_URL_CLIENT = "http://localhost:" + PORT_CLIENT; // Change this to your actual base URL
    protected static final String ENDPOINT_CLIENT = "/autocrypt";

    protected WireMockServer wireMockServerModule;
    protected WireMockServer wireMockServerBd;
    protected WireMockServer wireMockServer;

    protected String path = "/dev/null";
    protected String commonName = "commonName";
    protected String name = "name";
    protected String ttl = "24h";
    protected String type = "internal";
    protected String roleName = "roleName";
    protected String serialNumber = "serialNumber";
    protected String issuerRef = "issuerRef";
    protected int id = 1;
}
