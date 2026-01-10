package com.wininger.cli_image_labeler.commands;

import com.wininger.cli_image_labeler.image.tagging.dto.ImageInfo;
import com.wininger.cli_image_labeler.image.tagging.services.ImageInfoService;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.stream.Stream;

import static com.wininger.cli_image_labeler.image.tagging.utils.PrintUtils.getTimeTakenMessage;
import static com.wininger.cli_image_labeler.image.tagging.utils.PrintUtils.printImageInfoResults;

@Command(name = "generate-image-tags-for-directory", mixinStandardHelpOptions = true)
public class GenerateImageTagsForDirectoryCommand implements Runnable {
    @Parameters(paramLabel = "<directory-path>", description = "The path to a directory containing images to process")
    String directoryPath;

    private final ImageInfoService imageInfoService;

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "tif"
    );

    @Inject
    public GenerateImageTagsForDirectoryCommand(final ImageInfoService imageInfoService) {
        this.imageInfoService = imageInfoService;
    }

    @Override
    public void run() {
        final long startTime = System.currentTimeMillis();

        final Path directory = Paths.get(directoryPath);

        if (!Files.exists(directory)) {
            System.err.println("Error: Directory does not exist: " + directoryPath);
            return;
        }

        if (!Files.isDirectory(directory)) {
            System.err.println("Error: Path is not a directory: " + directoryPath);
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            paths
                .filter(Files::isRegularFile)
                .filter(this::isImageFile)
                .forEach(this::processImage);
        } catch (IOException e) {
            System.err.println("Error walking directory: " + e.getMessage());
            throw new RuntimeException("Failed to process directory", e);
        }

      System.out.printf("\n\nCompleted processing all images in: %s",
          getTimeTakenMessage(startTime, System.currentTimeMillis()));
    }

    private boolean isImageFile(final Path path) {
        final String fileName = path.getFileName().toString().toLowerCase();
        final int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) {
            return false;
        }
        final String extension = fileName.substring(lastDot + 1);
        return IMAGE_EXTENSIONS.contains(extension);
    }

    private void processImage(final Path imagePath) {
        try {
            final long startTime = System.currentTimeMillis();
            System.out.println("\n=== Processing: " + imagePath + " ===");
            final ImageInfo imageInfo = imageInfoService.generateImageInfoAndMetadata(imagePath.toString(), false);
            printImageInfoResults(imageInfo, startTime);
        } catch (Exception e) {
            System.err.println("Error processing image " + imagePath + ": " + e.getMessage());
            writeFailedImageProcess(imagePath, e);
            // Continue processing other images even if one fails
        }
    }

    private void writeFailedImageProcess(final Path imagePath, final Exception exception) {
        try {
            final Path logFile = Paths.get("data", "failed-image-processing.log");
            final String fullPath = imagePath.toAbsolutePath().toString();
            final String exceptionClass = exception.getClass().getName();
            final String logEntry = "\"" + fullPath + "\", \"" + exceptionClass + "\"\n";

            // Create a data directory if it doesn't exist
            final Path dataDir = Paths.get("data");
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }

            // Append to a log file (create if it doesn't exist)
            Files.writeString(
                logFile,
                logEntry,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            // If we can't write to the log file, just print a warning
            System.err.println("Warning: Failed to write to failed-image-processing.log: " + e.getMessage());
        }
    }
}

