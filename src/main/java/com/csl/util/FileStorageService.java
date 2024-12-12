package com.csl.util;

import com.csl.logger.LoggerCustomEndpoints;
import com.csl.logger.LoggerInterfaces;
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
    private final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);

    public FileStorageService(String downloadPath) {
        this.rootLocation = Paths.get(downloadPath);
        try {
            Files.createDirectories(this.rootLocation);
            FileUtils.cleanDirectory(this.rootLocation.toFile());
        } catch (Exception e) {
            logger.error("Error creating download directory: " + downloadPath, e);
        }
        ThreadUtils.uncorrelatedSingleThreadScheduledAtFixedRate(
                scheduledExecutorService,
                this::deleteFiles,
                0, 1, java.util.concurrent.TimeUnit.HOURS,
                LoggerCustomEndpoints.AUTO_DELETING_FILES, LoggerInterfaces.CSL_CLIENT);
    }

    public FileStorageService() {
        this("downloads");
    }

    public Path saveFile(InputStream inputStream, String fileName) throws IOException {
        Path filePath = rootLocation.resolve(fileName);
        Files.copy(inputStream, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        deleteSchedule.put(filePath, OffsetDateTime.now().plusSeconds(6 * 3600L));
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

    public void deleteFile(String filename) {
        try {
            Path fullFilePath = rootLocation.resolve(filename);
            Files.deleteIfExists(fullFilePath);
            deleteSchedule.remove(fullFilePath);
        } catch (IOException e) {
            logger.error("Error deleting file: " + filename, e);
        }
    }

    /**
     * Get the file path for the given filename.
     *
     * @param filename The filename to get the path for.
     * @return The path for the given filename.
     */
    public Path getFilePath(String filename) {
        logger.info("...........Getting file path for:........." + filename);
        Path filepath = rootLocation.resolve(filename);
        if (Files.exists(filepath)) {
            return filepath;
        } else {
            return null;
        }
    }
}
