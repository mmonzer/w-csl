package main.demo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import com.csl.core.CSLContext;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;


public class Test03TestCtrl {

    public static void sendEncapsulatedMsg(String host, int port, String msg) {
		try {

			byte[] message = msg.getBytes();

			// Get the internet address of the specified host
			InetAddress address = InetAddress.getByName(host);

			// Initialize a datagram packet with data and address
			DatagramPacket packet = new DatagramPacket(message, message.length,
					address, port);

			// Create a datagram socket, send the packet through it, close it.
			try (DatagramSocket dsocket = new DatagramSocket()) {
				dsocket.send(packet);
			}
		} catch (Exception e) {
		}
	}
	
	// to be used by module
		// host, port is the target name and port
		public static void sendObjectTo(String host,int port,String idOdTarget,int flowNumber,  Json objectToSend,
				boolean acquit)  {
			String msg=encapsulateObject(idOdTarget, flowNumber ,  objectToSend,acquit).toString();
			sendEncapsulatedMsg(host, port, msg);
		}

		

		public static Json encapsulateObject(String targetID, int n,Json j,boolean acquit) {
			Json obj =Json.object();
			obj.at("fromPort",CSLContext.getInstance().getCslUDPServer().getCurrentPortForUDP());
			obj.at("idOfTarget", targetID);
			obj.at("flowNumber", ""+n);
			//obj.at("nameOfVariable", varName);

			if (acquit) obj.at("fromPort",CSLContext.getInstance().getCslUDPServer().getCurrentPortForUDP());

			obj.at("data",j);

			return obj;

		}

	public static void main(String[] args) throws IOException {
        String str = "{\"type\":\"CTRL\",\"cmd\":\"stop\"}";
        Json j= Json.read(str);
		System.out.println(JsonUtil.prettyPrint(j));
			String host="localhost";
			int port=8001;
			
			
			for (int i=0; i<10; i++) {
				
				sendObjectTo(host, port, "cible", 1, j, false);

			}

		
	}
}

