import com.csl.core.CSLContext;
import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.ScanUtils;
import com.csl.intercom.cslscan.ScanWebSocketHandler;
import com.csl.intercom.cslscan.models.CpeItem;
import com.csl.intercom.dbapi.DbapiHandler;
import com.csl.intercom.services.CpeItemsSynchronizationService;
import com.csl.intercom.services.CpeScanService;
import com.csl.intercom.services.DataSynchronizationService;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.DiscoveryServices;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DiscoveryServicesTest {
    Json jConfig;

    private DiscoveryServices discoveryServices;

    private ScanApiHandler scanApiHandler;
    private ScanWebSocketHandler scanWebSocketHandler;
    private CpeScanService cpeScanService;
    private DataSynchronizationService deletedCpeItemsSynchronizationService;
    private DataSynchronizationService cpeItemsSynchronizationService;
    private DbapiHandler dbapiHandler;
    private DataSynchronizationService microsoftKbSynchronizationService;


    @BeforeEach
    void setUp() {
        jConfig = CSLContext.instance.getConfig();
        MockitoAnnotations.openMocks(this);

        discoveryServices = new DiscoveryServices();
        scanApiHandler = new ScanApiHandler();
        cpeScanService = new CpeScanService();
        dbapiHandler = new DbapiHandler();

        microsoftKbSynchronizationService = new CpeItemsSynchronizationService(cpeScanService);
        deletedCpeItemsSynchronizationService = new CpeItemsSynchronizationService(cpeScanService);
        cpeItemsSynchronizationService = new CpeItemsSynchronizationService(cpeScanService);

        scanWebSocketHandler = new ScanWebSocketHandler(discoveryServices, ScanUtils.generateScanDiscoveryUrlFromConfig(jConfig) ,cpeScanService);

        cpeScanService.init(deletedCpeItemsSynchronizationService, microsoftKbSynchronizationService);


        discoveryServices.setCpeScanService(cpeScanService);
        discoveryServices.setDbapiHandler(dbapiHandler);
        discoveryServices.setScanApiHandler(scanApiHandler);
        discoveryServices.setCpeItemSynchronizationService(cpeItemsSynchronizationService);
        discoveryServices.setDeletedCpeItemsSynchronizationService(deletedCpeItemsSynchronizationService);
        discoveryServices.setScanWebSocketHandler(scanWebSocketHandler);
    }

    /*Test unitaire insuffisant */
    //TODO : A tester en test intégration
    @BeforeEach
    @Test
    void initTest(){
        //Given
        String cslDir = "cslDir";
        Json statusExpected = Json.object();

        statusExpected.set("is_http_api_reachable", true);
        Json websocketStatus = scanWebSocketHandler.getStatus();
        boolean requests_ws_status = JsonUtil.getBooleanFromJson(websocketStatus, "is_requests_websocket_connected", false);
        boolean notifications_ws_status = JsonUtil.getBooleanFromJson(websocketStatus, "is_notifications_websocket_connected", false);
        statusExpected.set("is_websocket_connected", requests_ws_status && notifications_ws_status);

        //When
        boolean reussie = discoveryServices.init(jConfig, cslDir);
        Json status = discoveryServices.getStatus();
        //Then
        assert(reussie);
        assert(status.toString().equals(statusExpected.toString()));
    }

    @Test
    void terminateTest(){
        //when
        boolean terminate = discoveryServices.terminate();
        //then
        assertFalse(terminate);
    }

    @Test
    void getAllCpesTest(){
        //when
        List<CpeItem> result = discoveryServices.getAllCpes();
        System.out.println("Les CPEs : "+result.toString());
        //then
        assertNotNull(result);
    }
    @Test
    void syncAllTest(){
        //when
        discoveryServices.syncAll();
        //then
        assertDoesNotThrow(()->dbapiHandler.sendNewDevicesToScanner(scanApiHandler));
    }
}