import main.services.CpeServices;
import org.junit.jupiter.api.BeforeEach;

public class CpeServicesTest {
    private CpeServices cpeServices;

    @BeforeEach
    public void setUp(){
        cpeServices = new CpeServices();
    }
}
