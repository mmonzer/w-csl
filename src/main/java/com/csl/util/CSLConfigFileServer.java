package com.csl.util;

import com.csl.core.CSLContext;
import com.csl.interfaces.IFileModificationViaUploadListener;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import lombok.Setter;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class CSLConfigFileServer {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
    //public static CSLConfigFileServer  instance= new CSLConfigFileServer();
    static boolean debug = false;
    List<String> listOfLocalFiles = new ArrayList<String>();
    int maxNumberOfFileVersion = 99;

    List<IFileModificationViaUploadListener> fileModificationViaUploadListeners = new ArrayList<IFileModificationViaUploadListener>();
    @Setter
    private String dirOfLocalFiles = System.getProperty("user.dir");
    private final boolean initialized = false;
    private boolean running = false;
    private boolean verbose = false;


    //================================================================================================================
    public void initConfigFileManager(Json j) {
        if (j == null) return;

        running = JsonUtil.getBooleanFromJson(j, "on", false);
        if (!running) return;

        if (initialized) {
            System.err.println("already initialized");
            System.exit(0);
        }

        //Json j=CSLContext.context.getConfig().get("file_server");
        Json jarray = j.get("conf_files");

        for (Json je : jarray.asJsonList()) {
            String name = je.asString();
            addLocalFileTransferableViaHttp(name);
            if (debug) System.out.println("Adding autorisation for transferable file:" + name);
        }

        int max_number_of_version = JsonUtil.getIntFromJson(j, "max_number_of_version", 5);
        setMaxNumberOfLocalFileVersion(max_number_of_version); // default 99, -1 keep all

        String dir = CSLContext.instance.buildFullPathInUserDir(
                JsonUtil.getStringFromJson(j, "root_conf_dir", ""));
        if (dir.isEmpty()) dir = CSLContext.instance.getUserDir();

        setDirOfLocalFiles(dir);

        boolean show_modified_files = JsonUtil.getBooleanFromJson(j, "show_modified_files", false);

        if (show_modified_files) {
            addFileModificationViaUploadListener(new IFileModificationViaUploadListener() {

                @Override
                public void modifying(String filename) {
                    // TODO Auto-generated method stub
                    System.out.println("File " + filename + " has been modified");
                }
            });
        }
        verbose = JsonUtil.getBooleanFromJson(j, "verbose", false);
        debug = JsonUtil.getBooleanFromJson(j, "debug", false);

        if (verbose) {
            System.out.println("Config File server configuration:");
            System.out.println("=================================");

            System.out.println(" running:" + running);
            System.out.println(" debug  :" + debug);
            System.out.println(" file_directory:" + dirOfLocalFiles);
        }
    }

    public String setFile(String filename, String contents) {
        boolean ok = isValidLocalFileTransferableViaHttp(filename);
        if (!ok) {
            return "Invalid file name <" + filename + ">: no rights to uploaded this file";
        }
        String path = dirOfLocalFiles + File.separatorChar + filename;

        renameFileWithTimeStamp(dirOfLocalFiles + File.separatorChar + filename, "");
        deleteFiles(path, maxNumberOfFileVersion);

        deleteReverseFile(dirOfLocalFiles + File.separatorChar + filename);


        writeFile(path, contents);

        for (IFileModificationViaUploadListener l : fileModificationViaUploadListeners) {
            l.modifying(filename);
        }

        return "ok";
    }

    public String getFile(String filename) {
        boolean ok = isValidLocalFileTransferableViaHttp(filename);
        if (!ok) {
            return "Invalid file name <" + filename + ">: no rights to read this file";
        }
        String path = dirOfLocalFiles + File.separatorChar + filename;

        return readAFile(path);
    }


    // ===
    //== Local Files ===//

    public String reverseToPreviousFileVersion(String filename) {

        boolean ok = isValidLocalFileTransferableViaHttp(filename);
        if (!ok) {
            return "Invalid file name <" + filename + ">: no rights to reverse this file";
        }
        String path = dirOfLocalFiles + File.separatorChar + filename;

        return doReverseFile(path);
    }

    public String cancelReverseToPreviousFileVersion(String filename) {

        boolean ok = isValidLocalFileTransferableViaHttp(filename);
        if (!ok) {
            return "Invalid file name <" + filename + ">: no rights to reverse this file";
        }
        String path = dirOfLocalFiles + File.separatorChar + filename;


        renameFileWithTimeStamp(dirOfLocalFiles + File.separatorChar + filename, "");
        deleteFiles(path, maxNumberOfFileVersion);


        return doCancelReverseFile(path);
    }

    public String getDirOfLocalFiles() {
        return dirOfLocalFiles;
    }

    public void addLocalFileTransferableViaHttp(String fileName) {
        listOfLocalFiles.add(fileName);
    }

    public void removeLocalFileTransferableViaHttp(String fileName) {
        listOfLocalFiles.remove(fileName);
    }

    public void setListOfLocalFiles(List<String> listOfLocalFiles) {
        this.listOfLocalFiles = listOfLocalFiles;
    }

    public boolean isValidLocalFileTransferableViaHttp(String fileName) {
        return listOfLocalFiles.contains(fileName);
    }

    public int getMaxNumberOfLocalFileVersion() {
        return maxNumberOfFileVersion;
    }

    public void setMaxNumberOfLocalFileVersion(int maxNumberOfFileVersion) {
        this.maxNumberOfFileVersion = maxNumberOfFileVersion;
    }

    // ====

    public void addFileModificationViaUploadListener(IFileModificationViaUploadListener l) {
        fileModificationViaUploadListeners.add(l);
    }

    public void removeFileModificationViaUploadListener(IFileModificationViaUploadListener l) {
        fileModificationViaUploadListeners.remove(l);
    }




    /*
     * Rename a file in adding a time stamp :
     * 		name.ext --> name_timestamp.ext
     *
     */

    private void writeFile(String path, String content) {

        try {

            Files.write(Paths.get(path), content.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readAFile(String filePath) {
        String content = "";

        try {
            content = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            e.printStackTrace();
            return "Error:" + e.getMessage();
        }

        return content;
    }


    // get the list of file with the path and name given in filePath and a time extensioon

    private void renameFileWithTimeStamp(String filePath, String moreInfo) {

        File f = new File(filePath);

        Path path = Paths.get(filePath);

        // call getFileName() and get FileName path object
        Path fileName = path.getFileName();
        Path parentPath = path.getParent();


        String fs = fileName.getFileName().toString();
        int pos = fs.lastIndexOf(".");
        String fileNameExtension = "", fileNameWithoutExtension = "";
        if (pos != -1) {
            fileNameExtension = fs.substring(pos);
            fileNameWithoutExtension = fs.substring(0, pos);
        } else {
            fileNameWithoutExtension = fs;
        }

        Date date = new Date();
        String dir = parentPath.toString();
        String newFileName = dir + File.separatorChar + fileNameWithoutExtension + '_' + sdf.format(date.getTime()) + fileNameExtension + moreInfo;


        if (f.exists() && !f.isDirectory()) {
            File f2 = new File(newFileName);
            boolean success = f.renameTo(f2);
        }
    }

    /*
     *
     * delete backup files if there is more files than max
     */

    private File[] getListOfFilesWithTimeStamp(String filePath, String moreInfo) {
        Path path = Paths.get(filePath);


        Path fileName = path.getFileName();
        Path parentPath = path.getParent();


        String fs = fileName.getFileName().toString();
        int pos = fs.lastIndexOf(".");
        String fileNameExtension = "", fileNameWithoutExtension = "";
        if (pos != -1) {
            fileNameExtension = fs.substring(pos);
            fileNameWithoutExtension = fs.substring(0, pos);
        } else {
            fileNameWithoutExtension = fs;
        }


        File f = new File(parentPath.toString());
        final String ext = fileNameExtension + moreInfo;
        final String fwext = fileNameWithoutExtension;


        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File f, String name) {
                return name.endsWith(ext) && name.startsWith(fwext + "_");
            }
        };


        File[] files = f.listFiles(filter);

        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

        return files;
    }

    private void deleteFiles(String filePath, int max) {


        File[] files = getListOfFilesWithTimeStamp(filePath, "");

        int i = 0;
        for (File file : files) {
            i = i + 1;
            if (i > max) {
                boolean ok = file.delete();
            }
        }
    }

    // put back the previous version of a file

    private void deleteReverseFile(String filePath) {
        File[] files2 = getListOfFilesWithTimeStamp(filePath, ".reversed");
        for (File file : files2) {
            boolean ok = file.delete();
        }
    }

    private String doReverseFile(String filePath) {


        File[] files = getListOfFilesWithTimeStamp(filePath, "");
        File f = null;

        if (files.length > 0) {
            f = files[0];
        } else {
            return "No previous version ";
        }

        deleteReverseFile(filePath);


        if (f != null) {
            renameFileWithTimeStamp(filePath, ".reversed"); // rename current file
        }

        if (f.exists() && !f.isDirectory()) {
            File f2 = new File(filePath);
            boolean success = f.renameTo(f2);
        }
        return "ok";
    }

    private String doCancelReverseFile(String filePath) {
        File[] files = getListOfFilesWithTimeStamp(filePath, ".reversed");
        File f = null;

        if (files.length > 0) {
            f = files[0];
        } else {
            return "No reversed version ";
        }

        deleteReverseFile(filePath);


        if (f != null) {
            renameFileWithTimeStamp(filePath, ".reversed"); // rename current file
        }

        if (f.exists() && !f.isDirectory()) {
            File f2 = new File(filePath);
            boolean success = f.renameTo(f2);
        }
        return "ok";
    }
}
