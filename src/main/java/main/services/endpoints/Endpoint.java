package main.services.endpoints;

import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.ucsl.interfaces.IJsonCmdHelp;

/**
 * Generic interface for endpoints enums.
 */
public interface Endpoint {
    /**
     * Gives the cmd that will be received on json
     *
     * @return cmd that will be received on json
     */
    String cmd();

    /**
     * Gives the help that will be presented on api-help
     *
     * @return help that will be presented on api-help
     */
    JsonCmdHelp help();

    /**
     * General method to create an empty deprecated help.
     *
     * @return an empty deprecated help.
     */
    static IJsonCmdHelp EMPTY_DEPRECATED() {
        return new JsonCmdHelp().setDesc("DEPRECATED");
    }

    /**
     * General method to create an empty help.
     *
     * @return an empty help.
     */
    static JsonCmdHelp EMPTY() {
        return new JsonCmdHelp();
    }
}
