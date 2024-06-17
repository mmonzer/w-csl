import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.intercom.cslscan.ScanUtils;
import com.csl.intercom.dbapi.DbapiHandler;
import com.csl.intercom.services.CpeScanService;
import com.csl.intercom.services.DataSynchronizationService;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.json.Json;
import main.services.DiscoveryServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static main.extensions.Utils.execCmd;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DiscoveryServicesTest {

    private DiscoveryServices discoveryServices;
    @Mock
    private CpeScanService cpeScanService;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        discoveryServices = new DiscoveryServices();
        cpeScanService = Mockito.mock(CpeScanService.class);
        discoveryServices.setCpeScanService(cpeScanService);
        Mockito.doNothing().when(cpeScanService).init(any(), any());
    }
    /*Test unitaire insuffisant */
    //TODO : A tester en test intégration
    @Test
    void initTest(){
        //Given
        Json jConfig = Json.object();
        String cslDir = "cslDir";
        //When
        boolean reussie = discoveryServices.init(jConfig, cslDir);
        Json status = discoveryServices.getStatus();
        System.out.println(status.toString());
        //Then
        assertThrows(NullPointerException.class, ()-> {
            discoveryServices.startScan(List.of());
        });
        assert(reussie);
    }
}