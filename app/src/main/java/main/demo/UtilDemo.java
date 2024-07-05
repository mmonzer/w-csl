package main.demo;

import com.ucsl.json.Json;

public class UtilDemo {

	
	static public void printModel(Json model) {
		
		
		for (Json d:model.asJsonList() ) {
			System.out.println("ip:"+d.get("ip")+" name:"+d.get("name")+"  risk="+d.get("risk")+" country="+d.get("country"));
			Json links = d.get("links");
			if (links!=null)
			{
				for (Json l:d.get("links").asJsonList() ) {
					System.out.println("   "+l);
				}
			}
			
		}
	}
}
