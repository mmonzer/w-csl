package com.csl.defaultclasses;

import com.ucsl.interfaces.ICSLFile;
import com.ucsl.interfaces.ICSLFileFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
