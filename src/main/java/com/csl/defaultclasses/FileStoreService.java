package com.csl.defaultclasses;

import com.csl.util.FileUtils;
import com.ucsl.json.Json;
import lombok.Getter;

import java.io.File;
import java.io.IOException;

public class FileStoreService {
    @Getter
    String cslConfDir = ".";

    public FileStoreService(String cslConf) {
        this.cslConfDir = cslConf;
    }
}
