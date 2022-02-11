package com.csl.devdb;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.csl.core.CSLContext;
import com.csl.logger.CSLLogger;
import com.xcsl.json.Json;
import com.xcsl.json.JsonUtil;
import com.xcsl.util.FileUtils;

public class DBHisto {

	//public static String SUBDIR_CURRENT="current";
	
	
	List<Json> histo = new ArrayList<>();
	
	
	static public int DEFAULT_MAX_SIZE=10000000;
	private long max_size_of_log_files=DEFAULT_MAX_SIZE;
	
	private Path pathLogFile = null;
	private  BufferedWriter writerLogFile = null;
	String prefixFileName="";
	String ext="txt";

	File fileToLog;
	String nameFile="";

	String dataDir=".";
	
	String lastTimeStamp="";
	
	private boolean addTimeStampToFilename=true;


	private String dbFilename="";


	private String dirBackup;


	private String dirCurrent;

	
	void addTransaction(Json t) {
		histo.add(t);
		sendToFile(t.toString()+'\n');
	}
	
	
	public void builFileName() {
		nameFile = prefixFileName;
		if (addTimeStampToFilename) {
			this.lastTimeStamp=Instant.now().atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_INSTANT).replaceAll("\\D+", "_");
			if (lastTimeStamp.endsWith("_")) lastTimeStamp=lastTimeStamp.substring(0, lastTimeStamp.length()-1);
			
			nameFile=nameFile+'_'+ lastTimeStamp;
			if (nameFile.endsWith("_")) nameFile=nameFile.substring(0, nameFile.length()-1);
			nameFile=nameFile+ "."+ext;

		}
		else
			nameFile=nameFile+"."+ext;


	}
	
	void dump() {
		
		System.out.println("\n\n");
		System.out.println("HISTO");
		for (Json j :histo)
			System.out.println(j);
	}
	
	public DBHisto(String dir_histo, String dbfilename, String dirCurrent, String dirBackup) { 
	
			
			
			this.addTimeStampToFilename=true;
			this.prefixFileName="DBHISTO";
			this.ext="txt";

			this.dbFilename= dbfilename;
			this.dirBackup=dirBackup;
			this.dirCurrent=dirCurrent;
			this.dataDir=
					CSLContext.instance.buildFullPathInConfDir(dir_histo)+File.separator+dirCurrent;
					
			
			InitializeLogging();
			

	}
	
	

	public String getLastTimeStamp() {
		return lastTimeStamp;
	}


	private void sendToFile(final String message) {

		//	System.out.println("log to"+this.pathLogFile+" :"+message);
		if (fileToLog.length()>this. max_size_of_log_files) {
			TerminateLogging();
			InitializeLogging();
		}

		if(writerLogFile != null) {
			//String line = String.format("[%s] DEBUG - %s\r\n", Instant.now().atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_INSTANT), message);
			String line; // = String.format("[%s] %s\r\n", Instant.now().atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_INSTANT), message);
			//line =""+System.currentTimeMillis()+':'+ message+"\r\n";
			line=message;

			try {
				writerLogFile.write(line);
				writerLogFile.flush();
				double bytes = fileToLog.length();
				//System.out.println(bytes);
			} catch(final IOException ex) {
				System.err.println("Unable to write message message to disk:\n" + line);
			}

		}
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
	
	
	public void InitializeLogging() {
		
		builFileName();

		pathLogFile = Paths.get(dataDir+ //  CSLContext.context.getParamAsString("data_dir", "myappdata")+
				File.separator + nameFile);
		
		new File(dataDir).mkdirs();

		try {
			fileToLog= new File(pathLogFile.toString());

			writerLogFile = Files.newBufferedWriter(pathLogFile);
			
		} catch (IOException ex) {
			CSLLogger.instance.error( "This session cannot be logged to disk: " + ex.getMessage());
		}


	}


	public void backupHistoTo(String dir) {
		// TODO Auto-generated method stub
		TerminateLogging();
		try {
			copyDirectory(dataDir, dir);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		InitializeLogging();
	}

	
	private void moveFile(File source, File dest) throws IOException  {
		
		System.out.println("MOVE FILE :"+source+"-->"+dest);
	    //Files.copy(source.toPath(), dest.toPath());
	    Files.move(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
		
	}
	
	
	private  void copyDirectory(String sourceDirectoryName, String destinationDirectoryName) throws IOException {
	   
		
		File sourceDirectory= new File(sourceDirectoryName);
		File destinationDirectory = new File(destinationDirectoryName);
		
		if (!destinationDirectory.exists()) {
	        destinationDirectory.mkdir();
	    }
		
	    for (String f : sourceDirectory.list()) {
	    	if ( f.startsWith(this.prefixFileName)||f.startsWith(this.dbFilename) ) {
	    		File fSource = new File(sourceDirectory, f);
	    		File fDest = new File(destinationDirectory, f);
	    		if (!fSource.isDirectory()) {
	    			moveFile(fSource, fDest);
	    		}
	    	}
	    }
	}
	
	
	public Json backupFiles() {

		// System.out.println(j);

		String dir = CSLContext.instance.buildFullPathInConfDir(dirBackup);
		File d= new File(dir); d.mkdirs();
		String ts=FileUtils.getTimeStamp();
		//String filename=FILE_NAME_DEVICES+'_'+ts+ ".json";
		
		dir =dir+File.separator+"db"+ts;
		//String path = dir+File.separator+filename;
		
		backupHistoTo(dir);
		
		try {
			
			if (hasInitialDatabaseImageInCurrent()) {
				System.out.println("COPY DATABSE IMAGE "+dataDir+File.separator+getFullDBFileNameForInitialImage()+ "-->"+dir+File.separator+getFullDBFileNameForInitialImage());
				moveFile(
					new File(dataDir+File.separator+getFullDBFileNameForInitialImage()),
					new File(dir+File.separator+getFullDBFileNameForInitialImage()) );
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return Json.object().set("dir",dir).set("timestamp", ts);
	}

	
	
	public Json saveDatabase(String fileName, Json contents) {
	
		boolean pretty=true;
		
		System.out.println("SAVE DATABSE IMAGE "+dataDir+File.separator+getFullDBFileNameForInitialImage());
		
		String result="";
		Json r=Json.object();
		
		String s="";
		if (pretty)
				s=JsonUtil.prettyPrint(contents);
		else
				s=contents.toString();

		if ((fileName!=null)&&(contents!=null)) {
				result=writeDataFile(fileName, s);
				r.set("result","ok");


		} else {
				if (fileName==null) r.set("error", "Invalid file name ");
				if (contents==null) r.set("error","Invalid contents ");

		}
		return r;
	}
	
	
	private  String writeDataFile(String path, String content) {

		
		try {

			Files.write(Paths.get(path), content.getBytes());
			return "ok";
		} catch (IOException e) {
			e.printStackTrace();
			return "error";
		}

	}

	private String getFullDBFileNameForInitialImage() {
		return dbFilename+"_0.json";
	}
	
	private boolean hasInitialDatabaseImageInCurrent() {
		
		File f= new File(dataDir+File.separator+getFullDBFileNameForInitialImage());
		return f.exists();
	}
	
	/*
	 * 
	 * init database image
	 * 
	 *  backup current dir
	 * 	clear files
	 * 	backup database at initial time in current dir
	 *  
	 */

	public Json initImage(Json databaseContents, boolean force) {
		// TODO Auto-generated method stub
		
		if (!force&&hasInitialDatabaseImageInCurrent()) return Json.object().set("info", "No image created"); // use current image
		
		
		Json result = backupFiles();
		saveDatabase(dataDir+File.separator+getFullDBFileNameForInitialImage(), databaseContents);
		
		return result;
		
	}

	
}
