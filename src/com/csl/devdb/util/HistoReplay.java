package com.csl.devdb.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.xcsl.json.Json;

public class HistoReplay {

	boolean DEBUG=false;
	boolean verbose=false;

	List<String> listofFiles =new ArrayList<String>();

	int currentFileIndex=-1;

	File currentOpenFile=null;
	BufferedReader b;

	Json currentObject=null;
	private String currentLine=null;


	boolean first=true;
	boolean end =false;

	String filePrefix="???";
	String workinDir="";

	


	static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	public HistoReplay(String dir, String prefix) {
		this.workinDir=dir;
		this.filePrefix=prefix;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public Json getCurrentTransaction() {
		return currentObject;
	}

	

	public List<String> getAllFilesFromDir(String dir, String filePrefix) {

		List<String> listOfFile= new ArrayList<String>();


		File folder = new File(dir);
		File[] listOfFiles = folder.listFiles();

		if (listOfFiles==null) return listOfFile;


		for (File file : listOfFiles) {
			if (file.isFile()) {
				if (file.getName().startsWith(filePrefix)) {
					listOfFile.add(file.getAbsolutePath());
				}
				System.out.println(file.getName());
			}
		}


		return listOfFile;
	}



	
	public boolean openFile() {

		try {
			if (b!=null) {
				b.close();
			}

			if (currentFileIndex<0) {
				this.listofFiles=getAllFilesFromDir(this.workinDir, filePrefix);
				if (listofFiles.size()==0) return false;
				currentFileIndex=0;
			}
			else {
				currentFileIndex++;
			}
			if (currentFileIndex>=listofFiles.size()) return false;

			if (DEBUG) System.out.println("**** opening file "+listofFiles.get(currentFileIndex));

			if (verbose) System.out.println("Reading file :"+listofFiles.get(currentFileIndex));
			this.currentOpenFile= new File(listofFiles.get(currentFileIndex));
			this.b = new BufferedReader(new FileReader(currentOpenFile));
			return true;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

	}


	public boolean next() {

		try {
			if (b==null) {
				boolean result=openFile();
				if (!result) {	// canot not open file
					end=true;
					return false;
				}
			}

			String readLine = "";

			while ((readLine = b.readLine()) == null) {
				//System.out.println(readLine);
				boolean result=openFile();
				if (!result) {end=true; return false; } //null; }
			}

			this.currentLine=readLine;
			parseCurrentLine();

			return true;// eadLine;

		} catch (IOException e) {
			e.printStackTrace();
			end=true;
			return false; //;

		}
	}


	

	void parseCurrentLine() {

		if (currentLine==null) {
			currentObject=null;
			
		}	
		
		currentObject= Json.read(currentLine);
		
		first=false;
	}




	public static void main(String[] args) {


		HistoReplay jo= new HistoReplay("/Users/flausj/Documents/dev/_csl/tests/testdb/cslconf/db_backup/db20210728_102449", "DBHISTO");

		String s;
		boolean hasNext=true;
		do {
			hasNext=jo.next();
			if (hasNext) {
				System.out.println( jo.getCurrentTransaction());
			}
			//System.out.println(s);
		} while (hasNext);
	}
}

