package com.csl.defaultclasses;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


import com.xcsl.interfaces.ICSLFile;
import com.xcsl.interfaces.ICSLFileFactory;

public class CSLFileFactory implements ICSLFileFactory {

	
	@Override
	public List<String> getAllFilesInDir(String dir, String filePrefix) {
	
			List<String> listOfFile= new ArrayList<String>();


			File folder = new File(dir);
			File[] listOfFiles = folder.listFiles();

			if (listOfFiles==null) return listOfFile;


			for (File file : listOfFiles) {
				if (file.isFile()) {
					if (file.getName().startsWith(filePrefix)) {
						listOfFile.add(file.getAbsolutePath());
					}
					//println(file.getName());
				}
			}


			return listOfFile;
		

	}

	@Override
	public ICSLFile createICSLFile() {
		// TODO Auto-generated method stub
		return new CSLFile();
	}

}
