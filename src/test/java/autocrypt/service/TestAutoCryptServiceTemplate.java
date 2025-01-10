package autocrypt.service;

import autocrypt.TestConfig;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.ucsl.json.Json;
import main.services.AutoCryptService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class TestAutoCryptServiceTemplate extends TestConfig {
    
    
    @BeforeEach
    public void setUp() {
        // Mock the module
        wireMockServerModule = new WireMockServer(PORT_MODULE);
        wireMockServerModule.start();
        wireMockServerBd = new WireMockServer(8787);
        wireMockServerBd.start();
        // This ensures that we touch the DB
        Json globalConfig = configObj.get("global");
        globalConfig.delAt("ip_server_remote");
        globalConfig.at("ip_server_remote", "localhost:"+PORT_DBAPI);
        globalConfig.delAt("api_key");
        globalConfig.at("api_key", "");
        globalConfig.delAt("use_ssl");
        globalConfig.at("use_ssl", false);

        Json config = Json.object();
        config.at("ip", configObj.get("auto_crypt").get("ip").asString());
        config.at("port", PORT_MODULE);
        config.at("global", globalConfig);

        service = new AutoCryptService();
        service.init();
    }

    @AfterEach
    public void tearDown() {
        // Stop the WireMock server
        wireMockServerModule.stop();
        wireMockServerBd.stop();
    }

}
