package com.csl.modules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.csl.core.CSLContext;
import com.csl.interfaces.ICSLContext;
import com.csl.interfaces.IModule;
import com.csl.interfaces.IModuleContext;
import com.csl.logger.FileLog;
import com.csl.monitor.ActivityMonitor;
import com.csl.web.websockets.CSLWebSocket;
import com.xcsl.ids.IDSMainProcessor;
import com.xcsl.ids.IDSTrace;
import com.xcsl.interfaces.ErrorResult;
import com.xcsl.interfaces.ICSLFlowListener;
import com.xcsl.interfaces.IResult;
import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;



/*   CONFIG
 * 
 * 
 */
public class ModuleIDS implements IModule {

	static boolean DEBUG=true;
	
	ICSLContext context=null;
	//.sendObjectTo(host, port, idOdTarget, flowNumber, objectToSend, true);

	String host="";
	int port=9000;
	String idOfTarget="???";
	int flowNumber=0;
	
	ActivityMonitor activityMonitor = new ActivityMonitor();
	
	//boolean acquit=false;

	//String idsModelDir="";

	
	//boolean logToFile=true;		
	
	boolean loggingOn=false;			// record the packets
	boolean idsDetectOn=false;			// do the detection

	
	boolean sendToBrowser=true;
	boolean sendToConsole=true;
	

	FileLog packetsLog , variablesLog, networkLog;
	private FileLog eventsLog;

	
	//IDSContext idsContext=null;
	IDSMainProcessor idsMainProcessor=null;
	
	boolean running=false;
	//boolean on=true;

	private boolean fileOpened=false;

	private String variablesFilename="VARIABLES";

	private String packetsFilename="PACKETS";

	private String networkFilename="NETWORK";


	
	private long max_size=10000000;

	//public IDSRunner idsRunner;

	
//	public IDSContext getIdsContext() {
//		return idsContext;
//	}



	public boolean isSendToBrowser() {
		return sendToBrowser;
	}



	public void setSendToBrowser(boolean sendToBrowser) {
		this.sendToBrowser = sendToBrowser;
	}



	public boolean isSendToConsole() {
		return sendToConsole;
	}



	public void setSendToConsole(boolean sendToConsole) {
		this.sendToConsole = sendToConsole;
	}


	public String runningState( ) {
		
		return "Running:"+running+" detect:"+idsDetectOn+" record:"+loggingOn+" sendToConsole:"+sendToConsole+" sendToBrowser:"+sendToBrowser;
		
	}

	//	
	public void setModeIdle() {
		idsDetectOn=false;
		loggingOn=false;


	}

	public void setModeRecord() {
		idsDetectOn=false;
		loggingOn=true;
	}

	public void setModeDetect() {
		idsDetectOn=true;
		loggingOn=true;
	}

	

	public void outDisplay(Json jj) {
		if (sendToBrowser) {
			
			Json j=Json.object();
	        j.set("line", jj.toString());
	        j.set("type", "packet");
//			CSLWebSocketForConsole.broadcastMessageJson("log", j);
			CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_CONSOLE,j );
		}
		//if (sendToConsole) System.out.println(jj);
		
	}
	
	
	
	public void openLogFiles() {
		
		fileOpened=true;
		
		Json j=context.getConfig();
		
		String datadir=JsonUtil.getStringFromJson(j, "ids_conf/packets_dir_for_recording", "./recorded_packets");

		this.variablesFilename=JsonUtil.getStringFromJson(j,"ids_conf/variables_prefix_filename", "vars");
		this.packetsFilename=JsonUtil.getStringFromJson(j, "ids_conf/packets_prefix_filename", "pkt");
		this.networkFilename=JsonUtil.getStringFromJson(j, "ids_conf/network_prefix_filename", "net");
			
		

		this.max_size=JsonUtil.getLongFromJson(j, "ids_conf/max_size_of_log_files", 10000000);
		//		CSLUtil.getConfigLongValue(mcontext.getConfig(), "max_size_of_log_files", 10000000);
		//		mcontext.getConfig().get("max_size_of_log_files").asLong();

		packetsLog= new FileLog(datadir, packetsFilename,max_size, CSLContext.instance::getSystemCurrentTimeMillis);
		variablesLog= new FileLog(datadir, variablesFilename,max_size, CSLContext.instance::getSystemCurrentTimeMillis);
		networkLog= new FileLog(datadir, networkFilename,max_size, CSLContext.instance::getSystemCurrentTimeMillis);
	
			
	}
	
	public void reOpenLogFiles(String datadir) {
		
		packetsLog= new FileLog(datadir, packetsFilename,max_size, CSLContext.instance::getSystemCurrentTimeMillis);
		variablesLog= new FileLog(datadir, variablesFilename,max_size, CSLContext.instance::getSystemCurrentTimeMillis);
		networkLog= new FileLog(datadir, networkFilename,max_size, CSLContext.instance::getSystemCurrentTimeMillis);
		
		
	}
	
	
	@Override
	public IResult init(ICSLContext context,IModuleContext mcontext) {
		// TODO Auto-generated method stub
		this.context=context;
		Json config=mcontext.getConfig();
		Json j=context.getConfig();
		
		
		activityMonitor.startTicTask();

	//	this.logToFile= IDSRunner.instance.getIdsParams().isLogToFile();
	//			JsonUtil.getBooleanFromJson(j,  "ids_conf/log_to_file", true) ; // if not read only in the table

			//JsonUtil.getBooleanFromJson(j,  "ids_conf/send_to_console", false) ;
				
		
		//int IDSMode= IDSRunner.instance.getIDSMode();
			//JsonUtil.getIntFromJson(j, "ids_conf/mode",0);
		
		//if (logToFile&&(IDSMode>0))
		{
			
			openLogFiles();
			
//			String datadir=JsonUtil.getStringFromJson(j, "ids_conf/idslogs_dir", "./logsxxx");
//
//			String variablesFilename=JsonUtil.getStringFromJson(j,"ids_conf/variables_prefix_filename", "vars");
//			String packetsFilename=JsonUtil.getStringFromJson(j, "ids_conf/packets_prefix_filename", "pkt");
//			String networkFilename=JsonUtil.getStringFromJson(j, "ids_conf/network_prefix_filename", "net");
//
//			long max_size=JsonUtil.getLongFromJson(j, "ids_conf/max_size_of_log_files", 10000000);
//			//		CSLUtil.getConfigLongValue(mcontext.getConfig(), "max_size_of_log_files", 10000000);
//			//		mcontext.getConfig().get("max_size_of_log_files").asLong();
//
//			packetsLog= new FileLog(datadir, packetsFilename,max_size);
//			variablesLog= new FileLog(datadir, variablesFilename,max_size);
//			networkLog= new FileLog(datadir, networkFilename,max_size);

			//logOnlyNewVariables= CSLUtil.getConfigBooleanValue(mcontext.getConfig(), "log_only_new", false);
		}

		
		
		//idsDataDir= CSLUtil.getConfigStringValue(config,"ids_data_dir", "");

		//  creation of decalred vars
//		if (config.has("vars")) {
//			declaredVars = getListOfString(config,"vars");
//	
//		for (String v:declaredVars) {
//			initVar(v,"0");
//		}
//		}

		
		//idsContext=CSLContext.instance.getIDSMainProcessor().getIDSContext();
		idsMainProcessor=CSLContext.instance.getIDSMainProcessor();
		
		
		this.sendToBrowser=CSLContext.instance.getIdsParams().isSendToBrowser();
		//JsonUtil.getBooleanFromJson(j,  "ids_conf/send_to_browser", false) ;
		this.sendToConsole=CSLContext.instance.getIdsParams().isSendToConsole();

		//IDSContext.createInstance(mcontext);
		//IDSContext.instance.setIdsDataDir(idsDataDir);


		boolean on =JsonUtil.getBooleanFromJson(context.getConfig(), "ids_conf/on",false);
		running=on;
			
		int n_input=1;
		ICSLFlowListener listener=new ICSLFlowListener() {
			@Override
			public String getName() {
				// TODO Auto-generated method stub
				return "IDS";
			}
			@Override
			public int newElementOnQueue(Json jj) {
				//System.out.println("jmf_runninfg="+running);
				//System.out.println(jj);
				if (!running) {
					IDSTrace.log(IDSTrace.UDP_TRACE, "IDS received object but not running ");
					return ICSLFlowListener.REMOVE_FROM_QUEUE;
				}
				//while (!CSLContext.context.getFlowManager().isFlowEmpty(n_input))
				{
				//	Json jj = CSLContext.context.getFlowManager().takeFromFlow(n_input);
					if (CSLContext.instance.getIdsParams().isShowReceivedObject()) System.out.println("Received object:"+jj);
					if (jj.has("type")) {
						//System.out.println(jj);
						String type =jj.get("type").asString();
						
						//System.out.println("type="+type);
						if (type.compareTo("PKT")==0) {
							if (loggingOn) {
								String s= jj.toString();
								packetsLog.RecordLogMessage(s);
							}
							outDisplay(jj);
							//IDSContext.instance.processPacket(jj);
							if (idsDetectOn) idsMainProcessor.processPacket(jj);
							//System.out.println(jj);
							//packetsLog.RecordLogMessage(jj.toString());
						}
						else if (type.compareTo("VAR")==0) {
							//IDSContext.instance.processVariables(jj);  // use this for symetry, but processing of vars is made in this module
							if (loggingOn) 
								variablesLog.RecordLogMessage(jj.toString());
							outDisplay(jj);
							if (idsDetectOn) idsMainProcessor.processVariables(jj);
						}
						else if (type.compareTo("EVT")==0) {
							//idsMainProcessor.processEvent(jj);
							//if (loggingOn) 
							//	variablesLog.RecordLogMessage(jj.toString());
							outDisplay(jj);
							if (idsDetectOn) idsMainProcessor.processEvent(jj);
						} 
						else if (type.compareTo("EVE")==0) {
							if (loggingOn) packetsLog.RecordLogMessage(jj.toString());
							//	variablesLog.RecordLogMessage(jj.toString());
							outDisplay(jj);
							if (idsDetectOn) idsMainProcessor.processEvent(jj);
						} 
						else if (type.compareTo("TIC")==0) {
							//if (loggingOn) packetsLog.RecordLogMessage(jj.toString());
							//	variablesLog.RecordLogMessage(jj.toString());
							//outDisplay(jj);
							//if (idsDetectOn) idsMainProcessor.processEvent(jj);
							
							activityMonitor.processEvent(jj);
						} 
						else if (CSLContext.instance.isTestMode()) {
							if (type.compareTo("CTRL")==0) {
								String cmd=JsonUtil.getStringFromJson(jj, "cmd", "");
								if (cmd.compareTo("stop")==0) {
									System.err.println("Received cmd:stop");
									System.exit(0);
								}
							}
						} 
						
//						else if (type.compareTo("CMD")==0) {
//							outDisplay(jj);
//							//IDSContext.instance.processEvent(jj);
//							idsContext.getCommandProcessor().processCommandInDetectMode(idsContext, jj);
//						}
						else if ((type.compareTo("NET_FLOW")==0)||(type.compareTo("NET_NODE")==0)) {
							if (loggingOn) 
								networkLog.RecordLogMessage(jj.toString());
							outDisplay(jj);
						}
					} else {
						//System.out.println("NO TYPE"+jj);
						outDisplay(jj);
					}
						
				}
				return ICSLFlowListener.REMOVE_FROM_QUEUE; // cancel next listeners
			}
		};

		
		idsMainProcessor.init();
		
		CSLContext.instance.getCslUDPServer().addListener(n_input,listener); 
		
		//CSLContext.context.getFlowManager().addListener(n_input,listener); 

		
		

		return IResult.OK;
	}



//	public void processVariables(Json jj) {
//
//		if (jj.has("vars")) {
//			List<Json> vars =jj.get("vars").asJsonList();
//			for (Json v:vars) {
//				if (v.has("name")) {
//					String n=v.get("name").asString();
//					if (!declaredVars.contains(n)) {
//						if (!unDeclaredVars.contains(n)) {
//					
//							CSLContext.cslLogger.info("This variable has not not been declared in IDS <"+n+">");
//							unDeclaredVars.add(n);
//						}
//						//if (DEBUG) System.out.println("This variable has not not been declared in IDS <"+n+">");
//					} else {
//						if (v.has("value")) {
//							String value=v.get("value").asString();
//							double x=context.getGlobalVariablesTable().get(n).getAsDouble();
//							context.getGlobalVariablesTable().get(n).setValue(value);
//							context.getGlobalVariablesTable().get(n).setInitialized(true);
//							
//							//if (DEBUG) System.out.println("Set var "+n+" to "+value);
//							IDSTrace.log(IDSTrace.PROCESS_VAR,"Set var "+n+" to "+value+" at "+CSLContext.context.getSystemCurrentTimeMillisAsFormattedString()
//							+ "  (was "+x+ ")");
//						}
//						else
//							System.err.println("No value for var:"+n);
//					}
//
//				}
//			}
//			
//		if (idsContext.getSyslearner()!=null) {
//			idsContext.getSyslearner().processVariables(jj);
//		}
//		}
//	}

	


	private List<String> getListOfString(Json config, String name) {
		Json params=config.get(name);

		List<String> l= new ArrayList<String>();
		if (params==null) return l;
		Iterator<Json> itr=params.iterator();
		while(itr.hasNext()) {
			Json jv = itr.next();
			l.add(jv.asString());
		}
		return l;
	}

//	void initVar(String name,String value)  {
//
//		boolean initialized=true;
//		if (value.isEmpty()) {
//			initialized=false;
//			value="0";
//		}
//		
//		double x= new Double(value);
//		if (!context.getGlobalVariablesTable().varDefined(name))
//			context.getGlobalVariablesTable().createDoubleVariable(name,x);
//		
//		context.getGlobalVariablesTable().get(name).setInitialized(initialized);
//	}

	@Override
	public IResult start(ICSLContext context,IModuleContext mcontext) {
		// TODO Auto-generated method stub
		//context.getGlobalVariablesTable().createDoubleVariable("u",0);



		return IResult.OK;
	}

	@Override
	public IResult restart(ICSLContext context,IModuleContext mcontext) {
		// TODO Auto-generated method stub
		return IResult.OK;
	}

	@Override
	public IResult stop(ICSLContext context,IModuleContext mcontext) {
		// TODO Auto-generated method stub
		return IResult.OK;
	}



	@Override
	public IResult execInputPart(ICSLContext context,IModuleContext mcontext) {
		// TODO Auto-generated method stub
		//CSLContext.logger.debug("exec input "+mcontext.getName());
		return IResult.OK;

	}

	
	
	@Override
	public IResult execStepPart(ICSLContext context,IModuleContext mcontext) {

		if (!running) return IResult.OK;
		//System.out.println("step");
		if (idsDetectOn) idsMainProcessor.execSysStateRules(context.getSystemCurrentTimeMillis());
	//	System.out.println(context.getGlobalVariablesTable().toPrettyString());

		// TODO Auto-generated method stub
		//		CSLContext.logger.debug("exec step "+mcontext.getName());
		//		
		//		Json jj= Json.object();
		//		jj.at("test_value","hello");
		//		
		//		CSLContext.context.getFlowManager()
		//		.sendObjectTo(host, port, idOfTarget, flowNumber, jj, true);
		//		//System.out.println("SEND "+jj);
		return IResult.OK;

	}

	@Override
	public IResult execOutputPart(ICSLContext context,IModuleContext mcontext) {
		// TODO Auto-generated method stub
		//CSLContext.logger.debug("exec output "+mcontext.getName());
		return IResult.OK;

	}


	@Override
	public IResult execCommand(ICSLContext context, IModuleContext mcontext, Map<String, String> params) {
		// TODO Auto-generated method stub
		return new ErrorResult("Invalid Command", 1);


	}

	static {
		CSLContext.instance.registerModuleClass("ModuleIDS",ModuleIDS.class);
	}

//	public void synchronizeDeclaredVars() {
//		// TODO Auto-generated method stub
//		for (IDSProcessVariableDescriptor pvd:idsContext.getIdsVariables().getListOfIDSPRocessVariableDescriptor()) {
//			if (!declaredVars.contains(pvd.getName()) ) {
//					declaredVars.add(pvd.getName());
//			}
//			if (pvd.isInitialized()) {
//				initVar(pvd.getName(),""+pvd.getInitialValue());
//			}
//			else {
//				initVar(pvd.getName(),"");
//				
//			}
//		}
//		
//		//System.out.println(CSLContext.context.getGlobalVariablesTable());
//	}

}
