package com.ucsl.json;

public class JsonTest {

	
	public static void main(String[] args) {
		
		
		JsonSelector js= new JsonSelector().parse("a.*.hjh.*");
		System.out.println(js);
		System.out.println(js.toStringWithDetails());
		
		
		Json x=Json.object();
		
		
		try {
			JsonUtil.setElement(x, "a.*.hjh.*",Json.make("ok"),true);
		} catch (JsonException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(x);

		x=Json.object();
		Json jj= Json.array();
		
		x.set("a",  Json.object());
		
		x.get("a").set("soft",jj );
		for (int i=1;i<10; i++) jj.add("soft #"+i);
		
		System.out.println(x);
		
		try {
			JsonUtil.setElement(x, "a.soft.6",Json.make("ok"));
		} catch (JsonException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		try {
			System.out.println(JsonUtil.getElement(x, "a.soft"));
			
			JsonUtil.setElement(x, "a.soft.*", Json.make("new value"));
		} catch (JsonException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(x);
		
		/*Json j= Json.array();
		j.add("test");
		Json z= Json.make(true);
		System.out.println(z+"  "+z.isBoolean());
		
		
		System.out.println(j.at(0));*/
	}
}
