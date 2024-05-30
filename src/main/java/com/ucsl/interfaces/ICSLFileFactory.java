package com.ucsl.interfaces;

import java.util.List;

public interface ICSLFileFactory {

	ICSLFile createICSLFile();
	List<String> getAllFilesInDir(String dir, String filePrefix);

	
}
