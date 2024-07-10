import com.csl.intercom.jsoncmd.ApiCommands;
import com.ucsl.json.Json;
import main.services.StatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class StatusServiceTest {
    private StatusService statusService;

    @BeforeEach
    public void setUp(){
        statusService = new StatusService();
    }
    @Test
    public void initTest(){
        //Given
        Json jConfig = Json.object();
        String cslDir = "cslDir";
        List<String> listOfCommandsExpected = List.of("get_status", "set_should_send_notifications");
        //When
        boolean reussie = statusService.init(jConfig, cslDir);
        List<String> listOfCommands = ((ApiCommands) statusService.getApiCommands()).getListOfCommands();
        //Then
        assert(reussie);
        assert(listOfCommandsExpected.equals(listOfCommands));
    }
}
