package com.wininger.cli_image_labeler.commands;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

@Command(name = "randomize-gps-coordinates", mixinStandardHelpOptions = true,
         description = "Creates copies of JPEG images with GPS coordinates replaced by fake coordinates from famous landmarks")
public class RandomizeGpsCoordinatesCommand implements Runnable {

    // Famous landmarks with their coordinates (latitude, longitude)
    private static final List<LocationInfo> FAKE_LOCATIONS = List.of(
        new LocationInfo("Sydney Opera House", -33.8568, 151.2153),
        new LocationInfo("Eiffel Tower, Paris", 48.8584, 2.2945),
        new LocationInfo("Statue of Liberty, NYC", 40.6892, -74.0445),
        new LocationInfo("Big Ben, London", 51.5007, -0.1246),
        new LocationInfo("Colosseum, Rome", 41.8902, 12.4922),
        new LocationInfo("Christ the Redeemer, Rio", -22.9519, -43.2105),
        new LocationInfo("Great Wall of China", 40.4319, 116.5704),
        new LocationInfo("Machu Picchu, Peru", -13.1631, -72.5450),
        new LocationInfo("Taj Mahal, India", 27.1751, 78.0421),
        new LocationInfo("Petra, Jordan", 30.3285, 35.4444)
    );

    private record LocationInfo(String name, double latitude, double longitude) {}

    @Parameters(paramLabel = "<path>", description = "Path to an image file or directory containing images")
    String path;

    @Option(names = {"-r", "--recursive"}, description = "Process directories recursively")
    boolean recursive = false;

    private int locationIndex = 0;

    @Override
    public void run() {
        final Path targetPath = Paths.get(path);

        if (!Files.exists(targetPath)) {
            System.err.println("Path does not exist: " + path);
            return;
        }

        // Collect files to process
        final List<Path> filesToProcess;
        try {
            filesToProcess = collectJpegFiles(targetPath);
        } catch (IOException e) {
            System.err.println("Error scanning for files: " + e.getMessage());
            return;
        }

        if (filesToProcess.isEmpty()) {
            System.out.println("No JPEG files found to process.");
            return;
        }

        // Show what will be processed and ask for confirmation
        System.out.println("This operation will create safe copies with randomized GPS coordinates:");
        System.out.println("=========================================================================");
        for (final Path file : filesToProcess) {
            System.out.println("  " + file + " -> " + getSafeOutputPath(file));
        }
        System.out.println("=========================================================================");
        System.out.println("Total files to process: " + filesToProcess.size());
        System.out.println();

        if (!confirmAction()) {
            System.out.println("Operation cancelled.");
            return;
        }

        // Process the files
        System.out.println();
        System.out.println("Processing files...");
        System.out.println();

        int successCount = 0;
        int failCount = 0;

        for (final Path jpegPath : filesToProcess) {
            final LocationInfo location = FAKE_LOCATIONS.get(locationIndex % FAKE_LOCATIONS.size());
            final Path outputPath = getSafeOutputPath(jpegPath);

            System.out.printf("Processing: %s -> %s (%s, %.4f, %.4f)%n",
                jpegPath.getFileName(),
                outputPath.getFileName(),
                location.name(),
                location.latitude(),
                location.longitude());

            try {
                createSafeCopyWithNewGps(jpegPath.toFile(), outputPath.toFile(), location.latitude(), location.longitude());
                System.out.println("  SUCCESS: Created " + outputPath);
                successCount++;
            } catch (Exception e) {
                System.err.println("  FAILED: " + e.getMessage());
                failCount++;
            }

            locationIndex++;
        }

        System.out.println();
        System.out.println("=========================================================================");
        System.out.printf("Processing complete! Success: %d, Failed: %d%n", successCount, failCount);
    }

    /**
     * Collects all JPEG files from the given path.
     * If path is a file, returns a list with just that file (if it's a JPEG).
     * If path is a directory, returns all JPEG files (optionally recursive).
     */
    private List<Path> collectJpegFiles(final Path targetPath) throws IOException {
        if (Files.isRegularFile(targetPath)) {
            if (isJpegFile(targetPath)) {
                return List.of(targetPath);
            } else {
                return List.of();
            }
        }

        if (Files.isDirectory(targetPath)) {
            try (final Stream<Path> stream = recursive
                    ? Files.walk(targetPath)
                    : Files.list(targetPath)) {

                return stream
                    .filter(Files::isRegularFile)
                    .filter(this::isJpegFile)
                    .sorted()
                    .toList();
            }
        }

        return List.of();
    }

    /**
     * Asks the user to confirm the operation.
     */
    private boolean confirmAction() {
        System.out.print("Do you want to proceed? (yes/no): ");

        // Try Console first (works in real terminal)
        final Console console = System.console();
        if (console != null) {
            final String response = console.readLine();
            return "yes".equalsIgnoreCase(response != null ? response.trim() : "");
        }

        // Fall back to Scanner (works in IDE)
        try (final Scanner scanner = new Scanner(System.in)) {
            final String response = scanner.nextLine();
            return "yes".equalsIgnoreCase(response.trim());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generates the safe output path for a given input file.
     * Example: image.jpg -> image-safe.jpg
     */
    private Path getSafeOutputPath(final Path inputPath) {
        final String fileName = inputPath.getFileName().toString();
        final int dotIndex = fileName.lastIndexOf('.');

        final String baseName = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        final String extension = dotIndex > 0 ? fileName.substring(dotIndex) : "";

        final String safeFileName = baseName + "-safe" + extension;
        return inputPath.getParent() != null
            ? inputPath.getParent().resolve(safeFileName)
            : Paths.get(safeFileName);
    }

    /**
     * Creates a copy of the JPEG file with new GPS coordinates.
     * The original file is not modified.
     */
    private void createSafeCopyWithNewGps(final File inputFile, final File outputFile,
                                           final double latitude, final double longitude) throws Exception {

        // Read existing metadata
        final var metadata = Imaging.getMetadata(inputFile);

        TiffOutputSet outputSet = null;

        if (metadata instanceof JpegImageMetadata jpegMetadata) {
            final TiffImageMetadata exif = jpegMetadata.getExif();
            if (exif != null) {
                outputSet = exif.getOutputSet();
            }
        }

        // If no existing EXIF, create a new output set
        if (outputSet == null) {
            outputSet = new TiffOutputSet();
        }

        // Set the GPS coordinates
        outputSet.setGpsInDegrees(longitude, latitude);

        // Write directly to the output file
        try (final FileOutputStream fos = new FileOutputStream(outputFile);
             final BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            new ExifRewriter().updateExifMetadataLossless(inputFile, bos, outputSet);
        }
    }

    private boolean isJpegFile(final Path path) {
        final String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg");
    }
}
