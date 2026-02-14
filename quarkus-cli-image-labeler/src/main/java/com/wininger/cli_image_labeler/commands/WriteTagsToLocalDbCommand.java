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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.wininger.cli_image_labeler.image.tagging.db.TagEntity;
import com.wininger.cli_image_labeler.setup.DataDirectoryInitializer;

import static com.wininger.cli_image_labeler.image.tagging.utils.PrintUtils.getTimeTakenMessage;
import static com.wininger.cli_image_labeler.image.tagging.utils.PrintUtils.printImageInfoResults;

@Command(name = "write-tags-to-local-db", mixinStandardHelpOptions = true)
public class WriteTagsToLocalDbCommand implements Runnable {
    @Parameters(paramLabel = "<path>", description = "The path to an image or directory containing images to process and save to database")
    String inputPath;

    @Option(names = "--update-existing", description = "Update existing database entries and regenerate thumbnails")
    boolean updateExisting;

    @Option(names = "--parallelism", description = "Number of parallel image processors (default: ${DEFAULT-VALUE})", defaultValue = "1")
    int parallelism;

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
    public void run() {
        final long startTime = System.currentTimeMillis();
        final String failLogName = "failed-image-processing-%s.log".formatted(startTime);

        final Path path = Paths.get(inputPath);
        if (!Files.exists(path)) {
            System.err.println("Error: Path does not exist: " + inputPath);
            return;
        }

        if (Files.isDirectory(path)) {
            processDirectory(path, failLogName, startTime);
        } else if (Files.isRegularFile(path)) {
            if (isImageFile(path)) {
                processImage(path, failLogName);
                System.out.printf("\n\nCompleted processing image in: %s",
                    getTimeTakenMessage(startTime, System.currentTimeMillis()));
            } else {
                System.err.println("Error: File is not a supported image type: " + inputPath);
            }
        } else {
            System.err.println("Error: Path is neither a file nor a directory: " + inputPath);
        }
    }

    private void processDirectory(final Path directory, final String failLogName, final long startTime) {
        final ExecutorService pool = Executors.newFixedThreadPool(parallelism);

        try {
            final List<Path> imageFiles;
            try (Stream<Path> paths = Files.walk(directory)) {
                imageFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(this::isImageFile)
                    .collect(Collectors.toList());
            }

            final int totalImages = imageFiles.size();
            final AtomicInteger processed = new AtomicInteger(0);
            System.out.println("Found " + totalImages + " image(s) to process with parallelism=" + parallelism);

            final List<Future<?>> futures = new ArrayList<>();
            for (final Path imagePath : imageFiles) {
                futures.add(pool.submit(() -> {
                    processImage(imagePath, failLogName);
                    System.out.println("Progress: " + processed.incrementAndGet() + "/" + totalImages);
                }));
            }

            // Wait for all tasks to complete
            for (final Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    System.err.println("Unexpected error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error walking directory: " + e.getMessage());
            throw new RuntimeException("Failed to process directory", e);
        } finally {
            pool.shutdown();
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

            // Check if image already exists in database (requires request context)
            final ImageInfoEntity existing = findExistingImage(fullPath);
            if (existing != null && !updateExisting) {
                System.out.println("Image already exists in database, skipping...");
                return;
            }
            if (existing != null) {
                System.out.println("Image already exists in database, updating...");
            }

            // Generate image info - this calls AI services and should NOT be in request context
            final ImageInfo imageInfo = imageInfoService.generateImageInfoAndMetadata(fullPath, true);

            if (Objects.isNull(imageInfo.tags())) {
                throw new RuntimeException("Null tags were returned");
            }

            // Save to database (requires request context)
            saveImageToDatabase(fullPath, imageInfo, existing != null, startTime);

        } catch (Exception e) {
            System.err.println("Error processing image " + imagePath + ": " + e.getMessage());
            writeFailedImageProcess(imagePath, failLogName, e);
            // Continue processing other images even if one fails
        }
    }

    @ActivateRequestContext
    ImageInfoEntity findExistingImage(final String fullPath) {
        return imageTagRepository.findByFullPath(fullPath);
    }

    @ActivateRequestContext
    void saveImageToDatabase(final String fullPath, final ImageInfo imageInfo,
                             final boolean isUpdate, final long startTime) {
        // Upsert all tags into the tags table and collect TagEntity objects
        final List<TagEntity> tagEntities = imageInfo.tags().stream()
            .map(tagRepository::upsertTag)
            .collect(Collectors.toList());

        // Fetch the existing entity within this request context if updating
        final ImageInfoEntity existing = isUpdate
            ? imageTagRepository.findByFullPath(fullPath)
            : null;

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
            imageTagRepository.save(
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
    }

  private void writeFailedImageProcess(final Path imagePath, final String failLogName, final Exception exception) {
    try {
      final Path dataDir = DataDirectoryInitializer.getDataDirectory();
      final Path logFile = dataDir.resolve(failLogName);
      final String fullPath = imagePath.toAbsolutePath().toString();
      final String exceptionClass = exception.getClass().getName();
      final String logEntry = "\"" + fullPath + "\", \"" + exceptionClass + "\"\n";

      // Create a data directory if it doesn't exist
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
