package main.help;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;






public class Help50DbDevices {

	HelpGenerator hg ;
	
	
	public Json exec(String cmd, Json params) {
		params.set("user", "user1");
		return hg.addExample(cmd,  params);
		
	}
	
	public Json exec(String cmd) {
		return exec(cmd, Json.object());
	}
	
	
	
	public void init() {
		
		String apiname="devdb";
		hg = new HelpGenerator(apiname, "localhost", 8000);
		hg.loadHelp();
		
		hg.setReplaceExisting(true);
		
		
	
	}
	
	

	public void finish() {
		
		
		
		hg.saveHelp();
	}
	
	
	
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
		
		System.out.println("TEST ADD LINK");
		
		r= test_del_device();
		System.out.println(JsonUtil.prettyPrint(r));
		r= test_get_devices();
		System.out.println(JsonUtil.prettyPrint(r));


		finish();
	}

	public static void main(String[] args) {
		
		
		
		Help50DbDevices h = new Help50DbDevices();
		
		h.run();
		
		
	}

}
