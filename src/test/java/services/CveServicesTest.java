import com.ucsl.json.Json;
import main.services.CveServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CveServicesTest {
    private CveServices cveServices;

    @BeforeEach
    public void setUp(){
        cveServices = new CveServices();
    }
    @Test
    public void initTest(){
        //Given
        Json jConfig = Json.object();
        String cslDir = "cslDir";
        //When
        boolean reussie = cveServices.init(jConfig, cslDir);
        //Then
        assert(reussie);
    }
}
