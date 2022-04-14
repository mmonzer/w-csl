package com.csl.udp;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;

import com.csl.core.CSLContext;

public class CSLUdpUnicastClient implements Runnable {
	private final int port;
	private final BlockingQueue<byte[]> messageQueue;
	DatagramSocket clientSocket=null;
	
	boolean closing=false;
	
	public CSLUdpUnicastClient(int port, BlockingQueue<byte[]> messageQueue) {
		this.port = port;
		this.messageQueue = messageQueue;
	}

	
	public void stop() {
		//clientSocket.disconnect();
		closing=true;
		clientSocket.close();
		
	}
	
	@Override
	public void run() {
		/**
		 * Bind the client socket to the port on which you expect to
		 * read incoming messages
		 */
		
		try {
			//clientSocket = new DatagramSocket(port) ;
			clientSocket = new DatagramSocket(port); //,Inet4Address.getByName("127.0.0.1")) ;
			
			// Set a timeout of 3000 ms for the client.
			//clientSocket.setSoTimeout(3000);
			while (true) {
				
				
				//System.out.println("JMF_receive packet");
				/**
				 * Create a byte array buffer to store incoming data. If the message length
				 * exceeds the length of your buffer, then the message will be truncated. To avoid this,
				 * you can simply instantiate the buffer with the maximum UDP packet size, which
				 * is 65506
				 */
				byte[] buffer = new byte[65507];
				DatagramPacket datagramPacket = new DatagramPacket(buffer, 0, buffer.length);

				/**
				 * The receive method will wait for 3000 ms for data.
				 * After that, the client will throw a timeout exception.
				 */
				clientSocket.receive(datagramPacket);

				/**
				 * Add the data contained in the datagram packet to the message
				 * queue.The 'put' method will block if the message queue is full,
				 * until there is space to store the new message.
				 */
				//CSLContext.context.logInfo("new message received "+datagramPacket.getData());
				this.messageQueue.put(datagramPacket.getData());
				System.out.println('.');
			}
		} catch (SocketException e) {
			if (!closing) e.printStackTrace();
		} catch (IOException e) {
			CSLContext.instance.logInfo("Timeout. Client is closing.");
		} catch (InterruptedException e) {
			e.printStackTrace();
			clientSocket.disconnect();
			clientSocket.close();
		}
	}
}