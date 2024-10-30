package main.demo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import com.csl.core.CSLContext;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;


public class Test02Suricata {
	private static DatagramSocket socket;
	private static InetAddress address;
	private static String str = "{\"type\":\"EVT\",\"timestamp\":\"2020-12-15T11:45:27.674926+0100\",\"flow_id\":1973389627968426,\"in_iface\":\"enp0s3\",\"event_type\":\"alert\",\"src_ip\":\"192.168.0.150\",\"src_port\":37440,\"dest_ip\":\"192.168.0.1\",\"dest_port\":502,\"proto\":\"TCP\",\"alert\":{\"action\":\"allowed\",\"gid\":1,\"signature_id\":2405007,\"rev\":0,\"signature\":\"#COM xCommunication non autorisee dans la PoC\",\"category\":\"\",\"severity\":3},\"flow\":{\"pkts_toserver\":2,\"pkts_toclient\":1,\"bytes_toserver\":166,\"bytes_toclient\":66,\"start\":\"2020-12-15T11:45:27.674730+0100\"}}";

	
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
		
		
		Json j= Json.read(str);
		System.out.println(JsonUtil.prettyPrint(j));
		
		if(args.length != 2) {
			System.out.println("Utilisation du jar :");
			System.out.println("sudo java -jar <file>.jar <Ip adress for UDP redirecting> <UDP port>");
		}
		else {
			
			String host="localhost";
			int port=8001;
			
			
			for (int i=0; i<10; i++) {
				
				sendObjectTo(host, port, "cible", 1, j, false);

			}

		}
	}
}

