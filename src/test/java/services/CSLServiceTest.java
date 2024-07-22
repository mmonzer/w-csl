import com.csl.core.CSLContext;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.json.Json;
import main.services.CSLService;
import main.services.CpeServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class CSLServiceTest {
    /*
        SERVICE CSLService  n'est pas utilisé !
     */
    private CSLService cslService;

    @BeforeEach
    public void setUp(){
        cslService = new CSLService();
    }
    @Test
    public void addCmdIsOkTest(){
        //Given
        String name = "test";
        //When
        String result = cslService.addCmd(name, new IJsonCmd(){
            @Override
            public Json exec(Json params) {
                return CSLContext.instance.getCSLAlertManager().getListOfCurrentAlertsAsJson();
            }

        }, new JsonCmdHelp().setDesc("").setStatus(JsonCmdHelp.STATUS_OK));
        //Then
        assert(result.equals("ok"));
    }

    @Test
    public void addCmdAlreadyExistsTest(){
        //Given
        String name = "test";
            String resultCmd1 = cslService.addCmd(name, new IJsonCmd(){
            @Override
            public Json exec(Json params) {
                return CSLContext.instance.getCSLAlertManager().getListOfCurrentAlertsAsJson();
            }

        }, new JsonCmdHelp().setDesc("").setStatus(JsonCmdHelp.STATUS_OK));
        //When
        String resultCmd2 = cslService.addCmd(name, new IJsonCmd(){
            @Override
            public Json exec(Json params) {
                return CSLContext.instance.getCSLAlertManager().getListOfCurrentAlertsAsJson();
            }

        }, new JsonCmdHelp().setDesc("").setStatus(JsonCmdHelp.STATUS_OK));
        //Then
        assert(resultCmd2.equals("Command with this name already registered :" + name));
    }

    @Test
    public void initTest(){
        //Given
        Json jConfig = Json.object();
        String cslDir = "cslDir";
        List<String> listOfCommandsExpected = List.of("test");
        //When
        boolean result = cslService.init(jConfig, cslDir);
        List<String> listOfCommands = ((ApiCommands) cslService.getApiCommands()).getListOfCommands();
        //Then
        assert(result);
        assert(listOfCommands.equals(listOfCommandsExpected));

    }
}
