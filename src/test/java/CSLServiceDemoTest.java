import com.csl.intercom.jsoncmd.ApiCommands;
import com.ucsl.json.Json;
import main.services.CSLServiceDemo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class CSLServiceDemoTest {

    private CSLServiceDemo cslServiceDemo;

    @BeforeEach
    public void setUp(){
        cslServiceDemo = new CSLServiceDemo();
    }
    @Test
    public void initTest(){
        //Given
        Json jConfig = Json.object();
        String cslDir = "cslDir";
        List<String> listOfCommandsExpected = List.of("demo_cmd");
        //When
        boolean reussie = cslServiceDemo.init(jConfig, cslDir);
        List<String> listOfCommands = ((ApiCommands) cslServiceDemo.getApiCommands()).getListOfCommands();
        //Then
        assert(reussie);
        assert(listOfCommands.equals(listOfCommandsExpected));
    }
}
