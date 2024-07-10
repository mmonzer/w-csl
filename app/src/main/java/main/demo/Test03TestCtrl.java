package main.demo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import com.csl.core.CSLContext;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;


public class Test03TestCtrl {





	private static DatagramSocket socket;
	private static InetAddress address;
	private static String str = "{\"type\":\"CTRL\",\"cmd\":\"stop\"}";

	
	static public void sendEncapsulatedMsg(String host, int port, String msg) {
		CSLContext.instance.logInfo("   sending to "+host+':'+port+" >>>> msg:"+msg);
		try {
			//String host = "localhost";
			//int port = 9000;

			byte[] message = msg.getBytes();

			// Get the internet address of the specified host
			InetAddress address = InetAddress.getByName(host);

			// Initialize a datagram packet with data and address
			DatagramPacket packet = new DatagramPacket(message, message.length,
					address, port);

			// Create a datagram socket, send the packet through it, close it.
			DatagramSocket dsocket = new DatagramSocket();
			dsocket.send(packet);
			dsocket.close();
		} catch (Exception e) {
			//System.err.println(e);
		}
	}
	
	// to be used by module
		// host, port is the target name and port
		static public void sendObjectTo(String host,int port,String idOdTarget,int flowNumber,  Json objectToSend,
				boolean acquit)  {

			//String msg=encapsulateObject(idOdTarget, flowNumber, varName, objectToSend );
			String msg=encapsulateObject(idOdTarget, flowNumber ,  objectToSend,acquit).toString();
			sendEncapsulatedMsg(host, port, msg);
		}

		

		static public Json encapsulateObject(String targetID, int n,Json j,boolean acquit) {
			Json obj =Json.object();
			obj.at("fromPort",CSLContext.instance.getCslUDPServer().getCurrentPortForUCP());
			obj.at("idOfTarget", targetID);
			obj.at("flowNumber", ""+n);
			//obj.at("nameOfVariable", varName);

			if (acquit) obj.at("fromPort",CSLContext.instance.getCslUDPServer().getCurrentPortForUCP());

			obj.at("data",j);

			return obj;

		}

	public static void main(String[] args) throws IOException {
		
		
		Json j= Json.read(str);
		System.out.println(JsonUtil.prettyPrint(j));
	//	sendEncapsulatedMsg("localhost",8001,"test");
		
		
			
			String host="localhost";
			int port=8001;
			
			
			for (int i=0; i<10; i++) {
				
				sendObjectTo(host, port, "cible", 1, j, false);

			}

		
	}
}

