package com.wininger.cli_image_labeler.setup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DataDirectoryInitializer {

    static {
        // Create directories when class is loaded (before Flyway runs)
        initializeDirectories();
    }

    private static void initializeDirectories() {
        final Path dataDir = Paths.get("data");
        if (!Files.exists(dataDir)) {
            try {
                Files.createDirectories(dataDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create data directory", e);
            }
        }

        final Path thumbnailsDir = Paths.get("data", "thumbnails");
        if (!Files.exists(thumbnailsDir)) {
            try {
                Files.createDirectories(thumbnailsDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create thumbnails directory", e);
            }
        }
    }

    /**
     * Ensures directories exist. Can be called explicitly if needed.
     */
    public static void ensureDirectoriesExist() {
        initializeDirectories();
    }
}

