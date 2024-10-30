package com.csl.ids;

import com.ucsl.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

public class IDSDataSetManager {
	/**
	 * Logger instance for this class.
	 */
	private static final Logger logger = LoggerFactory.getLogger(IDSDataSetManager.class);

	public static int RECORDING=1;
	public static int LEARNING=2;
	public static int DETECTION_OFFLINE=3;

	public static String CREATE="create";
	public static String DELETE="delete";
	public static String RENAME="rename";
	public static String SELECT="select";
	public static String COPY="copy";
	public static String LIST="list";

	IDSParams idsParams;

	public IDSDataSetManager(IDSParams idsParams) {
		this.idsParams=idsParams;
	}

	public String getDirOfCategory(int category) {
		if (category==LEARNING) return idsParams.getPackets_dir_for_learning();
		if (category==DETECTION_OFFLINE) return idsParams.getPackets_dir_for_detection_offline();
		return idsParams.getPackets_dir_for_recording();		
	}

	public String getCurrentdataSetOfCategory(int cat) {
		String s="";
		if (cat==LEARNING) s= idsParams.getCurrentDataSetNameForLearning();
		else if (cat==DETECTION_OFFLINE) s= idsParams.getCurrentDataSetNameForDetectionOffLine();
		else s= idsParams.getCurrentDataSetNameForRecording();

		return selectDataSet(cat, s);

	}

	public void setCurrentdataSetOfCategory(int cat, String name) {
		if (cat==LEARNING) idsParams.setCurrentDataSetNameForLearning(name);
		else if (cat==DETECTION_OFFLINE) idsParams.setCurrentDataSetNameForDetectionOffLine(name);
		else idsParams.setCurrentDataSetNameForRecording(name);
	}

	public String createDataSet(int category, String nameFile) {
		if (nameFile.isEmpty()) {
			nameFile=newdataSetName(getDirOfCategory(category));
		}
		String  path  = (getDirOfCategory(category)+
				File.separator + nameFile);
		new File(path).mkdirs();

		return nameFile;
	}

	public String selectDataSet(int category, String name) {
		String [] ds=dataSetNames(getDirOfCategory(category));
		if (ds==null) return "";
		for (int i=0; i<ds.length;i++) {
			if (ds[i].compareTo(name)==0) {
				setCurrentdataSetOfCategory(category, name);
				return name;
			}
		}
		name=findDataSetName((category));
		setCurrentdataSetOfCategory(category, name);
		return name;
	}

	private String int2str(int n) {
		if (n<=9999) {
			String s="0000"+n;
			return s.substring(s.length()-4, s.length());
		}
		else {
			return ""+n;
		}
	}

	public String findDataSetName(int category) {
		String dirName=getDirOfCategory(category);
		File file = new File(dirName);
		String[] directories = file.list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
		if (directories.length==0) {
			return createDataSet(category,newdataSetName(dirName));
		}
		else {
			return directories[0];
		}
	}

	public String renameDataSet(int category, String oldName, String newName) {
		String dirName=getDirOfCategory(category);
		String selected=getCurrentdataSetOfCategory(category);
		File oldDir = new File(dirName+File.separator + oldName);
		if (!oldDir.isDirectory()) {
			logger.error("cannot rename "+oldDir+" (not found)");
			return "cannot rename, "+oldName+" not found";
		} else {
			File newDir = new File(dirName + File.separator + newName);
			if (newDir.exists()) {
				logger.error("cannot rename "+oldDir+" to "+newDir+" (already exist)");
				return "cannot rename, target "+newName+" exists";
			}
			else {
				oldDir.renameTo(newDir);
				if (oldName.compareToIgnoreCase(selected)==0) 
					setCurrentdataSetOfCategory(category, newName);
			}
		}
		return "";
	}
	
	public String deleteDataSet(int category, String name) {
		String dirName=getDirOfCategory(category);
		File dir = new File(dirName+File.separator + name);
		if (!dir.isDirectory()) {
			logger.error("cannot delete "+dir+" (not found)");
			return "cannot delete "+name+" (not found)";
		} else {
			deleteDirectory(dir);
		}
		return "";
	}

	private void deleteDirectory(File directoryToBeDeleted) {
		File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectory(file);
			}
		}
		directoryToBeDeleted.delete();
	}

	public static void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation) 
			throws IOException {

		System.out.println("COPY "+sourceDirectoryLocation+" to "+destinationDirectoryLocation);
		try (Stream<Path> paths = Files.walk(Paths.get(sourceDirectoryLocation))) {

			paths.forEach(source -> {
				Path destination = Paths.get(destinationDirectoryLocation, source.toString()
						.substring(sourceDirectoryLocation.length()));
				try {
					Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					// e.printStackTrace();
				}
			});
		}
	}

	public String copyDataSet(int category, String name, int targetCategory, String targetName,boolean overwrite) {
		String dirNameOfSource=getDirOfCategory(category);
		String dirNameOfTarget=getDirOfCategory(targetCategory);


		String sourceDirectoryLocation=dirNameOfSource+File.separator + name;
		String destinationDirectoryLocation=	dirNameOfTarget+File.separator + targetName;

		File dirSrc = new File(sourceDirectoryLocation);
		File dirDst = new File(destinationDirectoryLocation);

		if (!overwrite) {
			if (dirDst.exists()) {
				logger.error("cannot overwrite "+dirDst);
				return "Error: Target dataset exists ";
			}
		}

		dirDst.mkdirs();

		if (!dirSrc.isDirectory()) {
			logger.error("cannot delete "+dirSrc+" (not found)");
			return "Error: Source dataset doesn't exist";
		} 

		try {
			copyDirectory(sourceDirectoryLocation, destinationDirectoryLocation);
		} catch (IOException e) {
			// e.printStackTrace();
			return "Error:"+e.getMessage();

		}
		return "";
	}

	public String newdataSetName(String dirName) {
		File file = new File(dirName);
		String[] directories = file.list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});

		int n=0;

		while (true) {
			String s="data"+int2str(n);
			File f= new File(dirName+File.separator+s);
			if (!f.isDirectory()) return s;
			n=n+1;
		}

	}

	public String[] dataSetNames(String dirName) {
		File file = new File(dirName);
		String[] directories = file.list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});

		return directories;

	}

	private Json getFolderFilesListAsJson(int category, String folderName) {
		
		String rootdir=getDirOfCategory(category);
		File folder= new File(rootdir+File.separator+folderName);
	    
	    File[] files = folder.listFiles();
	   
	    Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                  return o1.getName().compareToIgnoreCase(o2.getName());
            }
	    });
	    
	    int count = files.length;

	    Json j=Json.array();
	    
	    for (int i = 0; i < count; i++) {
	        if (files[i].isFile()) {
	        	Json jf= Json.object();
			    jf.set("name",files[i].getName());
			    jf.set("size",files[i].length());
	            j.add(jf);
	        }
	    }

	    
	    return j;
	}

	public Json exec(String cmd, int category, String name, int targetCategory, String targetName, boolean overwrite) {
		String s="";
		cmd=cmd.toLowerCase();

		if (CREATE.compareTo(cmd)==0) {
			s=createDataSet(category, name);
			if (s.isEmpty()) return Json.object(); else return Json.object().set("error",s);
		}
		else if (DELETE.compareTo(cmd)==0) {
			s=deleteDataSet(category, name);
			if (s.isEmpty()) return Json.object(); else return Json.object().set("error",s);
		}
		else if (RENAME.compareTo(cmd)==0) {
			s= renameDataSet(category, name, targetName);
			if (s.isEmpty()) return Json.object(); else return Json.object().set("error",s);
		}
		else if (SELECT.compareTo(cmd)==0) {
			s=selectDataSet(category, name);
			if (s.isEmpty()) return Json.object(); else return Json.object().set("error",s);
		}
		else if (COPY.compareTo(cmd)==0) {
			s= copyDataSet(category, name, targetCategory, targetName, overwrite);
			if (s.isEmpty()) return Json.object(); else return Json.object().set("error",s);
		}
		else if (LIST.compareTo(cmd)==0) {
			return getFolderFilesListAsJson(category, name);
			
		}
		else {
			s= "Invalid cmd:"+cmd;
			if (s.isEmpty()) return Json.object(); else return Json.object().set("error",s);
		}
	}
}
