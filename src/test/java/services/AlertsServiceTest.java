package services;

import com.csl.alert.CSLAlertManager;
import com.csl.core.CSLContext;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.json.Json;
import main.services.AlertsService;
import org.junit.jupiter.api.BeforeEach;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.mockito.Mockito.*;

public class AlertsServiceTest {

    private AlertsService alertsService;

    @Mock
    private CSLContext cslContext;

    @Mock
    private CSLAlertManager cslAlertManager;

    @BeforeEach
    public void setUp(){
        MockitoAnnotations.openMocks(this);
        when(cslContext.getCSLAlertManager()).thenReturn(cslAlertManager);
        alertsService = new AlertsService();
    }

    //@Test
    public void addCmdIsOkTest(){
        //Given
        String name = "get-alert_list";
        //When
        String result = alertsService.addCmd(name, params -> CSLContext.getInstance().getCSLAlertManager().getListOfCurrentAlertsAsJson(), new JsonCmdHelp().setDesc("").setStatus(JsonCmdHelp.STATUS_OK));
        //Then
        assert(result.equals("ok"));
    }

    //@Test
    public void addCmdAlreadyExistsTest(){
        //Given
        String name = "get-alert_list";
        String resultCmd1 = alertsService.addCmd(name, params -> CSLContext.getInstance().getCSLAlertManager().getListOfCurrentAlertsAsJson(), new JsonCmdHelp().setDesc("").setStatus(JsonCmdHelp.STATUS_OK));
        //When
        String resultCmd2 = alertsService.addCmd(name, params -> CSLContext.getInstance().getCSLAlertManager().getListOfCurrentAlertsAsJson(), new JsonCmdHelp().setDesc("").setStatus(JsonCmdHelp.STATUS_OK));
        //Then
        assert(resultCmd2.equals("Command with this name already registered :" + name));
    }

    //@Test
    public void initTest(){
        //Given
        Json jConfig = Json.object();
        String cslDir = "testDir";
        List<String> listOfCommandsExpected = List.of("set_acked", "get_list_inactive_alerts", "get_alerts_list",
                "set_masked", "set_added_to_model", "get_number_active_alerts_by_level", "get_list_all_alerts",
                "get_list_acked_alerts", "clear_list_of_all_alerts", "get_list_masked_alerts", "stats", "get_list_active_alerts",
                "test_alert0", "test_alert1", "test_alert2", "op_alert", "get_list_added_to_model_alerts");
        //When
        boolean reussite = alertsService.init();
        List<String> listOfCommands = alertsService.getApiCommands().getListOfCommands();
        //then
        assert(reussite);
        assert(listOfCommands.equals(listOfCommandsExpected));
    }

}
