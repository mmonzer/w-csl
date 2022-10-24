package main.help;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;






public class Help55Cve {

	HelpGenerator hg ;
	
	
	public Json exec(String cmd, Json params) {
		
		return hg.addExample(cmd,  params);
		
	}
	
	public Json exec(String cmd) {
		return exec(cmd, Json.object());
	}
	
	
	
	public void init() {
		
		String apiname="cve";
		hg = new HelpGenerator(apiname, "localhost", 8000);
		hg.loadHelp();
		
		hg.setReplaceExisting(true);
		
		
	
	}
	
	

	public void finish() {
		
		
		
		hg.saveHelp();
	}
	
	

	
	public Json test_get() {

		Json result=Json.object();


		Json p=Json.object();
		
		return exec("get_cve", p);
		
		
		
	
	}


	
	public Json test_stats() {

		Json result=Json.object();


		Json p=Json.object();
		
		return exec("stats", p);
		
		
		
	
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

		
		test_stats();
		
		
		System.out.println("TEST get cve");
		r= test_get();
		System.out.println(r);
		//r= test_get_list_all();
		
				
		finish();
	}

	
	
	
	
	public static void main(String[] args) {
		
		
		
		Help55Cve h = new Help55Cve();
		
		h.run();
		
		
	}
	
	

}
