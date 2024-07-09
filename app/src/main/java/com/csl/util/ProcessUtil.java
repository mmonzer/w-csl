package com.csl.util;

import com.csl.core.CSLContext;
import com.csl.logger.CSLLogger;
import com.csl.web.websockets.CSLWebSocket;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import main.util.WebsocketClientListener;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ProcessUtil {

	static public boolean DEBUG=false;


	static public void listJavaProcesses()  {

		Process process;
		try {
			process = Runtime.getRuntime().exec("ps -e");

			BufferedReader processReader =  new BufferedReader(new InputStreamReader(process.getInputStream()));
			// Read from BufferedReader

			List<String> list = readAllLines(processReader,"java");

			for (String s:list)
				System.out.println(s);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
	
	static public boolean isProcessRunning(String filter ) {
		return findProcess(filter).size()>0;
	}

	static public List<String> findProcess(String filter) {

		List<String>content = new 	ArrayList<String>();

		try {

			Process process = Runtime.getRuntime().exec("ps -e");
			BufferedReader reader =  new BufferedReader(new InputStreamReader(process.getInputStream()));

			//StringBuilder content = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {
				//	content.append(line);
				//	content.append(System.lineSeparator());
				//if (line.contains(filter)) {
				if (lineContains(line,filter)) {
					String[] parse = line.split("\\s", 2);
					if (parse.length>0) {

						String id=parse[0];
						content.add(id);
					}
				}


			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return content;
	}



	static public List<String> readAllLines(BufferedReader reader,String filter) throws IOException {
		List<String>content = new 	ArrayList<String>();
		String line;

		while ((line = reader.readLine()) != null) {
			//content.append(line);
			//content.append(System.lineSeparator());

			if (line.contains(filter)) {
				//System.out.println(line);
				content.add(line);
			}


		}

		return content;
	}

	// filter is a name or a list of name with ;

	static boolean lineContains(String line,String filter) {


		String[] tokens=filter.split(";");

		for (int i=0;i<tokens.length;i++) {
			if (line.contains(tokens[i])) return true;
		}
		return false;
	}


	static public void killProcess(String filter)  {

		if (filter.isEmpty()) return;
		try {

			Process process = Runtime.getRuntime().exec("ps -e");
			BufferedReader reader =  new BufferedReader(new InputStreamReader(process.getInputStream()));

			//StringBuilder content = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {
				//	content.append(line);
				//	content.append(System.lineSeparator());
				//if (line.contains(filter)) {
				if (lineContains(line,filter)) {

					String[] parse = line.split("\\s", 2);
					if (parse.length>0) {

						String id=parse[0];
						if (DEBUG) System.out.println("Kill ["+id+"]-->" +line);

						kill(id);
					}
				}


			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}



	static public void kill(String id) {

		if (id.compareTo(""+getPID())==0) return ;

		//String id="3144";
		try {
			Process process = Runtime.getRuntime().exec("kill -9 "+id);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	static public String startJarIfNotRunning(String dir, String jarFile, boolean showOuput, boolean send_output_to_hmi) {

		List<String> z = findProcess(jarFile);
		if (z.isEmpty()) 
			return startJar(dir, jarFile, showOuput,send_output_to_hmi);
		else
			return "Process already running java -jar "+jarFile+" in "+dir;

	}



	static public String startJar(String dir, String jarFile, boolean showOuput, boolean send_output_to_hmi) {
		ProcessBuilder pb = new ProcessBuilder("java", "-jar", jarFile);
		pb.directory(new File(dir));
		try {
			Process p = pb.start();
			//LogStreamReader lsr = new LogStreamReader(p.getInputStream());
			//Thread thread = new Thread(lsr, "LogStreamReader");
			//thread.start();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

			Thread thread = new Thread(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						String line = reader.readLine();
						while (line != null) {
							if (showOuput) System.out.println(line);
							if (send_output_to_hmi) sendStrToHmi(jarFile, line);

							line = reader.readLine();
						}
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			thread.start();
			return "starting java -jar "+ jarFile+" in directory "+dir;

		} catch (IOException e) {
			e.printStackTrace();
			return "cannot start java -jar "+ jarFile+" in directory "+dir;

		}
	}

	//static public String startScript2(String dir, String scriptName, String filter,boolean sudo, boolean showOuput, boolean send_output_to_hmi) {



	static public String startScript(String dir, String scriptName, String			filter,boolean sudo, boolean showOuput, boolean send_output_to_hmi) {
		//					if (!filter.isEmpty()) {
		//			            killProcess(filter);
		//			        }			        


/*		IDSTrace.log(IDSTrace.XTERNAL_COMMANDS, "External command startScript scriptName ="+scriptName);
		IDSTrace.log(IDSTrace.XTERNAL_COMMANDS, "External command startScript dir        ="+dir);
		IDSTrace.log(IDSTrace.XTERNAL_COMMANDS, "External command startScript filter     =<"+filter+">");
		IDSTrace.log(IDSTrace.XTERNAL_COMMANDS, "External command startScript sudo       ="+sudo);	
		IDSTrace.log(IDSTrace.XTERNAL_COMMANDS, "External command startScript show_output="+showOuput);
		IDSTrace.log(IDSTrace.XTERNAL_COMMANDS, "External command startScript      to_hmi="+send_output_to_hmi);
*/

		String cmd="./"+scriptName;
		String info=cmd;





		ProcessBuilder pb ;
		if (sudo) {
			pb = new ProcessBuilder("sudo",cmd);
			if (showOuput) System.out.println("Running cmd: sudo "+cmd);
		}
		else {
			pb = new ProcessBuilder(cmd);
			if (showOuput) System.out.println("Running cmd: "+cmd);
		}


		File fdir=new File(dir);
		if (!fdir.exists()|!fdir.isDirectory()) {
			System.err.println("*** ERROR *** Cannot not find directory :"+fdir);

		}
		if (showOuput) System.out.println("Running in dir :"+fdir);

		File fcmd = new File(fdir,scriptName); 
		if (!fcmd.exists()) {
			String s="";
			System.err.println("Cannot not find script :"+scriptName);
			if (showOuput) System.out.println("Direcory contents :");
			File[] fileList = fdir.listFiles();
			for (File file: fileList) {
				if (showOuput) System.out.println(file);
				if (!s.isEmpty()) s=s+",";
				s=s+file;
			}
			return "Error, script "+scriptName+" not found in "+dir+ "   (files:"+s+")";
		}

		pb.directory(new File(dir));

		try {
			Process p = pb.start();

			final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			final BufferedReader readerError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						String line = reader.readLine();
						while (line != null) {
							if (showOuput) {
								System.out.println(line);
							}
							if (send_output_to_hmi) {
								sendStrToHmi(scriptName, line);
							}
							line = reader.readLine();
						}
						reader.close();

						String lineError = readerError.readLine();
						while (lineError != null) {
							if (showOuput) {
								System.out.println(lineError);
								if (send_output_to_hmi) {
									sendStrToHmi(scriptName, lineError);
								}
							}
							lineError = readerError.readLine();
						}
						readerError.close();			                        
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			thread.start();
			return "starting "+info;
		} catch (IOException e) {
			e.printStackTrace();
			return "cannot start "+info;
		}
	}






	static public String startScript2(String dir, String scriptName, String filter,boolean sudo, boolean showOuput, boolean send_output_to_hmi) {

		//		if (!filter.isEmpty()) {
		//			killProcess(filter);
		//		}

		String cmd=dir+File.separator+scriptName;
		cmd="./"+scriptName;


		String info="";
		ProcessBuilder pb ;
		if (sudo) {

			pb = new ProcessBuilder("sudo",cmd);
			if (showOuput) System.out.println("Running: sudo "+cmd);
			info="sudo "+cmd;
		}
		else {
			pb = new ProcessBuilder(cmd);
			if (showOuput) System.out.println("Running:"+cmd);

		}

		File fdir=new File(dir);
		if (!fdir.exists()|!fdir.isDirectory()) {
			System.err.println("*** ERROR *** Cannot not find directory :"+fdir);

		}
		if (showOuput) System.out.println("Running in dir :"+fdir);

		File fcmd = new File(fdir,scriptName); 
		if (!fcmd.exists()) {
			System.err.println("Cannot not find script :"+scriptName);
			if (showOuput) System.out.println("Direcory contents :");
			File[] fileList = fdir.listFiles();
			for (File file: fileList) {
				if (showOuput) System.out.println(file);
			}
		}

		pb.directory(new File(dir));

		try {
			Process p = pb.start();

			final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

			Thread thread = new Thread(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						String line = reader.readLine();
						while (line != null) {
							if (showOuput) System.out.println(line);
							if (send_output_to_hmi) sendStrToHmi(scriptName, line);
							line = reader.readLine();
						}
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			thread.start();

			return "starting "+info;
		} catch (IOException e) {
			e.printStackTrace();
			return "cannot start "+info;
		}
	}


	
	static public String execCmdInShell(String dir, String cmd,  boolean showOuput) {
		//					if (!filter.isEmpty()) {
		//			            killProcess(filter);
		//			        }			        


		
		String info=cmd;





		ProcessBuilder pb ;
		{
			pb = new ProcessBuilder(cmd);
			if (showOuput) System.out.println("Running cmd: "+cmd);
		}


		if (!dir.isEmpty()) {
			File fdir=new File(dir);
			if (!fdir.exists()|!fdir.isDirectory()) {
				System.err.println("*** ERROR *** Cannot not find directory :"+fdir);

			}
			if (showOuput) System.out.println("Running in dir :"+fdir);



			pb.directory(new File(dir));
		}

		try {
			Process p = pb.start();

			final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			final BufferedReader readerError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

			/*Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						String line = reader.readLine();
						while (line != null) {
							if (showOuput) {
								System.out.println(line);
							}
							
							line = reader.readLine();
						}
						reader.close();

						String lineError = readerError.readLine();
						while (lineError != null) {
							if (showOuput) {
								System.out.println(lineError);
								
							}
							lineError = readerError.readLine();
						}
						readerError.close();			                        
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			thread.start();*/
			//int exitCode = p.waitFor();
			return "starting "+info; //+" exit code:"+exitCode;
		} catch (IOException e) {
			e.printStackTrace();
			return "cannot start "+info;
		} 
		/*catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "interrupted "+info;
		}*/
	}



	public static void sendStrToHmi(String name,String s) {
		Json j = Json.object();
		j.set("line", s);
		j.set("console_id", "csl_monitor:"+name);
		//CSLWebSocketForConsole.broadcastMessageJson("log", j);
		CSLWebSocket.broadcastMessageJson(CSLWebSocket.WEB_SOCKET_CONSOLE, j);
	}


	/*
	 * 
	 *  j.set("name","start_modbus"); 	// used to regsiter html command
	 * 	
	 	j.set("dir","");						// name of the command. If empty, --> user.dir
		j.set("mode","jar"); 					// jar or script
		j.set("sudo", true);					// sudo true to start and kill in sudo mode
												// can only be used with script
		j.set("jarfile","modbuspal.jar");		// name of the jar
		j.set("script","start.sh");				// name of the script 
		j.set("kill_filter","modbuspal");		// used to select the process to kill from ps -e
		j.set("kill_script", "");				// if sudo mode, used to kill the script

		show_output								// display process output if true
		kill_if_running							// if true, kill all process with same name (filter)

		Note : in sudo mode, the command must be added in a file copied to the sudoers.d dir
			/private/etc/sudoers.d
		or  /etc/sudoers.d

		Exemple :
		C-GSCOP-9872:sudoers.d flausj$ cat csl
		flausj  ALL=(ALL) NOPASSWD: /Users/flausj/Documents/dev/start_modbuspal.sh
		flausj  ALL=(ALL) NOPASSWD: /Users/flausj/git/z_csl/Z_CSL/start_modbuspal.sh
		flausj  ALL=(ALL) NOPASSWD: /Users/flausj/git/z_csl/Z_CSL/stop_modbuspal.sh
	 */

	static public String exec(Json j) {

		String name=JsonUtil.getStringFromJson(j,"name","");

		boolean showOuput=JsonUtil.getBooleanFromJson(j,"show_output",false);
		boolean send_output_to_hmi=JsonUtil.getBooleanFromJson(j,"send_output_to_hmi",false);


		boolean killIfRunning=JsonUtil.getBooleanFromJson(j,"kill_if_running",false);

		String dir = JsonUtil.getStringFromJson(j,"dir","");
		if (dir.isEmpty()) {
			dir=System.getProperty("user.dir");
		}
		File fdir = new File(dir);
		if (!fdir.exists()) {
			//IDSTrace.log(IDSTrace.XTERNAL_COMMANDS, "*** ERROR *** Cannnot find directory:"+dir);
			
			System.err.println("*** ERROR *** Cannnot find directory:"+dir);
		}
		if (!fdir.isDirectory()) {
			//IDSTrace.log(IDSTrace.XTERNAL_COMMANDS, "*** ERROR *** Not a directory:"+dir);
			System.err.println("*** ERROR *** Not a directory:"+dir);
		}

		String mode = JsonUtil.getStringFromJson(j,"mode","");


		//IDSTrace.log(IDSTrace.XTERNAL_COMMANDS, "External command mode :"+mode);

		String filename="";
		if (mode.compareToIgnoreCase("jar")==0) {
			filename=JsonUtil.getStringFromJson(j,"jar","");
		}
		else if (mode.compareToIgnoreCase("script")==0) {
			filename=JsonUtil.getStringFromJson(j,"script","");
		}
		else if (mode.compareToIgnoreCase("remote")==0) {
			//	filename=JsonUtil.getStringFromJson(j,"script","");
		}
		else {
			CSLContext.instance.printError("Invalid mode:"+mode+" (should be jar or script");
		}
		boolean sudo =JsonUtil.getBooleanFromJson(j,"sudo",false);

		String kill_filter = JsonUtil.getStringFromJson(j,"kill_filter","");

		// executed in sudo mode
		String kill_script = JsonUtil.getStringFromJson(j,"kill_script",""); 

		//boolean kill_sudo =JsonUtil.getBooleanFromJson(j,"kill_sudo",false);

		if (mode.compareTo("jar")==0) {
			if (killIfRunning) {
				killProcess(kill_filter);
				return startJar(dir, filename, showOuput, send_output_to_hmi);
			}
			else {
				return startJarIfNotRunning(dir, filename, showOuput, send_output_to_hmi);
			}
		}


		if ((mode.compareTo("script")==0)&&(sudo)) {
			if (killIfRunning) {
				return ProcessUtil.startScript(dir, kill_script,"",true, showOuput, send_output_to_hmi);
			}
			else {
				List<String> z = findProcess(kill_filter);
				if (z.isEmpty()) 
					return ProcessUtil.startScript(dir, filename,"",true, showOuput, send_output_to_hmi);
				else 
					return "Process already running :"+filename+" in "+dir;
			}
		}

		if ((mode.compareTo("script")==0)&&(!sudo)) {
			if (killIfRunning) {
				killProcess(kill_filter);
				return ProcessUtil.startScript(dir, filename,"",false, showOuput, send_output_to_hmi);
			}
			else {
				List<String> z = findProcess(kill_filter);
				if (z.isEmpty()) 
					return ProcessUtil.startScript(dir, filename,"",false, showOuput, send_output_to_hmi);
				else 
					return "Process already running :"+filename+" in "+dir;
			}
		}


		if ((mode.compareTo("remote")==0)&&(!sudo)) {

			String url =JsonUtil.getStringFromJson(j,"url","");
			String remote_name=JsonUtil.getStringFromJson(j,"remote_name",""); 
			if (remote_name.isEmpty()) remote_name=name;
			return execRemote(url,remote_name);
		}

		return "Invalid command ";
	}



	public static String WEB_SOCKET_CONSOLE="console";

	static public void listenRemote(String url) {
		//String s= "ws://" + "127.0.0.1" + ":" + "8000" + "/database";


		if (!url.endsWith("/")) url =url+"/";


		String s= "ws://" + url+ WEB_SOCKET_CONSOLE;


		//final WebsocketClientListener clientEndPoint = new WebsocketClientListener(new URI(s)); 
		String sws= url;

		if (sws.startsWith("http://")) {
			sws=sws.substring("http://".length());

		}
		sws="ws://"+sws+WEB_SOCKET_CONSOLE;

		sws=sws.replace("localhost", "127.0.0.1");

		System.out.println("Connect to websocket "+sws);

		final WebsocketClientListener clientEndPoint = WebsocketClientListener.get(sws);

		if (clientEndPoint==null) {

			System.err.println("Cannot connect to websocket "+s);
			return;
		}


		// add listener
		clientEndPoint.addMessageHandler(new WebsocketClientListener.MessageHandler() {
			public void handleMessage(String message) {
				//CSLWebSocketForConsole.broadcastMessageString("log", message);
				CSLWebSocket.broadcastMessageString(CSLWebSocket.WEB_SOCKET_CONSOLE, message);
				System.out.println("WS:"+message);
			}
		});

		//  Thread.sleep(100);



	}


	static public String execRemote(String url,String cmd) {


		listenRemote(url);

		if (!url.endsWith("/")) url =url+"/";


		System.out.println("XXXREMOTE:"+url+cmd);

		HttpGet get = new HttpGet(url+cmd);
		HttpClient  client    = HttpClientBuilder.create().build();

		try {
			HttpResponse response = client.execute(get);

			BufferedReader in = new BufferedReader(new InputStreamReader(response
					.getEntity().getContent()));

			StringBuffer sb = new StringBuffer("");
			String line = "";
			String NL = System.getProperty("line.separator");
			while ((line = in.readLine()) != null) {
				sb.append(line + NL);
			}
			in.close();

			String  result = sb.toString();
			System.out.println("XXXREMOTE: result="+result);

			return result;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			//return e.toString();
			String s="Cannot connect to "+url+" to exec <"+cmd+">";
			System.err.println(s);
			CSLLogger.instance.error(s);
			return s;
		}



	}
	/*
	 * class LogStreamReader implements Runnable {

    private BufferedReader reader;

    public LogStreamReader(InputStream is) {
        this.reader = new BufferedReader(new InputStreamReader(is));
    }

    public void run() {
        try {
            String line = reader.readLine();
            while (line != null) {
                System.out.println(line);
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
	 */


	public static long getPID() {
		String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
		if (processName != null && processName.length() > 0) {
			try {
				return Long.parseLong(processName.split("@")[0]);
			}
			catch (Exception e) {
				return 0;
			}
		}

		return 0;
	}


	public static void main(String[] args) {

		String[] tokens="aeae-aratr".split(";");

		for (int i=0;i<tokens.length;i++)
			System.out.println(tokens[i]);

		System.out.println(System.getProperty("user.dir"));

		String dir ="/Users/flausj/Documents/dev";
		String jarFile="Logview.jar";

		System.out.println(getPID());
		//String dir=System.getProperty("user.dir");


		System.out.println(dir);

		ProcessUtil.listJavaProcesses();

		Json j= Json.object();
		j.set("dir","");
		j.set("mode","jar"); // jar, script
		j.set("sudo", true);
		j.set("jarfile","");
		j.set("script","");
		j.set("kill_filter","modbuspal");
		j.set("kill_script", "");



		ProcessUtil.killProcess("main.ProcessUtil;test");
		ProcessUtil.killProcess("Logview.jar");
		//ProcessUtil.startJar(dir, jarFile,false);

		//ProcessUtil.killProcess("modbuspal.jar");
		ProcessUtil.killProcess("modbuspal.jar;Logview.jar");

		dir="/Users/flausj/Documents/dev";

		dir=System.getProperty("user.dir");
		String scriptName="start_modbuspal.sh";
		ProcessUtil.startScript(dir, scriptName,"modbuspal.jar",true, true, true);


	}
}
