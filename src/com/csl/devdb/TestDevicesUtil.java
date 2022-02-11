package com.csl.devdb;

import com.xcsl.json.Json;

public class TestDevicesUtil {

	Json NO_INFO=Json.object();
	
	
	
	//   ADD LERAN MODEL TO DEVICES DB
	/*
	 * 
	 * 
	 * 	IDSLearnedRules model = new IDSLearnedRules(idsVariables);
	 *	//model.readFromFile(dir,fLearnRules);
	 *
	 *	DevicesDB.instance.updateLearnedModel(model);
	 *
	 * 
	 */
	
	
	void testupdateLearnedModelFromDevicesDB() {
		
		
	}
	
	
	
	// ADD ALERT TO DEVICES DB
	
	
	
	
	// GENERATE DETECTION MODEL FROM DB
	
	
	
	// GET USER MODEL FROM DEVICES DB
	
	
	//
	//opOnDevices
	//opOnLinks
	//	src, dst ...
	//
	
	/*  Stockage d'un lien
	 * {
	      "src_uuid": "eed17994-fe7e-4947-a2ed-03cede5ec3cf",
	      "dst_uuid": "0082ac41-57e7-4026-85e3-85eab909f78a",
	      "props_list": [
	        {
	          "risk": "UNDEF",
	          "likelihood": "UNDEF",
	          "severity": "UNDEF",
	          "size": 0,
	          "packets": 1,
	          "maxKBpersec": 0,
	          "maxpktpersec": 0,
	          "first_time": 1617722901410,
	          "last_time": 1617722901410,
	          "port_src": -1,
	          "port_dst": 443,
	          "protocol": "TCP",
	          "app_protocol": "",
	          "_id": "#0"
	        }
	      ],
	      "uuid": "2b31f4c9-5cd0-4828-9067-bdb4adf8020c"
	    },
	*/	
	
	// ajouter des fcts de manip de lien
	// 
		
	//===
	
		// anomaly : -1, no link,  0-ok  1-lowrisk  4-abnormal
		
		public Json addLink(String src, String dst,int port_src, int port_dst, int prot, String app_prot, int anomaly) {
			
			return NO_INFO;
		}
				
		
		public Json delLink(String src, String dst,int port_src, int port_dst, int prot, String app_prot, int anomaly) {
			
			return NO_INFO;
		}
		
		
		// return anomaly level
		public Json getLink(String src, String dst,int port_src, int port_dst, int prot, String app_prot, int anomaly) {
			
			return NO_INFO;
		}
}
