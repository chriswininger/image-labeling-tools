package com.wininger.metadata_explorer;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Utility class to replace GPS coordinates in test images with fake coordinates.
 * This is a one-off utility to sanitize real location data from test resources.
 */
public class GpsMetadataReplacer {

    // Famous landmarks with their coordinates (latitude, longitude)
    private static final List<LocationInfo> FAKE_LOCATIONS = List.of(
        new LocationInfo("Sydney Opera House", -33.8568, 151.2153),
        new LocationInfo("Eiffel Tower, Paris", 48.8584, 2.2945),
        new LocationInfo("Statue of Liberty, NYC", 40.6892, -74.0445),
        new LocationInfo("Big Ben, London", 51.5007, -0.1246),
        new LocationInfo("Colosseum, Rome", 41.8902, 12.4922),
        new LocationInfo("Christ the Redeemer, Rio", -22.9519, -43.2105)
    );

    private record LocationInfo(String name, double latitude, double longitude) {}

    /**
     * Replaces GPS metadata in all JPEG images in the specified directory.
     * Each image gets a unique fake coordinate from famous world landmarks.
     *
     * @param imageDirectory the directory containing images to process
     */
    public static void replaceGpsInDirectory(final String imageDirectory) {
        final Path dirPath = Paths.get(imageDirectory);

        if (!Files.isDirectory(dirPath)) {
            System.err.println("Not a valid directory: " + imageDirectory);
            return;
        }

        try {
            final List<Path> jpegFiles = Files.list(dirPath)
                .filter(GpsMetadataReplacer::isJpegFile)
                .sorted()
                .toList();

            System.out.println("Found " + jpegFiles.size() + " JPEG files to process");

            int locationIndex = 0;
            for (final Path jpegPath : jpegFiles) {
                final LocationInfo location = FAKE_LOCATIONS.get(locationIndex % FAKE_LOCATIONS.size());

                System.out.printf("Processing: %s -> %s (%.4f, %.4f)%n",
                    jpegPath.getFileName(),
                    location.name(),
                    location.latitude(),
                    location.longitude());

                try {
                    replaceGpsCoordinates(jpegPath.toFile(), location.latitude(), location.longitude());
                    System.out.println("  SUCCESS");
                } catch (Exception e) {
                    System.err.println("  FAILED: " + e.getMessage());
                }

                locationIndex++;
            }

            System.out.println("\nProcessing complete!");

        } catch (IOException e) {
            System.err.println("Error listing directory: " + e.getMessage());
        }
    }

    /**
     * Replaces GPS coordinates in a single JPEG file.
     *
     * @param jpegFile the JPEG file to modify
     * @param latitude the new latitude
     * @param longitude the new longitude
     */
    public static void replaceGpsCoordinates(final File jpegFile, final double latitude, final double longitude)
            throws Exception {

        // Read existing metadata
        final var metadata = Imaging.getMetadata(jpegFile);

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

        // Write to a temporary file first, then replace the original
        final File tempFile = File.createTempFile("gps_replace_", ".jpg");

        try (final FileOutputStream fos = new FileOutputStream(tempFile);
             final BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            new ExifRewriter().updateExifMetadataLossless(jpegFile, bos, outputSet);
        }

        // Replace original with modified file
        Files.delete(jpegFile.toPath());
        Files.move(tempFile.toPath(), jpegFile.toPath());
    }

    private static boolean isJpegFile(final Path path) {
        final String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg");
    }

    /**
     * Main method to run the GPS replacement on the test images directory.
     */
    public static void main(String[] args) {
        final String testImagesPath;

        if (args.length > 0) {
            testImagesPath = args[0];
        } else {
            // Default path relative to metadata-explorer/app directory
            testImagesPath = Paths.get("../quarkus-cli-image-labeler/src/test/resources/test-images")
                .toAbsolutePath()
                .normalize()
                .toString();
        }

        System.out.println("GPS Metadata Replacer");
        System.out.println("=====================");
        System.out.println("Target directory: " + testImagesPath);
        System.out.println();

        replaceGpsInDirectory(testImagesPath);
    }
}
