package com.csl.logger;

import com.csl.alert.CSLAlertManager;
import com.csl.intercom.jsoncmd.JServiceLoader;
import com.ucsl.interfaces.IFileLog;
import com.ucsl.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.LongSupplier;



public class FileLog implements Runnable, IFileLog {
	/**
	 * Logger instance for this class.
	 */
	private static final Logger logger = LoggerFactory.getLogger(FileLog.class);

	private boolean running=false;


	static public int DEFAULT_MAX_SIZE=10000000;
	private Path pathLogFile = null;
	private  BufferedWriter writerLogFile = null;
	String prefixFileName="";
	String ext="txt";

	File fileToLog;
	String nameFile="";

	protected BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<String>(10);
	private long max_size_of_log_files=DEFAULT_MAX_SIZE;
	String dataDir=".";

	private boolean addTimeStampToFilename=true;

	private final LongSupplier getSystemCurrentTimeMillis;
	

	public  FileLog(String dataDir,String prefixFile,long maxSize, LongSupplier getSystemCurrentTimeMillis) {
		this.prefixFileName=prefixFile;
		this.dataDir=JServiceLoader.buildFullPathInUserDir(dataDir);

		JServiceLoader.displayInfo("Creation of logs <"+prefixFile+">  in "+this.dataDir); // System.out.println("[DIRECTORY   ]);
		
		this.getSystemCurrentTimeMillis=getSystemCurrentTimeMillis;
		
		this.max_size_of_log_files=maxSize;
		builFileName();
		
	}


	public  FileLog(String dataDir,String prefixFile ,LongSupplier getSystemCurrentTimeMillis) {
		this(dataDir,prefixFile,DEFAULT_MAX_SIZE, getSystemCurrentTimeMillis);
	}

	public  FileLog(String dataDir,String prefixFile,String ext,boolean addTimeStampToFileName, LongSupplier getSystemCurrentTimeMillis ) {

		this.addTimeStampToFilename=addTimeStampToFileName;
		this.prefixFileName=prefixFile;
		this.dataDir=dataDir;
		this.ext=ext;

		this.getSystemCurrentTimeMillis=getSystemCurrentTimeMillis;
		
		builFileName();

	}

	public  String getLogFilePath() {
		return pathLogFile.toAbsolutePath().toString();
	}





	@Override
	public void run() {
		try {

			while(true){
				String buffer = blockingQueue.take();
				//Check whether end of file has been reached

				if(buffer.equals("EOF")){ 
					break;
				}
				SendLogMessageToFile(buffer);
			}               


		} catch(InterruptedException e){

		}finally{
			TerminateLogging();
		} 

	}

	public void builFileName() {
		nameFile = prefixFileName;
		if (addTimeStampToFilename) {
			nameFile=nameFile+'_'+ Instant.now().atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_INSTANT).replaceAll("\\D+", "_");
			if (nameFile.endsWith("_")) nameFile=nameFile.substring(0, nameFile.length()-1);
			nameFile=nameFile+ "."+ext;

		}
		else
			nameFile=nameFile+"."+ext;


	}

	public void InitializeLogging() {
		// Use the current timestamp with non-numeric characters replaced by underscores.
		builFileName();

		pathLogFile = Paths.get(dataDir+ File.separator + nameFile);

		new File(dataDir).mkdirs();

		try {
			fileToLog= new File(pathLogFile.toString());

			writerLogFile = Files.newBufferedWriter(pathLogFile);

			//Start listening for alerts.
		} catch (IOException ex) {
			logger.error( "This session cannot be logged to disk: " + ex.getMessage());
		}

	}

	private void startLog() {
		running=true;
		InitializeLogging();
		new Thread(this).start();

	}

	public  void TerminateLogging() {
		if(writerLogFile != null) {
			try {
				writerLogFile.flush();
				writerLogFile.close();
			} catch (final Exception ex) {
				System.err.println("Error shutting down logging subsystem: " + ex.getMessage());
			} finally {
				writerLogFile = null;
			}
		}
	}
	public  void RecordLogMessage(final String message) {
		if (!running) {
			startLog();
		}
		try {

			String line;

			long t= this.getSystemCurrentTimeMillis.getAsLong();
			line =""+t +':'+ message+"\r\n";

			blockingQueue.put(line);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public  void addMessageToFile(final String message) {
		if (!running) startLog();
		try {
			blockingQueue.put(message);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void SendLogMessageToFile(final String message) {
		if (fileToLog.length()>this. max_size_of_log_files) {
			TerminateLogging();
			InitializeLogging();
		}

		if(writerLogFile != null) {
			String line;
			line=message;

			try {
				writerLogFile.write(line);
				writerLogFile.flush();
				double bytes = fileToLog.length();
			} catch(final IOException ex) {
				System.err.println("Unable to write message message to disk:\n" + line);
			}

		}
	}

	public void send(Json j) {
		RecordLogMessage(j.toString());
	}

	public void send(String s) {
		RecordLogMessage(s);
	}




}

