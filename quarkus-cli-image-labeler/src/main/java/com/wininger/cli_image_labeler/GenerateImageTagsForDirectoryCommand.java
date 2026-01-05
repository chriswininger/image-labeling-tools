package com.wininger.cli_image_labeler;

import com.wininger.cli_image_labeler.image.tagging.ImageInfo;
import com.wininger.cli_image_labeler.image.tagging.ImageInfoService;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Stream;

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
            System.out.println("\n=== Processing: " + imagePath + " ===");
            final ImageInfo imageInfo = imageInfoService.generateImageInfoAndMetadata(imagePath.toString(), true);
            System.out.println("Image Info: " + imageInfo);
        } catch (Exception e) {
            System.err.println("Error processing image " + imagePath + ": " + e.getMessage());
            // Continue processing other images even if one fails
        }
    }
}

