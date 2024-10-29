package services;

import com.ucsl.json.Json;
import main.services.TapsServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class TapsServicesTest {
    private TapsServices tapsServices;

    @BeforeEach
    public void setUp(){
        tapsServices = new TapsServices();
    }
    //@Test
    public void sendIncludesIsOk(){
        //Given
        Json jConfig = Json.object();
        String name = "name";
        initTest();
        //When - Then
        assertDoesNotThrow(()->TapsServices.sendIncludes(name));

    }
    /*Test unitaire insuffisant */
    //TODO : A tester en test intégration
    //@Test
    public void initTest(){
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
        boolean reussie = tapsServices.init();

        //Then
        assert(reussie);
    }


}
