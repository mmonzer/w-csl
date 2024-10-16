package com.wcsl.ids;

import com.csl.alert.AlertDescriptor;
import com.csl.alert.CSLAlertFactory;
import com.csl.alert.CSLAlertManager;
import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.defaultclasses.FileLogFactory;
import com.csl.defaultclasses.FileStoreService;
import com.csl.ids.IDSConsole;
import com.ucsl.interfaces.*;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

public class IDSMainProcessor {
    private FileStoreService fileStoreServices;

    private IDSMainProcessorParams idsMainProcessorParams;

    private IFileLogFactory fileLogFactory;

    private CSLAlertManager alertManager;

    private CSLAlertFactory alertFactory;

    public IDSMainProcessor(Config.IdsConf config, String cslConfDir) {
        this.fileStoreServices = new FileStoreService(cslConfDir);

        this.fileLogFactory = new FileLogFactory();

        this.idsMainProcessorParams = new IDSMainProcessorParams(this, config);

        this.alertFactory = new CSLAlertFactory();
    }

    public void setAlertFactory(CSLAlertFactory alertFactory) {
        // TODO Auto-generated method stub
        this.alertFactory = alertFactory;
    }

    public CSLAlertFactory getAlertFactory() {
        // TODO Auto-generated method stub
        return alertFactory;
    }

    public void init() {
        // TODO Auto-generated method stub

    }

    public void setFileLogFactory(IFileLogFactory fileLogFactory) {
        this.fileLogFactory = fileLogFactory;
    }

    public void setFileStoreServices(FileStoreService fileUtils) {
        // TODO Auto-generated method stub
        this.fileStoreServices = fileUtils;
    }

    public void setAlertManager(CSLAlertManager cslAlertManager) {
        // TODO Auto-generated method stub
        this.alertManager = cslAlertManager;
    }

    public void saveJsonInModelDir(String dir, String fileName, Json j) {
        if (!dir.isEmpty())
            dir = idsMainProcessorParams.getIdsModelDir() + File.separator + dir;
        getFileStoreServices().saveJsonToFile(dir, fileName, j);
    }

    public FileStoreService getFileStoreServices() {
        // TODO Auto-generated method stub
        return fileStoreServices;
    }

    public void setConsole(IDSConsole console) {
        // TODO Auto-generated method stub

    }

    public void removeAlertFromModel(AlertDescriptor a, int i) {

    }

    public void addAlertToModel(AlertDescriptor a, int level) {

    }

    public List<String> getErrors() {
        System.err.println("Not implemented in basic version");
        return null;
    }
}
