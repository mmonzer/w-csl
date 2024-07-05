package main.help;

import java.util.List;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;






public class Help52Alerts {

	HelpGenerator hg ;
	
	
	public Json exec(String cmd, Json params) {
		
		return hg.addExample(cmd,  params);
		
	}
	
	public Json exec(String cmd) {
		return exec(cmd, Json.object());
	}
	
	
	
	public void init() {
		
		String apiname="alerts";
		hg = new HelpGenerator(apiname, "localhost", 8000);
		hg.loadHelp();
		
		hg.setReplaceExisting(true);
		
		
	
	}
	
	

	public void finish() {
		
		
		
		hg.saveHelp();
	}
	
	public Json test1() {

		Json result=Json.object();


		Json p=Json.object();
		
		exec("test_alert1", p);
		
		
		
		
		return result;
	}

	
	public Json test_stats() {

		Json result=Json.object();


		Json p=Json.object();
		
		exec("stats", p);
		
		
		
		
		return result;
	}

	public Json test_get_list_active() {

		Json p=Json.object();
		return exec("get_list_active_alerts", p);
		
	}


	public Json test_get_list_acked() {

		Json p=Json.object();
		return exec("get_list_acked_alerts", p);
		
	}
	
	public Json test_get_list_masked() {

		Json p=Json.object();
		return exec("get_list_masked_alerts", p);
		
	}
	
	public Json test_get_list_added() {

		Json p=Json.object();
		return exec("get_list_added_to_model_alerts", p);
		
	}
	
	public Json test_get_list_inactive() {

		Json p=Json.object();
		return exec("get_list_inactive_alerts", p);
		
	}
	
	public Json test_clear() {

		Json p=Json.object();
		return exec("clear_list_of_all_alerts", p);
		
	}
	

	public Json test_get_list_all() {

		Json p=Json.object();
		return exec("get_list_all_alerts", p);
		
	}
	
	public String getUuid(int n) {

		
		Json p=Json.object();
		
		Json r=exec("get_list_all_alerts", p);
		
		if (n<0) return "";
		if (r.isArray()) {
			List<Json> z = r.asJsonList();
			if (n>=z.size()) return "";
			return z.get(n).get("alert_id").asString();
		}
		else return "";
	}
	
	public Json test_set_masked() {

		Json result=Json.object();


		Json p=Json.object();
		
		String uuid=getUuid(0);
		p.set("alert_id",uuid);
		p.set("value", true);
		p.set("time_for_end_of_mask", System.currentTimeMillis()+1000*3600);
		
		return exec("set_masked", p);
		
		
		
		
	}


	public Json test_unset_masked() {

		Json result=Json.object();


		Json p=Json.object();
		
		String uuid=getUuid(0);
		p.set("alert_id",uuid);
		p.set("value", false);
		
		return exec("set_masked", p);
		
		
		
		
	}
	
	

	public Json test_set_acked() {

		Json result=Json.object();


		Json p=Json.object();
		
		String uuid=getUuid(1);
		p.set("alert_id",uuid);
		p.set("value", true);
		
		return exec("set_acked", p);
		
		
		
		
	}


	public Json test_unset_acked() {

		Json result=Json.object();


		Json p=Json.object();
		
		String uuid=getUuid(1);
		p.set("alert_id",uuid);
		p.set("value", false);
		
		return exec("set_acked", p);
		
		
		
		
	}
	
	
	public Json test_set_add_to_model() {

		Json result=Json.object();


		Json p=Json.object();
		
		String uuid=getUuid(2);
		p.set("alert_id",uuid);
		p.set("value", true);
			
		return exec("set_added_to_model", p);
		
		
		
		
	}


	public Json test_unset_add_to_model() {

		Json result=Json.object();


		Json p=Json.object();
		
		String uuid=getUuid(2);
		p.set("alert_id",uuid);
		p.set("value", false);
		
		return exec("set_added_to_model", p);
		
		
		
		
	}
	
	
	public void  print(Json r) {
		
		boolean err=false;
		if (r.isObject()) {
			if (r.has("error")) err=true;
		}
		
		if (err)
		{
			System.err.println(JsonUtil.prettyPrint(r));
		}
		else {
			System.out.println(JsonUtil.prettyPrint(r));
			
		}
	}
	public void run() {
		
		Json jparams= Json.object();
		
		init();
		
		Json r=exec("help", jparams);
		System.out.println(JsonUtil.prettyPrint(r));

		
		r=test1();
		
		System.out.println("TEST Stats");
		r= test_stats();
		System.out.println(JsonUtil.prettyPrint(r));

		r= test_get_list_active();
		System.out.println(JsonUtil.prettyPrint(r));
		
		
		
		System.out.println("set masked");
		r=test_set_masked();
		print((r));
		
		System.out.println("TEST List of masked");
		print(test_get_list_masked());
		
		
		
		System.out.println("set acked");
		r=test_set_acked();
		System.out.println(JsonUtil.prettyPrint(r));
		
		System.out.println("TEST List of acked");
		print(test_get_list_acked());
		
		
		System.out.println("set add to model");
		r=test_set_add_to_model();
		System.out.println(JsonUtil.prettyPrint(r));
		
		System.out.println("TEST List of added to model");
		print(test_get_list_added());
		
	
		
		System.out.println("TEST List of active");
		print(test_get_list_active());
		
		
		System.out.println("TEST List of inactive");
		print(test_get_list_inactive());
	
		
		System.out.println("unset masked");
		r=test_unset_masked();
		print(r);
		
		System.out.println("TEST List of masked (empty)");
		print(test_get_list_masked());
		
		//r= test_get_list_all();
		
				
		finish();
	}

	
	
	
	
	public static void main(String[] args) {
		
		
		
		Help52Alerts h = new Help52Alerts();
		
		h.run();
		
		
	}
	
	/*
	 * 
	 * 
	 *    exec -c  name param
	 *    
	 *    exec -f name_of file
	 *    
	 *    
	 *    file
	 *    
	 *    cmd  {
	 */

}
