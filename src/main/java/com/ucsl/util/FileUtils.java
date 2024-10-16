package com.ucsl.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileUtils {

    public static final String EOL = System.getProperty("line.separator");

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

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

    public static String getTimeStamp() {
        Date date = new Date();

        return sdf.format(date.getTime());
    }

    // put back the previous version of a file

    public static String sanitize(String filename) {
        if (filename == null)
            return null;
        if (filename.contains("/"))
            filename = filename.replace("/", "_");
        if (filename.contains(":"))
            filename = filename.replace(":", "_");
        if (filename.contains("*"))
            filename = filename.replace("*", "_");
        if (filename.contains("?"))
            filename = filename.replace("?", "_");
        if (filename.contains("\""))
            filename = filename.replace("\"", "_");
        if (filename.contains("<"))
            filename = filename.replace("<", "_");
        if (filename.contains(">"))
            filename = filename.replace(">", "_");
        if (filename.contains("|"))
            filename = filename.replace("|", "_");
        if (filename.contains("\\"))
            filename = filename.replace("\\", "_");
        if (filename.contains("&"))
            filename = filename.replace("&", "_");

        return filename;
    }
}
