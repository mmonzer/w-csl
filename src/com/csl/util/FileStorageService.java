package com.csl.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileStorageService {
    private final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private final Path rootLocation;

    public FileStorageService(String downloadPath) {
        this.rootLocation = Paths.get(downloadPath);
        try {
            Files.createDirectories(this.rootLocation);
        } catch (Exception e) {
            logger.error("Error creating download directory: " + downloadPath, e);
        }
    }

    public FileStorageService() {
        this("downloads");
    }

    public Path getRootLocation() {
        return rootLocation;
    }

    public String getRootLocationString() {
        return rootLocation.toString();
    }

    public Path saveFile(InputStream inputStream, String fileName) throws IOException {
        Path filePath = rootLocation.resolve(fileName);
        Files.copy(inputStream, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return filePath;
    }
}
