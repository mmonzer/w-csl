package com.csl.defaultclasses;

import com.csl.util.FileUtils;
import com.ucsl.json.Json;
import lombok.Getter;

import java.io.File;
import java.io.IOException;

public class FileStoreService {

    boolean trace = true;

    @Getter
    String cslConfDir = ".";

    public FileStoreService(String cslConf) {
        this.cslConfDir = cslConf;
    }

    public Json readJsonFromFile(String dir, String fileName) {
        try {
            return FileUtils.readJsonFromFile(dir, fileName);
        } catch (IOException e) {
            return Json.object();
        }

    }

    public void saveJsonToFile(String dir, String fileName, Json j) {
        if (trace) System.out.println("[FILESERVICE] saveJson" + dir + " " + fileName);

        FileUtils.saveJsonToFile(dir, fileName, j);
    }

    public boolean fileExists(String dir, String fileName) {
        return FileUtils.fileExists(dir, fileName);
    }

    public boolean fileExists(String fileName) {
        return FileUtils.fileExists(fileName);
    }

    public boolean dirExists(String fileName) {
        return FileUtils.dirExists(fileName);
    }

    public Json writeFileFromText(String filename, String text) {
        if (trace) System.out.println("[FILESERVICE] writefile :" + filename);

        return FileUtils.writeFileFromText(filename, text);
    }

    public Json readFileInAJsonText(String filename) {
        if (trace) System.out.println("[FILESERVICE] read Json :" + filename);

        return FileUtils.readFileInAJsonText(filename);
    }

    public String checkAndCreateDir(String s) {
        return FileUtils.checkAndCreateDir(s);
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

    public boolean deleteDirectory(String rootDir, String name) {
        File file = new File(name);
        return deleteDirectory(file);
    }

    public void createSubDir(String rootDir, String subDir) {
        File file = new File(rootDir + FileUtils.fileSeparator + subDir);
        if (!file.exists()) file.mkdirs();
    }

    public boolean renameSubDir(String rootDir, String dsrc, String ddst) {
        String fsrc = rootDir + dsrc;
        String fdst = rootDir + ddst;

        File fileToMove = new File(fsrc);
        boolean isMoved = fileToMove.renameTo(new File(fdst));
        return isMoved;
    }
}
