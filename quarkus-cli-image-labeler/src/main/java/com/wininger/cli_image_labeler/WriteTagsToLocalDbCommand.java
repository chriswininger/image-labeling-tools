package com.wininger.cli_image_labeler;

import com.wininger.cli_image_labeler.image.tagging.ImageInfo;
import com.wininger.cli_image_labeler.image.tagging.ImageInfoService;
import com.wininger.cli_image_labeler.image.tagging.ImageTagEntity;
import com.wininger.cli_image_labeler.image.tagging.ImageTagRepository;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Stream;

@Command(name = "write-tags-to-local-db", mixinStandardHelpOptions = true)
public class WriteTagsToLocalDbCommand implements Runnable {
    @Parameters(paramLabel = "<directory-path>", description = "The path to a directory containing images to process and save to database")
    String directoryPath;

    private final ImageInfoService imageInfoService;
    private final ImageTagRepository imageTagRepository;

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "tif"
    );

    @Inject
    public WriteTagsToLocalDbCommand(
        final ImageInfoService imageInfoService,
        final ImageTagRepository imageTagRepository
    ) {
        this.imageInfoService = imageInfoService;
        this.imageTagRepository = imageTagRepository;
    }

    @Override
    @ActivateRequestContext
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
            final long totalImages = paths
                .filter(Files::isRegularFile)
                .filter(this::isImageFile)
                .count();

            System.out.println("Found " + totalImages + " image(s) to process");

            try (Stream<Path> imagePaths = Files.walk(directory)) {
                final long[] processed = {0};
                imagePaths
                    .filter(Files::isRegularFile)
                    .filter(this::isImageFile)
                    .forEach(imagePath -> {
                        processImage(imagePath);
                        processed[0]++;
                        System.out.println("Progress: " + processed[0] + "/" + totalImages);
                    });
            }
        } catch (IOException e) {
            System.err.println("Error walking directory: " + e.getMessage());
            throw new RuntimeException("Failed to process directory", e);
        }

        System.out.println("\nCompleted processing all images!");
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
            final String fullPath = imagePath.toAbsolutePath().toString();
            System.out.println("\n=== Processing: " + fullPath + " ===");

            // Check if image already exists in database
            final ImageTagEntity existing = imageTagRepository.findByFullPath(fullPath);
            if (existing != null) {
                System.out.println("Image already exists in database, skipping...");
                return;
            }

            // Generate image info
            final ImageInfo imageInfo = imageInfoService.generateImageInfo(fullPath);
            
            // Convert tags list to string (comma-separated)
            final String tagsString = String.join(", ", imageInfo.tags());

            // Save to database
            final ImageTagEntity saved = imageTagRepository.save(
                fullPath,
                imageInfo.fullDescription(),
                tagsString
            );

            System.out.println("Saved to database with ID: " + saved.getId());
            System.out.println("Description: " + imageInfo.fullDescription());
            System.out.println("Tags: " + tagsString);
        } catch (Exception e) {
            System.err.println("Error processing image " + imagePath + ": " + e.getMessage());
            e.printStackTrace();
            // Continue processing other images even if one fails
        }
    }
}

