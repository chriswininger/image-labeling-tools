package com.wininger.cli_image_labeler.setup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DataDirectoryInitializer {

    /**
     * Environment variable name for overriding the data location.
     */
    public static final String DATA_LOCATION_ENV_VAR = "IL_DATA_LOCATION";

    /**
     * Default data directory name when environment variable is not set.
     */
    private static final String DEFAULT_DATA_DIR = "data";

    private static Path cachedDataDir = null;

    static {
        // Create directories when class is loaded (before Flyway runs)
        initializeDirectories();
    }

    private static void initializeDirectories() {
        final Path dataDir = getDataDirectory();
        if (!Files.exists(dataDir)) {
            try {
                Files.createDirectories(dataDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create data directory: " + dataDir, e);
            }
        }

        final Path thumbnailsDir = getThumbnailsDirectory();
        if (!Files.exists(thumbnailsDir)) {
            try {
                Files.createDirectories(thumbnailsDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create thumbnails directory: " + thumbnailsDir, e);
            }
        }
    }

    /**
     * Returns the data directory path, checking the IL_DATA_LOCATION environment variable first.
     * Falls back to "data" if the environment variable is not set.
     *
     * @return the path to the data directory
     */
    public static Path getDataDirectory() {
        if (cachedDataDir == null) {
            final String envValue = System.getenv(DATA_LOCATION_ENV_VAR);
            if (envValue != null && !envValue.isBlank()) {
                cachedDataDir = Paths.get(envValue);
                System.out.println("Using data directory from " + DATA_LOCATION_ENV_VAR + ": " + cachedDataDir);
            } else {
                cachedDataDir = Paths.get(DEFAULT_DATA_DIR);
            }
        }
        return cachedDataDir;
    }

    /**
     * Returns the thumbnails directory path (within the data directory).
     *
     * @return the path to the thumbnails directory
     */
    public static Path getThumbnailsDirectory() {
        return getDataDirectory().resolve("thumbnails");
    }

    /**
     * Returns the database file path (within the data directory).
     *
     * @return the path to the database file
     */
    public static Path getDatabasePath() {
        return getDataDirectory().resolve("image-tags.db");
    }

    /**
     * Returns the data directory as a string for use in configuration.
     *
     * @return the data directory path as a string
     */
    public static String getDataDirectoryString() {
        return getDataDirectory().toString();
    }

    /**
     * Ensures directories exist. Can be called explicitly if needed.
     */
    public static void ensureDirectoriesExist() {
        initializeDirectories();
    }
}

