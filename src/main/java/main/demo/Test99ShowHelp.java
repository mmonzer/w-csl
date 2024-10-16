package main.demo;

import com.csl.intercom.broker.CSLInterModuleCommunicationManager;
import com.csl.intercom.broker.MosquittoConfig;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.csl.util.JCmd;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

/*
 *
 *   exec externe
 *
 */
public class Test99ShowHelp {

    ApiCommands api = new ApiCommandsFactory().createApiCommands("essai");

    CSLInterModuleCommunicationManager imcm = new CSLInterModuleCommunicationManager("DB", new MosquittoConfig());

    public static void main(String[] args) {
        Test99ShowHelp test = new Test99ShowHelp();

        // Execute commande externe (module A)

        Json jparams = Json.object();
        jparams.set("user", "user1");

        Json r = test.imcm.executeCommand("devdb", Json.object().set(JCmd.CMD, "help").set(JCmd.PARAMETERS, jparams));
        System.out.println(JsonUtil.prettyPrint(r));

        r = test.imcm.executeCommand("ids", Json.object().set(JCmd.CMD, "help").set(JCmd.PARAMETERS, jparams));
        System.out.println(JsonUtil.prettyPrint(r));

        r = test.imcm.executeCommand("cve", Json.object().set(JCmd.CMD, "help").set(JCmd.PARAMETERS, jparams));
        System.out.println(JsonUtil.prettyPrint(r));

        r = test.imcm.executeCommand("cpe", Json.object().set(JCmd.CMD, "help").set(JCmd.PARAMETERS, jparams));
        System.out.println(JsonUtil.prettyPrint(r));

        r = test.imcm.executeCommand("alerts", Json.object().set(JCmd.CMD, "help").set(JCmd.PARAMETERS, jparams));
        System.out.println(JsonUtil.prettyPrint(r));

        System.exit(0);
    }
}
