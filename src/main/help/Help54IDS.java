package main.help;

import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;






public class Help54IDS {

	HelpGenerator hg ;
	
	
	public Json exec(String cmd, Json params) {
		
		return hg.addExample(cmd,  params);
		
	}
	
	public Json exec(String cmd) {
		return exec(cmd, Json.object());
	}
	
	
	
	public void init() {
		
		String apiname="ids";
		hg = new HelpGenerator(apiname, "localhost", 8000);
		hg.loadHelp();
		
		hg.setReplaceExisting(true);
		
		
	
	}
	
	

	public void finish() {
		
		
		
		hg.saveHelp();
	}
	
	

	
	public Json test_stats_links() {

		Json result=Json.object();


		Json p=Json.object();
		
		exec("stats_links", p);
		
		
		
		
		return result;
	}


	public Json test_stats_network() {

		Json result=Json.object();


		Json p=Json.object();
		
		exec("stats_network", p);
		
		
		
		
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
		r= test_stats_links();
		r= test_stats_network();
		
		
		//r= test_get_list_all();
		
				
		finish();
	}

	
	
	
	
	public static void main(String[] args) {
		
		
		
		Help54IDS h = new Help54IDS();
		
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
