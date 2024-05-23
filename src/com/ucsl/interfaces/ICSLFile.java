package com.ucsl.interfaces;

import java.io.File;
import java.util.List;

public interface ICSLFile {
	
	static String separator=File.separator;

	public boolean open(String fileName);
	public String readLine();
	public void close();
	
	
	

}
