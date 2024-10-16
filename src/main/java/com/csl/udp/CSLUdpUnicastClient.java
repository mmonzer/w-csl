package com.csl.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;

import com.csl.core.Config;
import com.csl.util.CorrelationUtils;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.csl.logger.LoggerUtils.traceAlertReceived;

/**
 * Class for a UDP client that listens to the given ip address and port, and adds the received message into
 * the messageQueue to be treated later.
 */
public class CSLUdpUnicastClient implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(CSLUdpUnicastClient.class);
	private final int port;
	private String ip="";
	private final BlockingQueue<CorrelatedMessage> messageQueue;
	DatagramSocket clientSocket=null;
	
	boolean closing=false;
	private boolean traceAll;

	/**
	 * Client UDP that listens to the given ip address and port, and adds the received message into the messageQueue.
	 * If traceAll is true, it will also print them on the screen.
	 * @param ip ip address for UDP listening
	 * @param port port for UDP listening
	 * @param messageQueue list to add the incoming messages
	 * @param traceAll true if we want to have the incoming messages written on the terminal.
	 */
	public CSLUdpUnicastClient(String ip,int port, BlockingQueue<CorrelatedMessage> messageQueue, boolean traceAll) {
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

	/**
	 * Function that runs in a thread which reads the incoming data and appends it to the messageQueue
	 */
	@Override
	public void run() {
		/**
		 * Bind the client socket to the port on which you expect to
		 * read incoming messages
		 */
		
		try {
			//System.out.println("XXXJMFUDP on "+ip+":"+port);
			
			clientSocket = new DatagramSocket(port,Inet4Address.getByName(ip)) ;

			// Set a timeout of 3000 ms for the client.
			//clientSocket.setSoTimeout(3000);
			while (true) {
				String xCorrelationId = CorrelationUtils.setXCorrelationId();
				CorrelationUtils.setEndpoint("/alerts");

				
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
				traceAlertReceived(logger, Config.instance.TapService.getLocalIpAddress(), Config.instance.TapService.getLocalPort(), "/alerts", "UDP");

				/**
				 * Add the data contained in the datagram packet to the message
				 * queue.The 'put' method will block if the message queue is full,
				 * until there is space to store the new message.
				 */
				if (traceAll) {System.out.println ("new message received "+data(datagramPacket.getData()));}
				this.messageQueue.put(new CorrelatedMessage(xCorrelationId, datagramPacket.getData()));
				//System.out.println('.');
			}
		} catch (SocketException e) {
			if (!closing) e.printStackTrace();
		} catch (IOException e) {
			logger.info("Timeout. Client is closing.");
		} catch (InterruptedException e) {
			e.printStackTrace();
			clientSocket.disconnect();
			clientSocket.close();
		}
	}

	@Getter
    static
    class CorrelatedMessage {
        byte[] bytes;
		String xCorrelationId;

		public CorrelatedMessage(String xCorrelationId, byte[] bytes) {
			this.bytes = bytes;
			this.xCorrelationId = xCorrelationId;
		}
    }
}