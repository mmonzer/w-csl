package com.csl.util;

import com.ucsl.json.Json;
import com.ucsl.util.FileUtils;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class LineCounter {

	@Setter
    boolean verbose=false;

	List<String> toExclude=null;
	private final String filePostfix;

	


	static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	public LineCounter(List<String> toExclude, String postfix) {
		this.toExclude=toExclude;
		this.filePostfix=postfix;
	}

    public boolean dirToExclude(String dir) {
		
		for (String s:toExclude) {
			if (dir.contains(s)) return true;
		}
		return false;
	}

	public Json getAllFilesFromDir(String dir, String filepostfix) {

		List<String> listOfFile= new ArrayList<String>();
		
		Json j=Json.object();
		
		int lines=0;
		int total=0;
		int n_char=0;
	
		
		System.out.println(dir);
		File folder = new File(dir);
		File[] listOfFiles =null;
		
		if (!dirToExclude(dir)) listOfFiles=folder.listFiles();

		if (listOfFiles==null) {
			j.set("name", dir);
			j.set("lines", lines);
			j.set("total", total);
			
			j.set("chars",  n_char);
			
			return j;
		}

	

		Json jl=Json.array();
		boolean empty=true;
		
		for (File file : listOfFiles) {
			Json jj=null;
			
			if (file.isFile()) {
				if (file.getName().endsWith(filePostfix)) {
					listOfFile.add(file.getAbsolutePath());
					jj=(count(file.getAbsolutePath()));
					
				}
				//System.out.println(file.getName());
			}
			if (file.isDirectory()) {
				jj=getAllFilesFromDir(file.getAbsolutePath(), filepostfix);
			}
			if (jj!=null) {
				jl.add(jj);
				lines=lines+jj.get("lines").asInteger();
				total=total+jj.get("total").asInteger();
				n_char=n_char+jj.get("chars").asInteger();
				empty=false;
				
			}
		}

		j.set("name", dir);
		j.set("lines", lines);
		j.set("total", total);
		
		j.set("chars",  n_char);
		j.set("contents",jl);
		

		return j;
	}


	
	public Json count(String fileName) {
		
		Json j= Json.object();
		File f=new File(fileName);
		
		int lines=0;
		int total=0;
		int n_char=0;
		
		try {
			
			
			BufferedReader b = new BufferedReader(new FileReader(fileName));
			
			String readLine="";
		
			
			while ((readLine = b.readLine()) != null) {
				//System.out.println(readLine);
				if (!readLine.isEmpty()) lines++;
				total++;
				n_char=n_char+readLine.trim().length();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		j.set("name", fileName);
		j.set("lines", lines);
		j.set("total", total);
		
		j.set("chars",  n_char);
		
		 
		return j;
	}
	
	
	

	public static void dump(Json j) {
		
		List<String> strlist=FileUtils.jsonToStringList("", j, new ArrayList<String>(), "");
		
		for (String s:strlist) {
			System.out.println(s);
		}
	}

	
	public Json countAll(List<String> list, String filepostfix) {
		
		Json j=Json.object();
		
		int lines=0;
		int total=0;
		int n_char=0;
	
		
		String listall="";
		Json jl=Json.array();
		
		
		for (String fileName : list) {
			if (!listall.isEmpty()) listall=listall+" ";
			listall=listall+fileName;
			
			File file= new File(fileName);
			
			Json jj=null;
			
			
			if (file.isDirectory()) {
				jj=getAllFilesFromDir(file.getAbsolutePath(), filepostfix);
			}
			if (jj!=null) {
				jl.add(jj);
				lines=lines+jj.get("lines").asInteger();
				total=total+jj.get("total").asInteger();
				n_char=n_char+jj.get("chars").asInteger();
				
				
			}
		}

		j.set("name", "ALL:"+listall);
		j.set("lines", lines);
		j.set("total", total);
		
		j.set("chars",  n_char);
		j.set("contents",jl);
		
		return j;
		
	}
}

