package com.csl.ids;

//import java.io.File;
//import java.io.FilenameFilter;

import com.ucsl.interfaces.IFileStoreService;
import com.ucsl.interfaces.IIDSMainProcessor;
import com.ucsl.interfaces.IIDSMainProcessorParams;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class IDSTapManager {
    String tapsConfigFileName = "TapsConfiguration.json";
    String tapsDir = "taps";

    String suricataConfigFileName = "suricata.yaml";
    String tapConfigFileName = "config.ini";

    public static int SURICATA_RULES_BASE = 3;
    public static int SURICATA_RULES_LEARNED = 4;
    public static int SURICATA_CONFIG = 5;
    public static int TAP_CONFIG = 6;

    private String rulesForSuricataBaseFileName = "RulesForSuricata.txt";
    private String rulesForSuricataLearnedFileName = "RulesForSuricataLearned.txt";

    String rootDir = "./idsconf";
    List<String> listTapIds = new ArrayList<>();
    boolean initialized = false;

    public static String ADD = "add";
    public static String DEL = "del";
    public static String RENAME = "rename";
    public static String UPDATE = "update";
    public static String LIST = "list";

    public static String LOAD = "load";
    public static String SAVE = "save";

    Json jTaps = Json.array();

    IIDSMainProcessorParams idsParams;
    private IFileStoreService fileUtils;
    private IIDSMainProcessor idsMainProcessor;

    public IDSTapManager(IIDSMainProcessorParams idsParams) {
        this.idsParams = idsParams;
        this.idsMainProcessor = idsParams.getIdsMainProcessor();
        this.fileUtils = idsParams.getFileUtils();
    }

    public IDSTapManager() {

    }

    void init() {
        if (idsParams != null) {
            setRulesForSuricataBaseFileName(idsParams.getRulesForSuricataBaseFileName());
            setRulesForSuricataLearnedFileName(idsParams.getRulesForSuricataBaseFileName());
            setRootDir(idsParams.getIdsModelDir());
            setTapsIDs(idsParams.getTapsIDs());
        }
        loadTapsConfig();
        setInitialized(true);
    }

    public String getTapsDir() {
        return getRootDir() + File.separator + tapsDir;
    }

    public void setTapsIDs(List<String> listTapIds) {
        this.listTapIds = listTapIds;
    }

    public void saveTapsConfig() {
        String dir = getTapsDir();
        String fileName = tapsConfigFileName;
        Json j = Json.object();
        fileUtils.saveJsonToFile(dir, fileName, jTaps);
    }

    public void loadTapsConfig() {
        String dir = getTapsDir();
        System.out.println("TAP dir=" + dir);
        if (fileUtils.fileExists(dir, tapsConfigFileName))
            jTaps = fileUtils.readJsonFromFile(dir, tapsConfigFileName);
        else
            jTaps = Json.array();
        System.out.println("Reading TAPS conf:" + jTaps);
    }

    public Json findTapDescriptor(String idname) {
        for (Json j : jTaps.asJsonList()) {
            String s = JsonUtil.getStringFromJson(j, "idname", "");
            if (s.compareTo(idname) == 0) return j;
        }
        return null;
    }

    public boolean isExistingTapDescriptor(String idname) {
        return findTapDescriptor(idname) != null;
    }

    public Json emptyFileError(String error) {
        Json result = Json.object();
        result.set("text", "");
        result.set("error", error);
        return result;
    }

    public Json emptyFile() {
        Json result = Json.object();
        result.set("text", "");
        result.set("error", "");
        return result;
    }

    public Json loadSuricataRulesBase(String idname) {
        if (!isExistingTapDescriptor(idname)) return emptyFileError("Tap not found " + idname);
        String filename =
                getTapsDir() + File.separator + idname + File.separator + getRulesForSuricataBaseFileName();
        if (!fileUtils.fileExists(filename)) return emptyFile();
        return fileUtils.readFileInAJsonText(filename);
    }

    public Json saveSuricataRulesBase(String idname, String text) {
        if (!isExistingTapDescriptor(idname)) return emptyFileError("Tap not found " + idname);
        String filename =
                getTapsDir() + File.separator + idname + File.separator + getRulesForSuricataBaseFileName();

        return fileUtils.writeFileFromText(filename, text);
    }

    public Json loadSuricataRulesLearned(String idname) {
        if (!isExistingTapDescriptor(idname)) return emptyFileError("Tap not found " + idname);
        String filename =
                getTapsDir() + File.separator + idname + File.separator + getRulesForSuricataLearnedFileName();

        if (!fileUtils.fileExists(filename)) return emptyFile();
        return fileUtils.readFileInAJsonText(filename);
    }

    public Json saveSuricataRulesLearned(String idname, String text) {
        if (!isExistingTapDescriptor(idname)) return emptyFileError("Tap not found " + idname);
        String filename =
                getTapsDir() + File.separator + idname + File.separator + getRulesForSuricataLearnedFileName();

        return fileUtils.writeFileFromText(filename, text);
    }

    public Json loadSuricataConfig(String idname) {
        if (!isExistingTapDescriptor(idname)) return emptyFileError("Tap not found " + idname);
        String filename =
                getTapsDir() + File.separator + idname + File.separator + suricataConfigFileName;
        if (!fileUtils.fileExists(filename)) return emptyFile();
        return fileUtils.readFileInAJsonText(filename);
    }

    public Json saveSuricataConfig(String idname, String text) {
        if (!isExistingTapDescriptor(idname)) return emptyFileError("Tap not found " + idname);
        String filename =
                getTapsDir() + File.separator + idname + File.separator + suricataConfigFileName;

        return fileUtils.writeFileFromText(filename, text);
    }

    public Json loadTapConfig(String idname) {
        if (!isExistingTapDescriptor(idname)) return emptyFileError("Tap not found " + idname);
        String filename =
                getTapsDir() + File.separator + idname + File.separator + tapConfigFileName;
        if (!fileUtils.fileExists(filename)) return emptyFile();
        return fileUtils.readFileInAJsonText(filename);
    }

    public Json saveTapConfig(String idname, String text) {
        if (!isExistingTapDescriptor(idname)) return emptyFileError("Tap not found " + idname);
        String filename =
                getTapsDir() + File.separator + idname + File.separator + tapConfigFileName;

        return fileUtils.writeFileFromText(filename, text);
    }

    public Json addTap(String idname, Json value) {
        Json result = Json.object();

        if (!isValidName(idname)) return Json.object().set("error", "Invalid type name " + idname);
        if (isExistingTapDir(idname) || isExistingTapDescriptor(idname))
            return Json.object().set("error", "Existing tap name " + idname);

        Json j = Json.object();
        j.set("idname", idname);
        j.set("ip", JsonUtil.getStringFromJson(value, "ip", ""));
        j.set("username", JsonUtil.getStringFromJson(value, "username", ""));
        j.set("password", JsonUtil.getStringFromJson(value, "password", ""));

        jTaps.add(j);
        createTapDir(idname);
        saveTapsConfig();

        return result;
    }

    public Json delTap(String idname) {
        Json result = Json.object();

        Json j = findTapDescriptor(idname);
        if (j == null) return Json.object().set("error", "Cannot find descriptor for " + idname);

        Json jarray = Json.array();
        for (Json jj : jTaps.asJsonList()) {
            if (idname.compareTo(jj.get("idname").asString()) != 0) jarray.add(jj);
        }
        jTaps = jarray;
        saveTapsConfig();

        deleteTapDir(idname);
        saveTapsConfig();

        return result;
    }

    public Json updateTap(String idname, Json value) {
        Json result = Json.object();

        Json j = findTapDescriptor(idname);
        if (j == null) return Json.object().set("error", "Cannot find descriptor for " + idname);

        if (value.has("ip")) j.set("ip", JsonUtil.getStringFromJson(value, "ip", ""));
        if (value.has("username")) j.set("username", JsonUtil.getStringFromJson(value, "username", ""));
        if (value.has("password")) j.set("password", JsonUtil.getStringFromJson(value, "password", ""));


        saveTapsConfig();

        return result;
    }

    public Json renameTap(String idname, Json value) {
        Json result = Json.object();

        Json j = findTapDescriptor(idname);
        if (j == null) return Json.object().set("error", "Cannot find descriptor for " + idname);

        String newName = JsonUtil.getStringFromJson(value, "new_idname", "");
        if (!isValidName(newName)) return Json.object().set("error", "Invalid type name " + newName);
        if (isExistingTapDir(newName) || isExistingTapDescriptor(newName))
            return Json.object().set("error", "Existing tap name " + newName);

        if (renameTapDir(idname, newName)) {
            j.set("idname", newName);
        }
        saveTapsConfig();
        return result;
    }

    public Json tapsList() {
        return jTaps;
    }

    public Json exec(String cmd, String idname, Json value, String text) {

        if (!isInitialized()) init();

        cmd = cmd.toLowerCase();
        if (ADD.compareTo(cmd) == 0) {
            return addTap(idname, value);
        } else if (DEL.compareTo(cmd) == 0) {
            return delTap(idname);
        } else if (RENAME.compareTo(cmd) == 0) {
            return renameTap(idname, value);
        } else if (UPDATE.compareTo(cmd) == 0) {
            return updateTap(idname, value);
        } else if (LIST.compareTo(cmd) == 0) {
            return tapsList();
        } else if (LOAD.compareToIgnoreCase(cmd) == 0) {
            int type = JsonUtil.getIntFromJson(value, "type", 0);
            if (type == SURICATA_CONFIG) return loadSuricataConfig(idname);
            else if (type == TAP_CONFIG) return loadTapConfig(idname);
            else if (type == SURICATA_RULES_BASE) return loadSuricataRulesBase(idname);
            else if (type == SURICATA_RULES_LEARNED) return loadSuricataRulesLearned(idname);
            else
                return Json.object().set("error", "Invalid type " + type + " in load");
        } else if (SAVE.compareToIgnoreCase(cmd) == 0) {
            int type = JsonUtil.getIntFromJson(value, "type", 0);
            if (type == SURICATA_CONFIG) return saveSuricataConfig(idname, text);
            else if (type == TAP_CONFIG) return saveTapConfig(idname, text);
            else if (type == SURICATA_RULES_BASE) return saveSuricataRulesBase(idname, text);
            else if (type == SURICATA_RULES_LEARNED) return saveSuricataRulesLearned(idname, text);

            else
                return Json.object().set("error", "Invalid type " + type + " in save");
        } else {
            return Json.object().set("error", "Invalid operation:" + cmd);
        }
    }

    public String normalizeName(String s) {
        String z = "";
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                z = z + ch;
            }
        }
        z = z.toLowerCase();
        if (z.length() > 8) z = z.substring(0, 8);
        return z;
    }

    public boolean isValidName(String s) {
        if (s == null) s = "";
        if (s.isEmpty()) return false;
        return s.compareTo(normalizeName(s)) == 0;
    }

    public boolean isExistingTapDir(String s) {
        String dirName = getTapsDir();

        return idsMainProcessor.getFileStoreServices().dirExists(dirName + File.separator + s);
    }

    public void createTapDir(String name) {
        String s = normalizeName(name);
        String dirName = getTapsDir();
        idsMainProcessor.getFileStoreServices().createSubDir(dirName, s);
    }

    public void deleteTapDir(String name) {
        String s = normalizeName(name);
        String dirName = getTapsDir();
        idsMainProcessor.getFileStoreServices().deleteDirectory(dirName, s);
    }

    public boolean renameTapDir(String src, String dst) {
        src = normalizeName(src);
        dst = normalizeName(dst);

        String dir = getTapsDir() + File.separator;
        return
                idsMainProcessor.getFileStoreServices().renameSubDir(dir, src, dst);
    }
}
