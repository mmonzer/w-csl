package main.demo;

import com.csl.intercom.broker.CSLInterModuleCommunicationManager;
import com.csl.intercom.broker.MosquittoConfig;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.csl.util.JCmd;
import com.ucsl.json.Json;

/*
 *
 *   exec externe
 *
 */
public class Test04InterProcessCommunicationB {

    ApiCommands api = new ApiCommandsFactory().createApiCommands("essai");
    CSLInterModuleCommunicationManager imcm = new CSLInterModuleCommunicationManager("IDS2", new MosquittoConfig());

    public static void main(String[] args) {

        Test04InterProcessCommunicationB test = new Test04InterProcessCommunicationB();


        // Execute commande externe (module A)

        Json r = test.imcm.executeCommand("essai", Json.object().set(JCmd.CMD, "test").set(JCmd.PARAMETERS, Json.object().set("x", 10)));

        System.out.println("Result (extern):" + r);
    }
}
