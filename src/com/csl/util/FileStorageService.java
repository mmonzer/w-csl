package com.csl.util;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class FileStorageService {
    private final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private final Path rootLocation;
    private final Map<Path, OffsetDateTime> deleteSchedule = new ConcurrentHashMap<>();
    private final long defaultDeleteDelaySec = 6 * 3600;
    private final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);

    public FileStorageService(String downloadPath) {
        this.rootLocation = Paths.get(downloadPath);
        try {
            Files.createDirectories(this.rootLocation);
            FileUtils.cleanDirectory(this.rootLocation.toFile());
        } catch (Exception e) {
            logger.error("Error creating download directory: " + downloadPath, e);
        }
        scheduledExecutorService.scheduleAtFixedRate(this::deleteFiles, 0, 1, java.util.concurrent.TimeUnit.HOURS);
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
        deleteSchedule.put(filePath, OffsetDateTime.now().plusSeconds(defaultDeleteDelaySec));
        return filePath;
    }

    private void deleteFiles() {
        OffsetDateTime now = OffsetDateTime.now();
        deleteSchedule.forEach((path, dateTime) -> {
            if (dateTime.isBefore(now)) {
                try {
                    logger.debug("Deleting file: " + path);
                    Files.deleteIfExists(path);
                    deleteSchedule.remove(path);
                } catch (IOException e) {
                    logger.error("Error deleting file: " + path, e);
                }
            }
        });
    }
}
