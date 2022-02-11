package main.help;

import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;






public class Help53Monitor {

	HelpGenerator hg ;
	
	
	public Json exec(String cmd, Json params) {
		
		return hg.addExample(cmd,  params);
		
	}
	
	public Json exec(String cmd) {
		return exec(cmd, Json.object());
	}
	
	
	
	public void init() {
		
		String apiname="monitor";
		hg = new HelpGenerator(apiname, "localhost", 8000);
		hg.loadHelp();
		
		hg.setReplaceExisting(true);
		
		
	
	}
	
	

	public void finish() {
		
		
		
		hg.saveHelp();
	}
	
	

	
	public Json test_stats_devices() {

		Json result=Json.object();


		Json p=Json.object();
		
		exec("stats_devices", p);
		
		
		
		
		return result;
	}


	public Json test_stats_taps() {

		Json result=Json.object();


		Json p=Json.object();
		
		exec("stats_taps", p);
		
		
		
		
		return result;
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

		
		
		System.out.println("TEST Stats");
		r= test_stats_devices();
		
		test_stats_taps();
		//r= test_get_list_all();
		
				
		finish();
	}

	
	
	
	
	public static void main(String[] args) {
		
		
		
		Help53Monitor h = new Help53Monitor();
		
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
