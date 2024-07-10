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
public class Test51ApiDevDb {


	//IApiCommands api= new ApiCommands("essai");
	IApiCommands api= new ApiCommandsFactory().createApiCommands("essai");


	CSLInterModuleCommunicationManager imcm = new CSLInterModuleCommunicationManager("DB", new MosquittoConfig());

	public void clearDatabaseDb() {

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
	

	public Json test_get_link_between(){

		Json result=Json.object();

		Json p=Json.object();
		p.set("ip","10.0.208.100");
		
		p.set("id2","10.0.208.110");
		p.set("op","GET_LINK_BETWEEN");
		
		
		return exec("op",p);
	}
	
	
	public Json test_add_link(){

		Json result=Json.object();

		Json p=Json.object();
		p.set("ip","1.1.1.1");
		
		p.set("id2","1.1.1.2");
		
		Json props= Json.object();
		props.set("testp", "blue");
		p.set("props", props);
		
		p.set("op","ADD_LINK");
		
		
		return exec("op",p);
	}
	
	
	public Json test_get_link_between2(){

		Json result=Json.object();

		Json p=Json.object();
		p.set("ip","1.1.1.1");
		
		p.set("id2","1.1.1.2");
p.set("op","GET_LINK_BETWEEN");
		
		
		return exec("op",p);
	}
	
	
	public void run() {
		
		Json jparams= Json.object();
		
		//clearDatabaseDb();
		
	/*	Json r=exec("help", jparams);
		System.out.println(JsonUtil.prettyPrint(r));

		
		System.out.println("TEST ADD");
		r= test_add_device();
		System.out.println(JsonUtil.prettyPrint(r));

		
		r= test_get_devices();
		System.out.println(JsonUtil.prettyPrint(r));*/
		
		
		Json r= test_add_link();
		System.out.println(JsonUtil.prettyPrint(r));
		
		r= test_get_link_between2();
		System.out.println(JsonUtil.prettyPrint(r));
		

		
//		System.out.println("TEST UPDATE");
//		
//		r= test_update_device();
//		System.out.println(JsonUtil.prettyPrint(r));
//
//		
//		r= test_get_devices();
//		System.out.println(JsonUtil.prettyPrint(r));
//
//
//		
//		System.out.println("TEST GET");
//		
//		r= test_get_device();
//		System.out.println(JsonUtil.prettyPrint(r));
//
//	
//
//		System.out.println("TEST DELETE");
//		
//		r= test_del_device();
//		System.out.println(JsonUtil.prettyPrint(r));
//		r= test_get_devices();
//		System.out.println(JsonUtil.prettyPrint(r));
//
//		
//		System.out.println("TEST DELETE");
//		
//		r= test_del_device();
//		System.out.println(JsonUtil.prettyPrint(r));
//		r= test_get_devices();
//		System.out.println(JsonUtil.prettyPrint(r));
//
//		
//		
//		
	}
	
	
	public static void main(String[] args) {



		Test51ApiDevDb test= new Test51ApiDevDb();

		
		test.run();
		

		
		System.exit(0);
	}
}
