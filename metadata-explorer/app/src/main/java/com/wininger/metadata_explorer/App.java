package com.wininger.metadata_explorer;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        final String imagePath = "/home/chris/projects/image-labeling-tools/quarkus-cli-image-labeler/src/test/resources/test-images/24-10-12 19-44-41 7914.jpg";

        try {
            final Map<String, Object> metadata = extractFileMetadata(imagePath);

            System.out.println("Metadata for: " + imagePath);
            System.out.println("----------------------------------------");
            metadata.forEach((key, value) -> System.out.println(key + ": " + value));
        } catch (Exception e) {
            System.err.println("Error extracting metadata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extracts file metadata such as coordinates, focal length, and created date.
     *
     * @param imagePath absolute path to the image file
     * @return Map containing metadata fields
     */
    public static Map<String, Object> extractFileMetadata(final String imagePath) throws ImageProcessingException, IOException {
        final Map<String, Object> result = new HashMap<>();
        final File imageFile = new File(imagePath);

        final Metadata metadata = ImageMetadataReader.readMetadata(imageFile);

        // Extract GPS coordinates
        final GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (gpsDirectory != null && gpsDirectory.getGeoLocation() != null) {
            final var geoLocation = gpsDirectory.getGeoLocation();
            result.put("latitude", geoLocation.getLatitude());
            result.put("longitude", geoLocation.getLongitude());
        } else {
            result.put("latitude", null);
            result.put("longitude", null);
        }

        // Extract EXIF data (focal length, date taken)
        final ExifSubIFDDirectory exifDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (exifDirectory != null) {
            // Focal length
            if (exifDirectory.containsTag(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)) {
                result.put("focalLength", exifDirectory.getDescription(ExifSubIFDDirectory.TAG_FOCAL_LENGTH));
            } else {
                result.put("focalLength", null);
            }

            // Date/time original (created on)
            if (exifDirectory.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
                result.put("createdOn", exifDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL));
            } else {
                result.put("createdOn", null);
            }
        } else {
            result.put("focalLength", null);
            result.put("createdOn", null);
        }

        return result;
    }

    private void dump(final Metadata metadata) {
        final var grr = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class).getTagNameMap();
    }
}
