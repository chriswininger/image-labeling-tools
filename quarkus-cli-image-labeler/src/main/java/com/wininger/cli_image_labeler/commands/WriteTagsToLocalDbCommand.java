package com.wininger.cli_image_labeler.commands;

import com.wininger.cli_image_labeler.image.tagging.dto.ImageInfo;
import com.wininger.cli_image_labeler.image.tagging.services.ImageInfoService;
import com.wininger.cli_image_labeler.image.tagging.db.ImageInfoEntity;
import com.wininger.cli_image_labeler.image.tagging.db.ImageInfoRepository;
import com.wininger.cli_image_labeler.image.tagging.db.TagRepository;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.wininger.cli_image_labeler.image.tagging.db.TagEntity;

import static com.wininger.cli_image_labeler.image.tagging.utils.PrintUtils.getTimeTakenMessage;
import static com.wininger.cli_image_labeler.image.tagging.utils.PrintUtils.printImageInfoResults;

@Command(name = "write-tags-to-local-db", mixinStandardHelpOptions = true)
public class WriteTagsToLocalDbCommand implements Runnable {
    @Parameters(paramLabel = "<directory-path>", description = "The path to a directory containing images to process and save to database")
    String directoryPath;

    @Option(names = "--update-existing", description = "Update existing database entries and regenerate thumbnails")
    boolean updateExisting;

    private final ImageInfoService imageInfoService;
    private final ImageInfoRepository imageTagRepository;
    private final TagRepository tagRepository;

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "tif"
    );

    @Inject
    public WriteTagsToLocalDbCommand(
        final ImageInfoService imageInfoService,
        final ImageInfoRepository imageTagRepository,
        final TagRepository tagRepository
    ) {
        this.imageInfoService = imageInfoService;
        this.imageTagRepository = imageTagRepository;
        this.tagRepository = tagRepository;
    }

    @Override
    @ActivateRequestContext
    public void run() {
        final long startTime = System.currentTimeMillis();
        final String failLogName = "failed-image-processing-%s.log".formatted(startTime);

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
                        processImage(imagePath, failLogName);
                        processed[0]++;
                        System.out.println("Progress: " + processed[0] + "/" + totalImages);
                    });
            }
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

    private void processImage(final Path imagePath, final String failLogName) {
        final long startTime = System.currentTimeMillis();

        try {
            final String fullPath = imagePath.toAbsolutePath().toString();
            System.out.println("\n=== Processing: " + fullPath + " ===");

            // Check if image already exists in database
            final ImageInfoEntity existing = imageTagRepository.findByFullPath(fullPath);
            if (existing != null) {
                if (updateExisting) {
                    System.out.println("Image already exists in database, updating...");
                } else {
                    System.out.println("Image already exists in database, skipping...");
                    return;
                }
            }

            // Generate image info (always generate thumbnail when updating existing entries)
            final ImageInfo imageInfo = imageInfoService.generateImageInfoAndMetadata(fullPath, true);

            if (Objects.isNull(imageInfo.tags())) {
                throw new RuntimeException("Null tags were returned");
            }

            // Upsert all tags into the tags table and collect TagEntity objects
            final List<TagEntity> tagEntities = imageInfo.tags().stream()
                .map(tagRepository::upsertTag)
                .collect(Collectors.toList());

            if (existing != null) {
                // Update existing entry
                existing.setDescription(imageInfo.fullDescription());
                existing.setTags(tagEntities);
                existing.setThumbnailName(imageInfo.thumbnailName());
                existing.setShortTitle(imageInfo.shortTitle());
                existing.setIsText(imageInfo.isText());
                existing.setTextContents(imageInfo.textContents());
                existing.setGpsLatitude(imageInfo.gpsLatitude());
                existing.setGpsLongitude(imageInfo.gpsLongitude());
                existing.setImageTakenAt(imageInfo.imageTakenAt());
                existing.setFileCreatedAt(imageInfo.fileCreatedAt());
                existing.setFileLastModified(imageInfo.fileLastModified());
                final ImageInfoEntity updated = imageTagRepository.update(existing);

                System.out.println("Updated database entry with ID: " + updated.getId());
                printImageInfoResults(imageInfo, startTime);
            } else {
                // Save new entry to database
                final ImageInfoEntity saved = imageTagRepository.save(
                    fullPath,
                    imageInfo.fullDescription(),
                    tagEntities,
                    imageInfo.thumbnailName(),
                    imageInfo.shortTitle(),
                    imageInfo.isText(),
                    imageInfo.textContents(),
                    imageInfo.gpsLatitude(),
                    imageInfo.gpsLongitude(),
                    imageInfo.imageTakenAt(),
                    imageInfo.fileCreatedAt(),
                    imageInfo.fileLastModified()
                );
                printImageInfoResults(imageInfo, startTime);
            }
        } catch (Exception e) {
            System.err.println("Error processing image " + imagePath + ": " + e.getMessage());
            writeFailedImageProcess(imagePath, failLogName, e);
          // Continue processing other images even if one fails
        }
    }

  private void writeFailedImageProcess(final Path imagePath, final String failLogName, final Exception exception) {
    try {
      final Path logFile = Paths.get("data", failLogName);
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
