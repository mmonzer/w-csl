package services;

import com.ucsl.json.Json;
import main.services.SuricataServices;
import org.junit.jupiter.api.BeforeEach;

public class SuricataServicesTest {

    /*Test unitaire insuffisant */
    //TODO : A tester en test intégration
    @BeforeEach
    public void setUp(){
        SuricataServices suricataServices = new SuricataServices();
    }

    //@Test
    void initTest(){
        //Given
        Json jConfig = Json.object();
        String cslDir = "cslDir";
        Json tmp = Json.object();

        tmp.set("knowHostFilePath", "filePathTest");
        tmp.set("localIpAddr", "ipTest");
        tmp.set("localPort", "portTest");
        tmp.set("name", "usernameTest");

        jConfig.set("knowHostFilePath", tmp.get("knowHostFilePath"));
        jConfig.set("localIpAddr", tmp.get("localIpAddr"));
        jConfig.set("localPort", tmp.get("localPort"));
        jConfig.set("name", tmp.get("name"));
        //When
        //boolean reussie = suricataServices.init(jConfig, cslDir);
        //Then
        //assert(reussie);
    }
}
