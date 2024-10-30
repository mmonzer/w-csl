package com.csl.util;

import com.ucsl.json.Json;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class FileUtils  {
    public static final String FILENAME = "filename";
    public static final String CONTENT = "content";

    public static final String EOL = System.getProperty("line.separator");

    public static boolean fileExists(String dir, String filename) {
        if (dir.isEmpty()) dir = ".";
        return fileExists(dir + File.separator + filename);
    }

    public static boolean fileExists(String filename) {
        return new File(filename).exists();
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

    public static Json readJsonFromFile(String dir, String fileName) throws IOException {
        if (dir.isEmpty()) dir = ".";
        String filePath = dir + File.separator + fileName;

        return readJsonFromFile(filePath);
    }

    public static Json readJsonFromFile(String filePath) throws IOException {
        return Json.read(readFile(filePath));
    }

    public static String readFile(String filename) throws IOException {
        try (FileReader fileReader = new FileReader(filename); BufferedReader bufferedReader = new BufferedReader(fileReader)){
            return readFile(bufferedReader);
        }
    }

    public static String readFile(BufferedReader bufferedReader) throws IOException {
        String nextLine = "";
        StringBuilder stringBuilder = new StringBuilder();
        while ((nextLine = bufferedReader.readLine()) != null) {
            stringBuilder.append(nextLine); // note: BufferedReader strips the EOL character
            stringBuilder.append(EOL);
        }
        return stringBuilder.toString();
    }

    public static Json readFileInAJsonText(String filename) {
        Json result = Json.object();
        result.set("text", "");
        result.set("error", "");
        try {
            String s = FileUtils.readFile(filename);
            result.set("text", s);
        } catch (IOException e) {
            
            // e.printStackTrace();
            result.set("error", e.getMessage());
        }
        return result;
    }

    public static Json writeFileFromText(String filename, String content) {
        Json result = Json.object();

        result.set("error", "");
        try {
            writeToFile(filename, content);
            return result;
        } catch (IOException e) {
            // e.printStackTrace();
            return result.set("error", e.getMessage());
        }
    }


    public static void writeToFile(String path, String content) throws IOException {
        try (FileWriter myWriter = new FileWriter(path)) {
            myWriter.write(content);
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
        try (FileWriter writer = new FileWriter(file)) {
            int size = arrData.size();
            for (int i = 0; i < size; i++) {
                String str = arrData.get(i).toString();
                writer.write(str);
                if (i < size - 1) //This prevent creating a blank like at the end of the file**
                    writer.write("\n");
            }
        }
    }

    public static void saveJsonToFile(String dir, String fileName, Json j) {
        if (dir.isEmpty()) dir = ".";

        new File(dir).mkdirs();
        String file = dir + File.separator + fileName;
        List<String> strlist = jsonToStringList("", j, new ArrayList<String>(), "");
        try {
            createFile(file, strlist);
        } catch (IOException e) {
            
            // e.printStackTrace();
        }
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
            if ("true".equals(values[i]) || "false".equals(values[i])) {
                tmp.set(toCamelCase(headers[i]), Boolean.parseBoolean(values[i]));
                continue;
            }

            // otherwise string-
            tmp.set(toCamelCase(headers[i]), values[i]);
        }
        return tmp;
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

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            String sheetName = sheet.getSheetName();
            Row headers;
            int startRow;

            if (i == 0) {  // First sheet
                headers = sheet.getRow(0);
                startRow = 1; // Data starts from the second row
            } else {  // All other sheets relate to http templates
                headers = sheet.getRow(1);
                startRow = 2; // Data starts from the third row
            }

            // Iterate through the rows starting from the determined startRow
            for (int j = startRow; j <= sheet.getLastRowNum(); j++) {
                Json tmp = lineXLSToJson(headers, sheet.getRow(j), sheetName);
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
    public static Json lineXLSToJson(Row headers, Row row, String sheetName) {
        // create a list of string
        List<String> knowFixedColumnForCnxRelatedToHttp = new ArrayList<>();
        knowFixedColumnForCnxRelatedToHttp.add("name");
        knowFixedColumnForCnxRelatedToHttp.add("port");
        knowFixedColumnForCnxRelatedToHttp.add("protocol");
        Json cnxInputFotCnxRelatedToHttp = Json.object();
        Json tmp = Json.object();
        // check if all values of temp are empty strings, return null
        boolean allEmpty = false;
        // Iterate through cells
        for (int i = 0; i < headers.getLastCellNum(); i++) {
            if(row==null) {
                return null;
            }
            Cell cell = row.getCell(i);
            String headerKey = toCamelCase(headers.getCell(i).getStringCellValue());

            if (cell==null) {
                tmp.set( toCamelCase(headers.getCell(i).getStringCellValue()), "");
                allEmpty = true;
                continue;
            } else{
                allEmpty = false;
            }
            switch (cell.getCellType()) {
                case STRING:
                    String stringValue = cell.getStringCellValue();
                    if (!knowFixedColumnForCnxRelatedToHttp.contains(headerKey) && !Objects.equals(sheetName, "snmp_powershell")) {
                        cnxInputFotCnxRelatedToHttp.set(headerKey, stringValue);
                    } else {
                        tmp.set(headerKey, stringValue);
                    }
                    break;
                case NUMERIC:
                    long numericValue = Math.round(cell.getNumericCellValue());
                    if (!knowFixedColumnForCnxRelatedToHttp.contains(headerKey)  && !Objects.equals(sheetName, "snmp_powershell")) {
                        cnxInputFotCnxRelatedToHttp.set(headerKey, numericValue);
                    } else {
                        tmp.set(headerKey, numericValue);
                    }
                    break;
                case BOOLEAN:
                    boolean booleanValue = cell.getBooleanCellValue();
                    if (!knowFixedColumnForCnxRelatedToHttp.contains(headerKey)  && !Objects.equals(sheetName, "snmp_powershell")) {
                        cnxInputFotCnxRelatedToHttp.set(headerKey, booleanValue);
                    } else {
                        tmp.set(headerKey, booleanValue);
                    }
                    break;
                default:
                    break;
            }
        }
        if (!Objects.equals(sheetName, "snmp_powershell")) {
            tmp.set("protocol", "HTTP");
            tmp.set("discoveryProtocolNameRelatedToHttpCnx", sheetName);
        }
//        if (allEmpty) {
//            return null;
//        }
        // Add cnxInputFotCnxRelatedToHttp JSON to tmp under the key "inputs"
        tmp.set("inputs", cnxInputFotCnxRelatedToHttp);

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



