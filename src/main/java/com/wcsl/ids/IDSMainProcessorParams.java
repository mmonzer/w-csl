package com.wcsl.ids;

import com.csl.core.Config;
import com.csl.defaultclasses.FileStoreService;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class IDSMainProcessorParams {
    @Setter
    @Getter
    private IDSMainProcessor idsMainProcessor;
    @Getter
    private final FileStoreService fileUtils;

    List<String> listTapIds = new ArrayList<>();

    String rulesForSuricataBaseFileName = "";

    public IDSMainProcessorParams(IDSMainProcessor idsMainProcessor, Config.IdsConf config) {

        this.idsMainProcessor = idsMainProcessor;
        fileUtils = idsMainProcessor.getFileStoreServices();
        initFromJson(config);
    }

    public String getIdsModelDir() {
        return "";
    }

    public List<String> getTapsIDs() {
        return listTapIds;
    }

    public void initFromJson(Config.IdsConf config) {
        rulesForSuricataBaseFileName = config.getRulesForSuricataBase();
        rulesForSuricataBaseFileName = config.getTapsDir();
    }

    public String getRulesForSuricataBaseFileName() {
        return rulesForSuricataBaseFileName;
    }
}
