package com.csl.udp;

import com.csl.core.CSLContext;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;

public class CSLUdpUnicastClient implements Runnable {
	private final int port;
	private String ip="";
	private final BlockingQueue<byte[]> messageQueue;
	DatagramSocket clientSocket=null;
	
	boolean closing=false;
	private final boolean traceAll;
	
	public CSLUdpUnicastClient(String ip,int port, BlockingQueue<byte[]> messageQueue, boolean traceAll) {
		this.ip=ip;
		this.port = port;
		this.messageQueue = messageQueue;
		this.traceAll=traceAll;
	}

	
	public void stop() {
		closing=true;
		clientSocket.close();
		
	}
	
	
	 // A utility method to convert the byte array
    // data into a string representation.
    public static StringBuilder data(byte[] a)
    {
        if (a == null)
            return null;
        StringBuilder ret = new StringBuilder();
        int i = 0;
        while (a[i] != 0)
        {
            ret.append((char) a[i]);
            i++;
        }
        return ret;
    }
	
	@Override
	public void run() {
		/**
		 * Bind the client socket to the port on which you expect to
		 * read incoming messages
		 */
		
		try {
			System.out.println("XXXJMFUDP on "+ip+":"+port);
			
			clientSocket = new DatagramSocket(port,Inet4Address.getByName(ip)) ;

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
				if (traceAll) System.out.println ("new message received "+data(datagramPacket.getData()));
				this.messageQueue.put(datagramPacket.getData());
				//System.out.println('.');
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