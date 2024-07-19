package com.csl.defaultclasses;

import com.csl.util.FileUtils;
import com.ucsl.interfaces.IFileStoreService;
import com.ucsl.json.Json;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class FileStoreService implements IFileStoreService {
	
	boolean trace=true;
	
	@Getter
    String cslConfDir=".";
	
	public FileStoreService(String cslConf) {
		this.cslConfDir=cslConf;
	}

	@Override
	public Json readJsonFromFile(String dir, String fileName) {
		if (trace) System.out.println("[FILESERVICE] readJson"+dir+" "+fileName);
		 
		return FileUtils.readJsonFromFile(dir, fileName);
	}

	@Override
	public void saveJsonToFile(String dir, String fileName, Json j) {
		if (trace) System.out.println("[FILESERVICE] saveJson"+dir+" "+fileName);
		
		FileUtils.saveJsonToFile(dir, fileName, j);
	}

	@Override
	public String buildFullPathInConfDir(String dir) {
		if (trace) System.out.println("[FILESERVICE] buildFullPath :"+dir);

		if (dir==null) dir ="";
		dir=dir.replace('\\','/');

		dir=clean(dir);

		if (dir.startsWith(".")) dir =dir.substring(1);
		if (dir.startsWith(File.separator)) dir =dir.substring(1);

		return getCslConfDir()+File.separator+dir;
	}
	
	private String clean(String s) {
		String z="../";
		while (s.indexOf(z)>=0) {
			int n= s.indexOf(z);
			String s1=s.substring(0,n);
			String s2=s.substring(n+z.length(),s.length());
			s=s1+s2;
		}
		return s;
	}

    @Override
	public String sanitizeDirPath(String s) {
		if (trace) System.out.println("[FILESERVICE] sanitizeDirPath :"+s);
		
		return FileUtils.sanitizeDirPath(s);
	}

	@Override
	public boolean fileExists(String dir, String fileName) {
		return FileUtils.fileExists(dir, fileName);
	}

	@Override
	public boolean fileExists(String fileName) {
		return FileUtils.fileExists(fileName);
	}
	
	@Override
	public boolean dirExists(String fileName) {
		return FileUtils.dirExists(fileName);
	}

	@Override
	public Json writeFileFromText(String filename, String text) {
		if (trace) System.out.println("[FILESERVICE] writefile :"+filename);
		
		return FileUtils.writeFileFromText(filename, text);
	}

	@Override
	public Json readFileInAJsonText(String filename) {
		if (trace) System.out.println("[FILESERVICE] read Json :"+filename);

		return FileUtils.readFileInAJsonText(filename);
	}

	@Override
	public String readFile(String filename) throws IOException {
		if (trace) System.out.println("[FILESERVICE] read file :"+filename);
		
		return FileUtils.readFile(filename);
	}
	
	@Override
	public List<String> readFileAsStringList(String filename) throws IOException {
		if (trace) System.out.println("[FILESERVICE] read file :"+filename);
		
		return FileUtils.readFileAsStringList(filename);
	}

	@Override
	public String writeFile(String filename, String contents) throws IOException {
		if (trace) System.out.println("[FILESERVICE] write file :"+filename);
		
		return FileUtils.writeFile(filename, contents);
	}

	@Override
	public void backupFileWithTimeStamp(String filePath, String idsModelDirBackup) {
		FileUtils.backupFileWithTimeStamp(filePath, idsModelDirBackup);
	}
	@Override
	public void reverseToLastBackupFile(String filePath) {
		FileUtils.reverseToLastBackupFile(filePath);
	}
	
	@Override
	public String checkAndCreateDir(String s) {
		return FileUtils.checkAndCreateDir(s);
	}

	@Override
	public void renameFileWithTimeStamp(String filePath, String string) {
		FileUtils.renameFileWithTimeStamp(filePath, string);
	}

	private boolean deleteDirectory(File directoryToBeDeleted) {
	    File[] allContents = directoryToBeDeleted.listFiles();
	    if (allContents != null) {
	        for (File file : allContents) {
	            deleteDirectory(file);
	        }
	    }
	    return directoryToBeDeleted.delete();
	}

	@Override
	public boolean deleteDirectory(String rootDir, String name) {
		File file = new File(name);
		return deleteDirectory(file);
	}

	@Override
	public boolean deleteFile(String rootDir, String name) {
		File file = new File(name);
		return file.delete();
	}
	
	@Override
	public void createSubDir(String rootDir,String subDir) {
		File file = new File(rootDir+FileUtils.fileSeparator+subDir);
		if (!file.exists()) file.mkdirs();
	}

	@Override
	public boolean renameSubDir(String rootDir, String dsrc, String ddst) {
		String fsrc=rootDir+dsrc;
		String fdst=rootDir+ddst;
	
		File fileToMove = new File(fsrc);
	    boolean isMoved = fileToMove.renameTo(new File(fdst));
	    return isMoved;
	}
	
}
