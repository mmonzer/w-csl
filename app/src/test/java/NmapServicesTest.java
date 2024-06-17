import com.ucsl.json.Json;
import main.services.NmapServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class NmapServicesTest {
    private NmapServices nmapServices;

    @BeforeEach
    void setUp() {
        nmapServices = new NmapServices();
    }

    /*Test unitaire insuffisant */
    //TODO : A tester en test intégration
    @Test
    void initTest(){
        //Given
        Json jConfig = Json.object();
        String cslDir = "cslDir";

        Json tmp = Json.object();
        tmp.set("debug_mode", "false");
        tmp.set("log_mode", "false");
        tmp.set("log_dir", "testLogDir");
        tmp.set("debug_dir", "testDebugDir");

        jConfig.set("debug_mode", tmp.get("debug_mode"));
        jConfig.set("log_mode", tmp.get("log_mode"));
        jConfig.set("log_dir", tmp.get("log_dir"));
        jConfig.set("debug_dir", tmp.get("debug_dir"));

        //When
        boolean reussie = nmapServices.init(jConfig, cslDir);
        //Then
        assert(reussie);
    }
    @Test
    void scanDeviceIsOkTest() {
        //Given
        Json params = Json.object();
        params.set("tap", "tapTest");

        Json jConfig = Json.object();

        Json tmp = Json.object();
        tmp.set("debug_mode", "false");
        tmp.set("log_mode", "false");
        tmp.set("log_dir", "testLogDir");
        tmp.set("debug_dir", "testDebugDir");

        jConfig.set("debug_mode", tmp.get("debug_mode"));
        jConfig.set("log_mode", tmp.get("log_mode"));
        jConfig.set("log_dir", tmp.get("log_dir"));
        jConfig.set("debug_dir", tmp.get("debug_dir"));

        initTest();
        //When - Then
        assertThrows(NullPointerException.class, ()->nmapServices.scanDevice(params, jConfig));


    }
}
