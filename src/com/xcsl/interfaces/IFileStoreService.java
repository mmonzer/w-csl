package com.xcsl.interfaces;


import java.io.IOException;
import java.util.List;

import com.xcsl.json.Json;

public interface IFileStoreService {

	Json readJsonFromFile(String dir, String fileName);

	void saveJsonToFile(String dir, String fileName, Json j);
	
	String buildFullPathInConfDir(String relativeDir);

	String sanitizeDirPath(String s);

	boolean fileExists(String dir, String fileName);
	boolean fileExists(String fileName);
	boolean dirExists(String fileName);

	

	Json writeFileFromText(String filename, String text);
	Json readFileInAJsonText(String filename);

	String readFile(String filename) throws IOException;

	String writeFile(String filename, String contents) throws IOException;

	void backupFileWithTimeStamp(String filePath, String idsModelDirBackup);
	void reverseToLastBackupFile(String filePath);

	String checkAndCreateDir(String s);

	void renameFileWithTimeStamp(String filePath, String string);

	List<String> readFileAsStringList(String filename) throws IOException;

	boolean deleteDirectory(String rootDir,String directoryToBeDeleted);
	boolean deleteFile(String rootDir,String fileToBeDeleted);

	void createSubDir(String rootDir, String subDir);
	boolean renameSubDir(String rootDir, String subsrc, String subdst);
	
	

	
}
