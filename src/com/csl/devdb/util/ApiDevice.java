package com.csl.devdb.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.csl.devdb.DevicesDB;
import com.csl.devdb.DevicesDBException;
import com.xcsl.json.Json;

public class ApiDevice {

	public String ERROR="ERROR";
	public String UUID="uuid";
	public String IP="ip";
	public String MACS="macs";
	public String PROPS="props";
	public static final String DEL_MAC = "del_macs";


	public Json formatDevice(Json device) {

		Json j=Json.object();

		j.set(UUID, device.get(UUID));
		j.set(IP, device.get(IP));
		j.set(MACS, device.get(MACS));

		if (device.has(PROPS)) {
			for (Map.Entry<String,Json> entry : device.get(PROPS).asJsonMap().entrySet()) {
				j.set(entry.getKey(),entry.getValue());
			}
		}
		
		if (!j.has("name")) j.set("name", "name undef" );
		if (!j.has("type")) j.set("type", "type undef" );
		if (!j.has("network")) j.set("network", "network undef" );
		

		return j;
	}



	public Json getDevices(Json user) throws DevicesDBException {

		
			Json  rawlist = DevicesDB.instance.exec(user, true,DevicesDB.GET_DEVICES, "", "", "", null, null);

			Json list=Json.array();
			for (Json d:rawlist.asJsonList()) {
				list.add(formatDevice(d));
			}
			return list;

		
	}




	public Json createDevice(Json user,String ip, Json deviceProps ) throws DevicesDBException {
		
		
		return  DevicesDB.instance.exec(user, true,DevicesDB.ADD_DEVICE,ip , "", "", deviceProps, null);

		
		
	}

	
	
	
	public Json getDevice(Json user,boolean mode, String id) throws DevicesDBException {

		
			Json result = DevicesDB.instance.exec(user, mode,DevicesDB.GET_DEVICE, id, "","",null, null);
			if (result.has(ERROR)) return result;
			return formatDevice(result);
		


	}

	private void updateMacs(Json user,boolean mode, String id, Json macs) throws DevicesDBException  {
		
		Json device = DevicesDB.instance.exec(user, mode,DevicesDB.GET_DEVICE, id, "","",null, null);
		
		List<String> newMacs= new ArrayList<String>();
		if (macs.isObject()) throw new DevicesDBException("Invalid value for macs (must be string or array of string)");
		if (macs.isArray()) {
			for (Json jj:macs.asJsonList()) {
				if (jj.isObject()) throw new DevicesDBException("Invalid value for macs (must be string or array of string)");
				newMacs.add(jj.asString());
			}
		}
		else {
			newMacs.add(macs.asString());
		}
		
		List<String> oldMacs= new ArrayList<String>();
		for (Json jj:device.get(MACS).asJsonList()) {
			oldMacs.add(jj.asString());
		}
		
		for (String s:oldMacs) {
			if (!newMacs.contains(s))
				DevicesDB.instance.exec(user, mode,DevicesDB.SET_DEVICE_PROP, id, "",DevicesDB.DEL_MAC,Json.make(s), null);
		}
		
		for (String s:newMacs) {
			if (!oldMacs.contains(s))
				DevicesDB.instance.exec(user, mode,DevicesDB.SET_DEVICE_PROP, id, "",DevicesDB.MACS,Json.make(s), null);
		}
	}


	public Json updateDevice(Json user,boolean mode, String id, Json deviceProps) throws DevicesDBException {

		if (id==null) throw new DevicesDBException("Missing id to update device");
		if (deviceProps==null) throw new DevicesDBException("Missing params to update device");
		
		for (Map.Entry<String,Json> entry : deviceProps.asJsonMap().entrySet()) {
			
			if (entry.getKey().compareToIgnoreCase(IP)==0) {
				System.err.println("To test name=ip");
			}
			else if (entry.getKey().compareToIgnoreCase(MACS)==0) {
				//System.err.println("To test name=MACS");
				updateMacs(user, mode, id, entry.getValue());
			}
			else
				DevicesDB.instance.exec(user, mode,DevicesDB.SET_DEVICE_PROP, id, "",entry.getKey(),entry.getValue(), null);
		}
		
		return Json.object();

	}


	public Json deleteDevice(Json user, boolean mode, String id) throws DevicesDBException {

		Json result = DevicesDB.instance.exec(user, mode,DevicesDB.DEL_DEVICE, id, "","",null, null);
		return result;
		
	
	}

	private String randomType() {
		
		int n=(int)Math.random()*4;
		String [] types=new String[] {"PLC","PC", "SERVEUR", "PC PORTABLE"};
		return types[n];
	}
	
	public Json test1(Json user) throws DevicesDBException {
		
		Json  rawlist = DevicesDB.instance.exec(user, true,DevicesDB.GET_DEVICES, "", "", "", null, null);

		Json list=Json.array();
		int n=1;
		for (Json d:rawlist.asJsonList()) {
			Json props=Json.object();
			d=formatDevice(d);
			if (!d.has("name")) props.set("name","name_"+n);
			if (!d.has("type")) props.set("name","name_"+n);
			
			
		}
		return list;
		
		
		
	}

}
