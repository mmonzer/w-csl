package services;

import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.ScanWebSocketHandler;
import com.csl.intercom.cslscan.models.CpeItem;
import com.csl.intercom.dbapi.DbapiHandlerForCSLScan;
import com.csl.intercom.services.CpeScanService;
import com.csl.intercom.services.DataSynchronizationService;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.services.DiscoveryServices;
import main.services.JsonApiResponse;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class DiscoveryServicesTest {
    Config jConfig = CSLContext.getInstance().getConfig();

    private DiscoveryServices discoveryServices;

    private ScanApiHandler scanApiHandler;
    @Mock
    private ScanWebSocketHandler scanWebSocketHandler;
    private CpeScanService cpeScanService;

    @Mock
    private DataSynchronizationService deletedCpeItemsSynchronizationService;
    @Mock
    private DataSynchronizationService cpeItemsSynchronizationService;
    @Mock
    private DataSynchronizationService microsoftKbSynchronizationService;

    private DbapiHandlerForCSLScan dbapiHandler;


    @BeforeEach
    void setUp() {
        jConfig.Scan.setManagerIp("localhost");

        MockitoAnnotations.openMocks(this);
        when(scanWebSocketHandler.requestScan(any())).thenReturn(JsonApiResponse.success());

        discoveryServices = new DiscoveryServices();
        scanApiHandler = new ScanApiHandler();
        cpeScanService = new CpeScanService();
        dbapiHandler = new DbapiHandlerForCSLScan();
        //Throw exception beacause of the syncAll method
        cpeScanService.init(cpeItemsSynchronizationService, microsoftKbSynchronizationService);

        discoveryServices.setCpeScanService(cpeScanService);
        discoveryServices.setDbapiHandler(dbapiHandler);
        discoveryServices.setScanApiHandler(scanApiHandler);
        discoveryServices.setCpeItemSynchronizationService(cpeItemsSynchronizationService);
        discoveryServices.setDeletedCpeItemsSynchronizationService(deletedCpeItemsSynchronizationService);
//        discoveryServices.setScanWebSocketHandler(scanWebSocketHandler);
    }

    /*Test unitaire insuffisant */
    //TODO : A tester en test intégration
    @BeforeEach
    //@Test
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
        boolean reussie = discoveryServices.init();
        Json status = discoveryServices.getStatus();
        //Then
        assert(reussie);
        assert(status.toString().equals(statusExpected.toString()));
    }

    //@Test
    void terminateTest(){
        //when
        boolean terminate = discoveryServices.terminate();
        //then
        assertFalse(terminate);
    }

    //@Test
    void getAllCpesTest(){
        //when
        List<CpeItem> result = discoveryServices.getAllCpes();
        System.out.println("Les CPEs : "+result.toString());
        //then
        assertNotNull(result);
    }

    //@Test
    void startScanTest(){
        //Given
        List<String> cpeNames = new ArrayList<>();
        for(CpeItem cpeItem : discoveryServices.getAllCpes()){
            cpeNames.add(cpeItem.getDeviceId());
        }
        //When
        JsonApiResponse rep = discoveryServices.startScan(cpeNames);
        System.out.println("CPE names : "+cpeNames.toString());
        //Then
        assert(rep.toString().equals(JsonApiResponse.success().toString()));
    }
}