package com.csl.defaultclasses;

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


import com.ucsl.interfaces.IFileLog;
import com.ucsl.json.Json;
import com.wcsl.ids.IDSMainProcessor;



public class FileLog implements Runnable, IFileLog {

	private boolean running=false;

	static public int DEFAULT_MAX_SIZE=10000000;
    private  BufferedWriter writerLogFile = null;
	String prefixFileName="";
	String ext="txt";

	File fileToLog;
	String nameFile="";

	protected BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<String>(10);
	private long max_size_of_log_files=DEFAULT_MAX_SIZE;
	String dataDir=".";

    private final LongSupplier getSystemCurrentTimeMillis;

	public  FileLog(String dataDir,String prefixFile,long maxSize, LongSupplier getSystemCurrentTimeMillis) {
		this.getSystemCurrentTimeMillis=getSystemCurrentTimeMillis;
		this.dataDir=dataDir;
		this.prefixFileName=prefixFile;
		if (maxSize>1000)
			this.max_size_of_log_files=maxSize;
		
		
		builFileName();
		
	}

	public  FileLog(String dataDir,String prefixFile ,LongSupplier getSystemCurrentTimeMillis) {
		this(dataDir,prefixFile,DEFAULT_MAX_SIZE, getSystemCurrentTimeMillis);
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
        boolean addTimeStampToFilename = true;
        if (addTimeStampToFilename) {
			nameFile=nameFile+'_'+ Instant.now().atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_INSTANT).replaceAll("\\D+", "_");
			if (nameFile.endsWith("_")) nameFile=nameFile.substring(0, nameFile.length()-1);
			nameFile=nameFile+ "."+ext;

		}
		else
			nameFile=nameFile+"."+ext;


	}

	public void InitializeLogging() {

		builFileName();
        Path pathLogFile = Paths.get(dataDir + File.separator + nameFile);

		new File(dataDir).mkdirs();

		try {
			fileToLog= new File(pathLogFile.toString());

			writerLogFile = Files.newBufferedWriter(pathLogFile);
		} catch (IOException ex) {
			IDSMainProcessor.cslLogger().logError( "This session cannot be logged to disk: " + ex.getMessage());
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
			line =""+t /*System.currentTimeMillis()*/+':'+ message+"\r\n";

			blockingQueue.put(line);
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

