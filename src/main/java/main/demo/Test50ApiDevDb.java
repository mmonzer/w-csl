package main.demo;

import com.csl.intercom.broker.CSLInterModuleCommunicationManager;
import com.csl.intercom.broker.MosquittoConfig;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.ucsl.interfaces.IApiCommands;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;


/*
 * 
 *   exec externe
 * 
 */
public class Test50ApiDevDb {

	ApiCommands api= new ApiCommandsFactory().createApiCommands("essai");

	CSLInterModuleCommunicationManager imcm = new CSLInterModuleCommunicationManager("DB", new MosquittoConfig());

	public void init() {

		exec("clear");


	}
	
	public Json exec(String cmd, Json params) {
		params.set("user", "user1");

		Json r=imcm.executeCommand("devdb", Json.object().set("cmd", cmd).set("params", params));


		return r;
		
	}
	public Json exec(String cmd) {
		return exec(cmd, Json.object());
	}

	/*
	 *
	 *
	 */

	public Json test_add_device() {

		Json result=Json.object();


		Json p=Json.object();
		p.set("props", Json.object().set("brand","siemens"));
		p.set("ip","1.1.1.1");
		
		exec("add_device", p);
		
		
		p=Json.object();
		p.set("props", Json.object().set("brand","schneider"));
		p.set("ip","1.1.1.2");
		
		exec("add_device", p);
		
		return result;
	}


	public Json test_get_device() {

		Json result=Json.object();

		Json p=Json.object();
		p.set("ip","1.1.1.1");
		
		return exec("get_device", p);

		
	}



	public Json test_update_device() {

		Json result=Json.object();

		Json p=Json.object();
		p.set("ip","1.1.1.1");
		p.set("props", Json.object().set("brand","yokogawa").set("impact level", 4));
		
		
		return exec("update_device", p);


	}



	public Json test_del_device() {

		Json result=Json.object();

		Json p=Json.object();
		p.set("ip","1.1.1.1");
		
		return exec("del_device", p);


	
	}


	public Json test_get_devices() {

		Json result=Json.object();


		
		return exec("get_devices");
	}
	
	
	public void run() {
		
		Json jparams= Json.object();
		
		init();
		
		Json r=exec("help", jparams);
		System.out.println(JsonUtil.prettyPrint(r));

		
		System.out.println("TEST ADD");
		r= test_add_device();
		System.out.println(JsonUtil.prettyPrint(r));

		
		r= test_get_devices();
		System.out.println(JsonUtil.prettyPrint(r));

		
		System.out.println("TEST UPDATE");
		
		r= test_update_device();
		System.out.println(JsonUtil.prettyPrint(r));

		
		r= test_get_devices();
		System.out.println(JsonUtil.prettyPrint(r));


		
		System.out.println("TEST GET");
		
		r= test_get_device();
		System.out.println(JsonUtil.prettyPrint(r));

	

		System.out.println("TEST DELETE");
		
		r= test_del_device();
		System.out.println(JsonUtil.prettyPrint(r));
		r= test_get_devices();
		System.out.println(JsonUtil.prettyPrint(r));

		
		System.out.println("TEST DELETE");
		
		r= test_del_device();
		System.out.println(JsonUtil.prettyPrint(r));
		r= test_get_devices();
		System.out.println(JsonUtil.prettyPrint(r));

		
		
		
	}
	
	
	public static void main(String[] args) {



		Test50ApiDevDb test= new Test50ApiDevDb();

		
		test.run();
		

		
		System.exit(0);
	}
}
