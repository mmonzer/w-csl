package com.csl.util;

import com.csl.intercom.cslscan.ScanApiHandler;
import com.csl.logger.CSLLogger;
import com.ucsl.json.Json;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(ScanApiHandler.class);

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


//        System.out.println(dir);
//        System.out.println(filename);

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
//        System.out.println(f.getName());
//        System.out.println(f.getParent());
//        System.out.println(f.getPath());
//        System.out.println(path);
//        System.out.println(sanitizeDirPath(path));


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

    public static List<Json> parseConnexionsFromCSV(byte[] fileContent) {
        return parseConnexionsFromCSV(new String(fileContent));
    }

    public static List<Json> parseConnexionsFromCSV(String fileContent) {
        // CSV file delimiter
        String DELIMITER_COLUMN = ",";
        String DELIMITER_LINE = "\n";

        List<Json> connections = new ArrayList<>();

        // Parse into connexion
        String[] lines = fileContent.split(DELIMITER_LINE);
        String[] headers = lines[0].split(DELIMITER_COLUMN);
        for (int i = 1; i < lines.length; i++) {
            Json tmp = lineCSVToJson(headers, lines[i].split(DELIMITER_COLUMN));
            if (tmp != null) {
                connections.add(tmp);
            }
        }
        return connections;
    }

    public static List<Json> parseConnexionsFromCSV(Json fileContent) {
        return parseConnexionsFromCSV(parseJsonByteFileToByteArray(fileContent));
    }

    /**
     * This method converts a string to camel case.
     * For example, "hello world" becomes "helloWorld".
     * @param input The string to convert to camel case.
     *              Must not be null.
     *              Must not be empty.
     * @return The input string converted to camel case.
     * **/
    public static String toCamelCase(String input) {
        // Split the input string by spaces
        String[] words = input.split(" ");

        // Convert the first word to lowercase
        StringBuilder camelCaseString = new StringBuilder(words[0].toLowerCase());

        // Convert each subsequent word's first letter to uppercase and append to the result
        for (int i = 1; i < words.length; i++) {
            camelCaseString.append(words[i].substring(0, 1).toUpperCase())
                    .append(words[i].substring(1).toLowerCase());
        }

        return camelCaseString.toString();
    }

    /**
     * From a list of values it makes a json object
     *
     * @param headers key values of the json object
     * @param values  values of the json object
     * @return json object
     */
    public static Json lineCSVToJson(String[] headers, String[] values) {
        Json tmp = Json.object();
        for (int i = 0; i < headers.length; i++) {
            // nto enough values (splits method trims the last empty spaces
            if (values.length <= i) {
                tmp.set(toCamelCase(headers[i]), "");
                continue;
            }
            // check if integer
            try {
                tmp.set(toCamelCase(headers[i]), Integer.parseInt(values[i]));
                continue;
            } catch (NumberFormatException ignored) {
            }
            // check if double
            try {
                tmp.set(toCamelCase(headers[i]), Double.parseDouble(values[i]));
                continue;
            } catch (NumberFormatException ignored) {
            }
            // check if boolean
            if (values[i]=="true" || values[i]=="false") {
                tmp.set(toCamelCase(headers[i]), Boolean.parseBoolean(values[i]));
                continue;
            }

            // otherwise string
            tmp.set(toCamelCase(headers[i]), values[i]);
        }
        return tmp;
    }

    public static List<Json> parseConnexionsFromXLSFile(Integer[] fileContent) throws FileNotFoundException {
        byte[] bytes = new byte[fileContent.length];
        for (int i=0;i<fileContent.length;i++) {
            bytes[i] = (byte) (int) (fileContent[i]);
        }
        return parseConnexionsFromXLSFile(bytes);
    }

    public static List<Json> parseConnexionsFromXLSFile(Json fileContent) throws FileNotFoundException {
        return parseConnexionsFromXLSFile(parseJsonByteFileToByteArray(fileContent));
    }

    public static List<Json> parseConnexionsFromXLSFile(byte[] fileContent) throws FileNotFoundException {
        try {
            // Parse the Excel file
            Workbook workbook = new HSSFWorkbook(new ByteArrayInputStream(fileContent));
            return parseExcelWorkbook(workbook);
        } catch (IOException e) {
            throw new FileNotFoundException();
        }
    }

    public static List<Json> parseConnexionsFromXLSXFile(Json fileContent) throws FileNotFoundException {
        return parseConnexionsFromXLSXFile(parseJsonByteFileToByteArray(fileContent));
    }

    public static List<Json> parseConnexionsFromXLSXFile(byte[] fileContent) throws FileNotFoundException {
        try {
            // Parse the Excel file
            Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileContent));
            return parseExcelWorkbook(workbook);
        } catch (IOException e) {
            throw new FileNotFoundException();
        }
    }

    private static List<Json> parseExcelWorkbook(Workbook workbook) {
        List<Json> connections = new ArrayList<>();
        // Iterate through sheets
//          for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
        for (int i = 0; i < 1; i++) {
            Sheet sheet = workbook.getSheetAt(i);

            // Iterate through rows
            Row headers = sheet.getRow(0);
            for (int j = 1; j < sheet.getLastRowNum(); j++) {
                Json tmp = lineXLSToJson(headers, sheet.getRow(j));
                if (tmp != null) {
                    connections.add(tmp);
                }
            }
        }
        return connections;
    }

    /**
     * From a row of values in Excel it makes a json object
     *
     * @param headers key values of the json object
     * @param row     values of the json object
     * @return json object
     */
    public static Json lineXLSToJson(Row headers, Row row) {
        Json tmp = Json.object();
        // Iterate through cells
        for (int i = 0; i < headers.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell==null) {
                tmp.set(headers.getCell(i).getStringCellValue(), "");
                continue;
            }
            switch (cell.getCellType()) {
                case STRING:
                    tmp.set(headers.getCell(i).getStringCellValue(), cell.getStringCellValue());
                    break;
                case NUMERIC:
                    tmp.set(headers.getCell(i).getStringCellValue(), cell.getNumericCellValue());
                    break;
                case BOOLEAN:
                    tmp.set(headers.getCell(i).getStringCellValue(), cell.getBooleanCellValue());
                    break;
                default:
                    break;
            }
        }
        return tmp;
    }

    public static byte[] parseJsonByteFileToByteArray(Json content) {
        Integer[] fileContent = Arrays.stream(content.asJsonList().stream().map(Json::asInteger).toArray()).toArray(Integer[]::new);
        byte[] bytes = new byte[fileContent.length];
        for (int i=0;i<fileContent.length;i++) {
            bytes[i] = (byte) (int) (fileContent[i]);
        }
        return bytes;
    }
}

