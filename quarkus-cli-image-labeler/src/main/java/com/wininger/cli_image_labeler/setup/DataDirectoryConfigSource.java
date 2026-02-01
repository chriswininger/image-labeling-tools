package com.wininger.cli_image_labeler.setup;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A custom ConfigSource that provides the data directory path and ensures
 * the directory structure exists before Flyway/datasource initialization.
 * 
 * This ConfigSource is loaded early in the Quarkus startup process, before
 * the datasource is created, ensuring directories exist when SQLite tries
 * to create the database file.
 */
public class DataDirectoryConfigSource implements ConfigSource {

    private static final String DATA_LOCATION_ENV_VAR = "IL_DATA_LOCATION";
    private static final String DEFAULT_DATA_DIR = "data";
    private static final String CONFIG_PROPERTY_NAME = "il.data.directory";

    private final String dataDirectory;

    public DataDirectoryConfigSource() {
        // Determine the data directory
        final String envValue = System.getenv(DATA_LOCATION_ENV_VAR);
        if (envValue != null && !envValue.isBlank()) {
            this.dataDirectory = envValue;
            System.out.println("Using data directory from " + DATA_LOCATION_ENV_VAR + ": " + dataDirectory);
        } else {
            this.dataDirectory = DEFAULT_DATA_DIR;
        }

        // Create the directories immediately when this ConfigSource is instantiated
        ensureDirectoriesExist();
    }

    private void ensureDirectoriesExist() {
        final Path dataDir = Paths.get(dataDirectory);
        if (!Files.exists(dataDir)) {
            try {
                Files.createDirectories(dataDir);
                System.out.println("Created data directory: " + dataDir.toAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException("Failed to create data directory: " + dataDir, e);
            }
        }

        final Path thumbnailsDir = dataDir.resolve("thumbnails");
        if (!Files.exists(thumbnailsDir)) {
            try {
                Files.createDirectories(thumbnailsDir);
                System.out.println("Created thumbnails directory: " + thumbnailsDir.toAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException("Failed to create thumbnails directory: " + thumbnailsDir, e);
            }
        }
    }

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> props = new HashMap<>();
        props.put(CONFIG_PROPERTY_NAME, dataDirectory);
        return props;
    }

    @Override
    public Set<String> getPropertyNames() {
        return Set.of(CONFIG_PROPERTY_NAME);
    }

    @Override
    public int getOrdinal() {
        // Higher than default (100) to override application.properties if needed
        // but lower than system properties (400) and env vars (300)
        return 250;
    }

    @Override
    public String getValue(String propertyName) {
        if (CONFIG_PROPERTY_NAME.equals(propertyName)) {
            return dataDirectory;
        }
        return null;
    }

    @Override
    public String getName() {
        return "DataDirectoryConfigSource";
    }
}
