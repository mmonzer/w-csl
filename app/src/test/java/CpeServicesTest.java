import com.csl.core.CSLContext;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.json.Json;
import main.services.CpeServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class CpeServicesTest {

    private CpeServices cpeServices;

    @BeforeEach
    public void setUp(){
        cpeServices = new CpeServices();
    }
    @Test
    public void addCmdIsOkTest(){
        //Given
        String name = "get-alert_list";
        //When
        String result = cpeServices.addCmd(name, new IJsonCmd(){
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
        String name = "get-alert_list";
        String resultCmd1 = cpeServices.addCmd(name, new IJsonCmd(){
            @Override
            public Json exec(Json params) {
                return CSLContext.instance.getCSLAlertManager().getListOfCurrentAlertsAsJson();
            }

        }, new JsonCmdHelp().setDesc("").setStatus(JsonCmdHelp.STATUS_OK));
        //When
        String resultCmd2 = cpeServices.addCmd(name, new IJsonCmd(){
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
        //ReadDictionnaryFromJson or XML throws ParserConfigurationException, SAXException, IOException
        //
        //Given
        Json jConfig = Json.object();
        String cslDir = "test";
        Json dictionnaryPath = Json.object();

        String DEFAULT_CONFIG_FILE = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "cslconf" + File.separator + "cpe" + File.separator + "CpeTree.json";

        jConfig.set("dictionaryPath", DEFAULT_CONFIG_FILE);
        jConfig.set("dictionnaryPath", dictionnaryPath.get("dictionaryPath"));

        List<String> listOfCommandsExcepted = List.of("getVersion", "getProduct", "getPrefix", "getVendor");

        //When
        boolean result = cpeServices.init(jConfig, cslDir);
        List<String> listOfCommands = ((ApiCommands) cpeServices.getApiCommands()).getListOfCommands();
        System.out.println(listOfCommands.toString());

        //Then
        assert(result);
        assert (listOfCommands.equals(listOfCommandsExcepted));


    }
    /*@Test
    public void initTestThrowException(){
        //ReadDictionnaryFromJson or XML throws ParserConfigurationException, SAXException, IOException
        //
        //Given
        Json jConfig = Json.object();
        String cslDir = "test";
        Json dictionnaryPath = Json.object();

        jConfig.set("dictionaryPath", "unknown/Path.json");
        jConfig.set("dictionnaryPath", dictionnaryPath.get("dictionaryPath"));

        //When - Then
        assertThrows(IOException.class, ()-> cpeServices.init(jConfig, cslDir))

    ;}*/
}
