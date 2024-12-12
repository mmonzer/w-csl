package services;

import com.csl.intercom.jsoncmd.ApiCommands;
import com.ucsl.json.Json;
import main.services.MonitorService;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

public class MonitorServiceTest {
    private MonitorService monitorService;

    @BeforeEach
    public void setUp(){
        monitorService = new MonitorService();
    }
    //@Test
    public void initTest(){
        //Given
        Json jConfig = Json.object();
        String cslDir = "cslDir";
        List<String> listOfCommandsExpected = List.of("stats_taps", "stats_devices");
        //When
        boolean reussie = monitorService.init();
        List<String> listOfCommands = ((ApiCommands) monitorService.getApiCommands()).getListOfCommands();
        //Then
        assert(reussie);
        assert(listOfCommandsExpected.equals(listOfCommands));
    }
}
