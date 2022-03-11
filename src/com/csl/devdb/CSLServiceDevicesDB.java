package com.csl.devdb;

import com.csl.core.CSLContext;
import com.csl.devdb.util.ApiDevice;
import com.csl.devdb.util.DBOpParams;
import com.csl.devdb.util.DevicesDBCompare;
import com.csl.intercom.jsoncmd.ApiCommands;
import com.csl.intercom.jsoncmd.ApiCommandsFactory;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.xcsl.interfaces.IApiCommands;
import com.xcsl.interfaces.ICSLService;
import com.xcsl.interfaces.IJsonCmd;
import com.xcsl.ids.IDSMainProcessor;
import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;

public class CSLServiceDevicesDB implements ICSLService {


	static boolean debug =true;
	//static CSLAlertManager cslAlertManager = new CSLAlertManager("Intrusion detection");
	IApiCommands apiCommands=new ApiCommandsFactory().createApiCommands("/"+"devdb");
			//new ApiCommands("/"+"devdb");
	
	
	ApiDevice apiDevice= new ApiDevice();

	@Override
	public IApiCommands getApiCommands() {
		// TODO Auto-generated method stub
		return apiCommands;
	}

	@Override
	public String getConfigFileSectionName() {
		// TODO Auto-generated method stub
		return "devdb";
	}

	@Override
	public boolean terminate() {
		// TODO Auto-generated method stub
		return true;
	}

//	public Json execDevicesDBOp(Json params) {
//		
//		
//		DBOpParams p= new DBOpParams(params);
//		if (debug) System.out.println("EXEC Devices DB operation:"+p);
//		
//		
//		
//			
//		if (debug) System.out.println("EXEC devicesDBOperation op:"+op+" modeIP:"+modeIp
//				+" id="+id+" id2="+id2+" path:"+path+" value"+value);
//		Json result;
//		try {
//			result = apiDevice.exec(user, modeIp,op, id, id2, path, value,selector);
//			
//		} catch (DevicesDBException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			result=Json.object().set("error", e.getMessage());
//		}
//
//		
//		
//		if (debug) System.out.println("RESULT <"+op+">:"+result);
//		
//
//		return result;
//	}
	
	public boolean init(Json j, String userDir) {

		
		
		
		/*
		 * user : Json describing the user (for logging of actions)
		 * modeip the identifier is ip if true, uuid if false
		 * op
		 * id  (ip or uuid
		 * path
		 * newParams
		 * oldP
		 */
		

		// api : let x = await asyncExecApiCmd("devdb","op",

	
		
		
		//CSLHttpServer.addApiCommands(apiCommands);
	
		//JServiceLoader.addApi(apiCommands);

	
		
		
			
		// operations : create, delete, rename, select, copy
		
		apiCommands.registerCmd("op", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				
				DBOpParams p= new DBOpParams(params);
				if (debug) System.out.println("EXEC Devices DB operation:"+p);
				
				
				System.out.println("EXEC Devices DB operation:"+p);
				
					
				
				Json result;
				try {
					result = DevicesDB.instance.exec(
							p.getUser(),
							p.getModeIp(),
							p.getOp(),
							p.getId(),
							p.getId2(),
							p.getPath(),
							p.getValue(),
							p.getSelector()
							);
							
							
					
				} catch (DevicesDBException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					result=Json.object().set("error", e.getMessage());
				}

				
				
				if (debug) System.out.println("RESULT <"+p.getOp()+">:"+result);
				

				return result;
				
			}
		},
				new JsonCmdHelp()
				.setDesc("operation on devices database")
				.setParam("op", "name of operation : get, ...",JsonCmdHelp.STR)
				.setResult("return the result of the operation or error",JsonCmdHelp.STR).hide()
				);
		
		
	
		
		/*******************************************
		 * 
		 * 
		 ********************************************/
		
		apiCommands.registerCmd("add_device", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				
				
				DBOpParams p= new DBOpParams(params);
				Json result;
				
				if (debug) System.out.println("EXEC Devices DB operation:"+p);

				/*try {
					result= apiDevice.getDevices(p.getUser());
				} catch (DevicesDBException e) {
					// TODO Auto-generated catch block
					result=Json.object().set("error", e.getMessage());
				}*/
				if (p.hasError()) {
					return Json.object().set("error",p.getError());
				}
				if (!p.hasId()) {
					return Json.object().set("error","IP required to create device");
				}
				try {
					result=apiDevice.createDevice(p.getUser(), p.getId(), p.getValue());
				} catch (DevicesDBException e) {
					// TODO Auto-generated catch block
					result=Json.object().set("error", e.getMessage());
				}
				
				return result;
			}
		},
				new JsonCmdHelp()
				.setDesc("create a device")
				.setParam("ip", "ip of the device ", JsonCmdHelp.STR)
				.setParam("props", "Json object with the props to update (or create)", JsonCmdHelp.JSON)
				
				.setResult("error if any or empty",JsonCmdHelp.JSON).setStatus(JsonCmdHelp.STATUS_TODO)
				);
		
		
		
		apiCommands.registerCmd("get_device", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				
				
				DBOpParams p= new DBOpParams(params);
				Json result;
				
				if (debug) System.out.println("EXEC Devices DB operation:"+p);

				try {
					result= apiDevice.getDevices(p.getUser());
				} catch (DevicesDBException e) {
					// TODO Auto-generated catch block
					result=Json.object().set("error", e.getMessage());
				}
				
				return result;
			}
		},
				new JsonCmdHelp()
				.setDesc("get a device")
				//.setParam("mode_ip", "true if id is IP, false if id is uuid", JsonCmdHelp.BOOL)
				.setParam("ip", "ip of the device ", JsonCmdHelp.STR)
				.setParam("uuid", "uuid of the device (alternative to ip) ", JsonCmdHelp.STR)
				
				.setResult("device in Json",JsonCmdHelp.JSON)
				);
		
		
		apiCommands.registerCmd("update_device", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				
				
				DBOpParams p= new DBOpParams(params);
				Json result;
				
				if (debug) System.out.println("EXEC Devices DB operation:"+p);

				try {
					result= apiDevice.updateDevice(p.getUser(),p.getModeIp(), p.getId(), p.getValue());
				} catch (DevicesDBException e) {
					// TODO Auto-generated catch block
					result=Json.object().set("error", e.getMessage());
				}
				
				return result;
			}
		},
				new JsonCmdHelp()
				.setDesc("update a device")
				//.setParam("mode_ip", "true if id is IP, false if id is uuid", JsonCmdHelp.BOOL)
				.setParam("id", "id of the device to update", JsonCmdHelp.STR)
				.setParam("uuid", "uuid of the device (alternative to ip) ", JsonCmdHelp.STR)
			
				.setParam("props", "Json object with the props to update (or create)", JsonCmdHelp.JSON)
				.setResult("error if any or empty",JsonCmdHelp.JSON).setStatus(JsonCmdHelp.STATUS_TOTEST)
				);
		
		
		apiCommands.registerCmd("del_device", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				
				
				DBOpParams p= new DBOpParams(params);
				Json result;
				
				if (debug) System.out.println("EXEC Devices DB operation:"+p);

				try {
					result= apiDevice.deleteDevice(p.getUser(),p.getModeIp(), p.getId());
				} catch (DevicesDBException e) {
					// TODO Auto-generated catch block
					result=Json.object().set("error", e.getMessage());
				}
				
				return result;
			}
		},
				new JsonCmdHelp()
				.setDesc("del a device")
				//.setParam("mode_ip", "true if id is IP, false if id is uuid", JsonCmdHelp.BOOL)
				.setParam("id", "id of the device ", JsonCmdHelp.STR)
				.setParam("uuid", "uuid of the device (alternative to ip) ", JsonCmdHelp.STR)
				
				.setResult("error if any or empty",JsonCmdHelp.JSON)
				.setStatus(JsonCmdHelp.STATUS_TODO)
				);
		
		apiCommands.registerCmd("get_devices", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				
				
				DBOpParams p= new DBOpParams(params);
				Json result;
				
				if (debug) System.out.println("EXEC Devices DB operation:"+p);

				try {
					result= apiDevice.getDevices(p.getUser());
				} catch (DevicesDBException e) {
					// TODO Auto-generated catch block
					result=Json.object().set("error", e.getMessage());
				}
				
				return result;
			}
		},
				new JsonCmdHelp()
				.setDesc("returns the list of devices")
				
				.setResult("array of devices in Json",JsonCmdHelp.JSON)
				);
		
		apiCommands.registerCmd("clear", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				
				if (debug) {
					DevicesDB.instance.clear();
					DevicesDB.instance.saveFiles();
				}
				else {
					System.err.println("works only in debug mode");
				}
				
				return Json.object();
			}
		},
				
				
				new JsonCmdHelp()
				.setDesc("Clear devices database (works in dev mode)")
				
				);
			

		apiCommands.registerCmd("test1", new IJsonCmd() {
			@Override
			public Json exec(Json params) {
				
				if (debug) {
					DevicesDB.instance.clear();
					
				}
				else {
					System.err.println("works only in debug mode");
				}
				
				return Json.object();
			}
		},
				
				
				new JsonCmdHelp()
				.setDesc("Clear devices database (works in dev mode)")
				
				);
	
	// operations : create, delete, rename, select, copy
		apiCommands.registerCmd("add_learned_model_to_dbdevices", new IJsonCmd() {

				@Override
				public Json exec(Json params) {

					System.out.println("EXEC add_to_dbdevices:"+params);


					IDSMainProcessor idsMainProcessor=CSLContext.instance.getIDSMainProcessor();
//					
//					String filePath=
//							CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDir()+File.separator+
//							CSLContext.instance.getIdsRunner().getIdsParams().getLearnedRulesFileName();
//					FileUtils.backupFileWithTimeStamp(filePath, CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDirBackup());

					idsMainProcessor.backupLearnedModel();
					
//					String dirfilePathOps=
//							CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDir()+File.separator+
//							"internal";
//
//					FileUtils.backupFileWithTimeStamp(dirfilePathOps+File.separator+"ops", CSLContext.instance.getIdsRunner().getIdsParams().getIdsModelDirBackup());
//					IDSTrace.log(IDSTrace.GENERAL, "test");
					
					idsMainProcessor.backupFileInModelDir("internal", "ops");
					
					//Json ops=DevicesUtil.updateLearnedModelFromDevicesDB(CSLContext.instance.getIdsRunner().getIdsParams());

					Json ops=DevicesUtil.updateDevicesDBFromLearnedModel(CSLContext.instance.getIDSMainProcessor());

					//FileUtils.saveJsonToFile(dirfilePathOps, "ops", ops);
					
					idsMainProcessor.saveJsonInModelDir("internal", "ops", ops);
					Json result=Json.object();

					return result;
				}
			},
				
				
				new JsonCmdHelp()
				.setDesc("add the learned model to the devices database").hide()
				
				);
				
		

		apiCommands.registerCmd("debug", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				
				if (debug) System.out.println("EXEC Devices DB operation:"+params);
				
				Json user=params.get("user");
				if (user==null) {
					return Json.object().set("error","No user specified");
				}
				String op= JsonUtil.getStringFromJson(params, "op","");
			
				String p1=JsonUtil.getStringFromJson(params,"p1","");
				Json selector=params.get("selector");
				
					
				
				Json result=Json.object();
				
					result = new DevicesDBCompare().compareDataBase("devices","devices2");
				
				
				
				if (debug) System.out.println("RESULT <"+op+">:"+result);
				

				return result;
			}
		},
				new JsonCmdHelp()
				.setDesc("operation on devices database")
				.setParam("name", "name of operation (get,set ...)",JsonCmdHelp.STR)
				
				.setResult("return the result of the operation or error",JsonCmdHelp.STR)
				.hide()
				);
		
			
		apiCommands.registerCmd("backup", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				
				if (debug) System.out.println("EXEC Devices DB backup:"+params);
				
				
				Json result=Json.object();
				
				result = DevicesDB.instance.backupFiles();
				
				return result;
			}
		},
				new JsonCmdHelp()
				.setDesc("backup database and histo")
				);
		
		
		
		apiCommands.registerCmd("save", new IJsonCmd() {

			@Override
			public Json exec(Json params) {
				
				if (debug) System.out.println("EXEC Devices DB save:"+params);
				
				
				Json result=Json.object();
				
				DevicesDB.instance.saveFiles();
				
				return result;
			}
		},
				new JsonCmdHelp()
				.setDesc("force save database (usually not needed, autosave)")
				
				);
		
			
			
			
			return true;
			
	}

}
