package main.demo;
import java.net.URI;
import java.net.URISyntaxException;

import main.xcom.WebsocketClientEndpoint;

public class CSLDemo06WebSocket {
	
	
	static public void listenAlert() {
		
		
		 try {
	            // open websocket
	        	
	        	
	        	String s= "ws://" + "127.0.0.1" + ":" + "8000" + "/alerts";
	        	
	            final WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(new URI(s)); //"wss://real.okcoin.cn:10440/websocket/okcoinapi"));

	            // add listener
	            clientEndPoint.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
	                public void handleMessage(String message) {
	                    System.out.println("Alert:"+message);
	                }
	            });

	               // wait 5 seconds for messages from websocket
	            Thread.sleep(5000);

	        } catch (InterruptedException ex) {
	            System.err.println("InterruptedException exception: " + ex.getMessage());
	        } catch (URISyntaxException ex) {
	            System.err.println("URISyntaxException exception: " + ex.getMessage());
	        }
	}
	
	
	static public void listenDatabase() {
		
		
		 try {
	            // open websocket
	        	
	        	
	        	String s= "ws://" + "127.0.0.1" + ":" + "8000" + "/database";
	        	
	            final WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(new URI(s)); 

	            // add listener
	            clientEndPoint.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
	                public void handleMessage(String message) {
	                    System.out.println("Database:"+message);
	                }
	            });

	            Thread.sleep(5000);


	        } catch (InterruptedException ex) {
	            System.err.println("InterruptedException exception: " + ex.getMessage());
	        } catch (URISyntaxException ex) {
	            System.err.println("URISyntaxException exception: " + ex.getMessage());
	        }
	}


    public static void main(String[] args) {
       
    	
    	listenAlert();
    	listenDatabase();
    	//List<String> ExternalCommands
    }
}