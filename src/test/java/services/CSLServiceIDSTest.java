import com.csl.intercom.jsoncmd.ApiCommands;
import com.ucsl.interfaces.IIDSOperationManager;
import com.ucsl.json.Json;
import main.services.CSLServiceIDS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CSLServiceIDSTest {

    private CSLServiceIDS cslServiceIDS;
    @Mock
    private IIDSOperationManager iidsOperationManager;

    @BeforeEach
    public void setUp(){
        MockitoAnnotations.openMocks(this);
        when(iidsOperationManager.exec(any())).thenReturn(Json.object());
        cslServiceIDS = new CSLServiceIDS();
    }
    @Test
    public void initTest(){
        //Given
        Json jconfig = Json.object();
        String cslDir = "cslDir";
        int numberOfCommandsExpected = 42;
        //When
        boolean result = cslServiceIDS.init(jconfig, cslDir);
        List<String> listOfCommands = ((ApiCommands) cslServiceIDS.getApiCommands()).getListOfCommands();
        //Then
        assert(result);
        assert(listOfCommands.size() == numberOfCommandsExpected);
    }
}
