package com.csl.util;

import com.csl.logger.CSLLogger;
import com.ucsl.json.Json;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class FileUtils {

    public static String fileSeparator = File.separator;

    public static final String EOL = System.getProperty("line.separator");

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

    public static boolean fileExists(String dir, String filename) {
        if (dir.isEmpty()) dir = ".";
        File file = new File(dir + File.separator + filename);
        return file.exists();
    }

    public static boolean fileExists(String filename) {
        File file = new File(filename);
        return file.exists();
    }

    public static boolean dirExists(String f) {
        File file = new File(f);

        return file.exists() && file.isDirectory();
    }

    public static String checkAndCreateDir(String s) {

        File file = new File(s);

        if (file.exists()) {
            if (file.isDirectory()) return s;
        }

        s = sanitizeDirPath(s);

        file = new File(s);
        int n = 2;
        while (file.exists() && !file.isDirectory()) {
            file = new File(s + n);
            n++;
        }
        if (!file.exists()) file.mkdirs();
        return file.getPath().toString();
    }

    public static void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation) throws IOException {
        Files.walk(Paths.get(sourceDirectoryLocation)).forEach(source -> {
            Path destination = Paths.get(destinationDirectoryLocation, source.toString().substring(sourceDirectoryLocation.length()));
            try {
                Files.copy(source, destination);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static Json readJsonFromFile(String dir, String fileName) {

        if (dir.isEmpty()) dir = ".";

        String f = dir + File.separator + fileName;

        String content = "{}";
        try {
            content = readFile(f);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            CSLLogger.instance.error("Cannot read Json file :" + f);
        }

        Json j = Json.read(content);
        return j;
    }

    public static String readFile(String filename) throws IOException {
        BufferedReader br = null;
        FileReader fr = null;

        try {
            fr = new FileReader(filename);
            br = new BufferedReader(fr);
            String nextLine = "";
            StringBuilder sb = new StringBuilder();
            while ((nextLine = br.readLine()) != null) {
                sb.append(nextLine); // note: BufferedReader strips the EOL character
                //   so we add a new one!
                sb.append(EOL);
            }
            return sb.toString();
        } finally {
            if (br != null) br.close();
            if (fr != null) fr.close();
        }
    }

    public static String writeFile(String filename, String content) {

        File file = new File(filename);
        String dir = file.getAbsoluteFile().getParent();

        if (dir != null) new File(dir).mkdirs();


        System.out.println(dir);
        System.out.println(filename);

        try {

            Files.write(Paths.get(filename), content.getBytes());
            return "ok";
        } catch (IOException e) {
            e.printStackTrace();
            return "error " + e.getMessage();
        }
    }

    public static Json readFileInAJsonText(String filename) {
        Json result = Json.object();
        result.set("text", "");
        result.set("error", "");
        try {
            String s = FileUtils.readFile(filename);
            result.set("text", s);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            result.set("error", e.getMessage());
        }
        return result;
    }

    public static Json writeFileFromText(String filename, String content) {
        Json result = Json.object();

        result.set("error", "");
        try {

            Files.write(Paths.get(filename), content.getBytes());
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return result.set("error", e.getMessage());
        }
    }

    private static void addCommaToLastrString(List<String> strList) {
        int n = strList.size() - 1;
        strList.set(n, strList.get(n) + ",");
    }

    public static List<String> jsonToStringList(String name, Json j, List<String> strList, String decal) {
        if (j.isObject()) {
            if (name.isEmpty()) strList.add(decal + "{");
            else strList.add(decal + '"' + name + '"' + ":{");
            boolean first = true;
            for (Entry<String, Json> entry : j.asJsonMap().entrySet()) {
                if (!first) addCommaToLastrString(strList);
                first = false;
                jsonToStringList(entry.getKey(), entry.getValue(), strList, "  " + decal);
            }
            strList.add(decal + "}");
        } else if (j.isArray()) {
            if (name.isEmpty()) strList.add(decal + "[");
            else strList.add(decal + '"' + name + '"' + ":[");
            boolean first = true;
            for (Json je : j.asJsonList()) {
                if (!first) addCommaToLastrString(strList);
                first = false;
                jsonToStringList("", je, strList, "  " + decal);
            }
            strList.add(decal + "]");
        } else {
            if (name.isEmpty()) strList.add(decal + j.toString());
            else strList.add(decal + '"' + name + '"' + ":" + j.toString());
        }


        return strList;
    }

    private static void createFile(String file, List<String> arrData) throws IOException {
        FileWriter writer = new FileWriter(file);
        int size = arrData.size();
        for (int i = 0; i < size; i++) {
            String str = arrData.get(i).toString();
            writer.write(str);
            if (i < size - 1) //This prevent creating a blank like at the end of the file**
                writer.write("\n");
        }
        writer.close();
    }

    public static void saveJsonToFile(String dir, String fileName, Json j) {
        if (dir.isEmpty()) dir = ".";

        new File(dir).mkdirs();
        String file = dir + File.separator + fileName;
        List<String> strlist = jsonToStringList("", j, new ArrayList<String>(), "");
        try {
            createFile(file, strlist);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static String getTimeStamp() {
        Date date = new Date();

        return sdf.format(date.getTime());
    }

    public static String renameFileWithTimeStamp(String filePath, String moreInfo) {

        File f = new File(filePath);

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

        Date date = new Date();
        String dir = parentPath.toString();
        String newFileName = dir + File.separatorChar + fileNameWithoutExtension + '_' + sdf.format(date.getTime()) + fileNameExtension + moreInfo;


        if (f.exists() && !f.isDirectory()) {
            File f2 = new File(newFileName);
            boolean success = f.renameTo(f2);
            if (success) return newFileName;
            return "";
        } else {
            return "";
        }
    }

    public static void backupFileWithTimeStamp(String filePath, String backupDir) {

        if (backupDir == null) backupDir = "";

        if (!backupDir.isEmpty()) {
            File f = new File(backupDir);
            f.mkdirs();
        }

        File f = new File(filePath);


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

        Date date = new Date();
        String dir = parentPath.toString(); //System.getProperty("user.dir");
        String newFileName = fileNameWithoutExtension + '_' + sdf.format(date.getTime()) + fileNameExtension;

        if (!backupDir.isEmpty()) {
            newFileName = backupDir + File.separatorChar + newFileName;
        } else {
            newFileName = dir + File.separatorChar + newFileName;
        }

        if (f.exists() && !f.isDirectory()) {
            File f2 = new File(newFileName);
            try {
                Files.copy(f.toPath(), f2.toPath());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    static private void deleteReverseFile(String filePath) {
        File[] files2 = getListOfFilesWithTimeStamp(filePath, ".reversed");
        for (File file : files2) {
            boolean ok = file.delete();
        }
    }

    public static String reverseToLastBackupFile(String filePath) {

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

    static private File[] getListOfFilesWithTimeStamp(String filePath, String moreInfo) {
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

    public static String sanitize(String filename) {
        if (filename == null) return null;
        if (filename.contains("/")) filename = filename.replace("/", "_");
        if (filename.contains(":")) filename = filename.replace(":", "_");
        if (filename.contains("*")) filename = filename.replace("*", "_");
        if (filename.contains("?")) filename = filename.replace("?", "_");
        if (filename.contains("\"")) filename = filename.replace("\"", "_");
        if (filename.contains("<")) filename = filename.replace("<", "_");
        if (filename.contains(">")) filename = filename.replace(">", "_");
        if (filename.contains("|")) filename = filename.replace("|", "_");
        if (filename.contains("\\")) filename = filename.replace("\\", "_");
        if (filename.contains("&")) filename = filename.replace("&", "_");

        return filename;
    }

    public static String sanitizeDirPath(String path) {

        String[] x = path.split(File.separator);

        boolean absolute = false;

        if (path.startsWith(File.separator)) absolute = true;
        String s = "";
        for (int i = 0; i < x.length; i++) {
            if (x[i].compareTo("..") != 0) {
                if (!s.isEmpty()) s = s + File.separator;
                s = s + sanitize(x[i]);
            }
        }
        if (absolute) s = File.separator + s;

        return s;
    }

    public static void main(String[] args) {

        String path = "/eaeae/../tes?t.ext/";
        File f = new File(path);
        System.out.println(f.getName());
        System.out.println(f.getParent());
        System.out.println(f.getPath());
        System.out.println(path);
        System.out.println(sanitizeDirPath(path));


        String dir = "/Users/flausj/Documents/usb/AMI";


        try {
            copyDirectory(dir, "/Users/flausj/Documents/usb/AMI2");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static List<String> readFileAsStringList(String filename) throws IOException {
        BufferedReader br = null;
        FileReader fr = null;

        try {
            fr = new FileReader(filename);
            br = new BufferedReader(fr);
            String nextLine = "";
            List<String> list = new ArrayList<String>();
            while ((nextLine = br.readLine()) != null) {
                list.add(nextLine);
            }
            return list;
        } finally {
            if (br != null) br.close();
            if (fr != null) fr.close();
        }
    }
}
