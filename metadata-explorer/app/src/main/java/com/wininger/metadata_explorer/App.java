package com.wininger.metadata_explorer;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class App {
    public static void main(String[] args) {
        final String imagePath = Paths.get("../quarkus-cli-image-labeler/src/test/resources/test-images/24-10-12 19-44-41 7914.jpg")
            .toAbsolutePath()
            .normalize()
            .toString();

        try {
            final Map<String, String> metadata = extractFileMetadata(imagePath);

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
    public static Map<String, String> extractFileMetadata(final String imagePath) throws ImageProcessingException, IOException {
        final Map<String, Object> result = new HashMap<>();
        final File imageFile = new File(imagePath);

        final Metadata metadata = ImageMetadataReader.readMetadata(imageFile);

       // System.out.println("========");
        return dump(metadata);
        //System.out.println("========");
        // Extract GPS coordinates
       /* final GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
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

        return result;*/
    }

    private static Map<String, String> dump(final Metadata metadata) {
        final var dir =metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        final Collection<Tag> tags = dir.getTags();

       return StreamSupport.stream(metadata.getDirectories().spliterator(), false)
              .flatMap(directory ->
                  directory.getTags().stream()
                      .map(tag -> Map.entry(
                          directory.getName() + "." + tag.getTagName(),
                          tag.getDescription()
                      ))
              )
              .collect(Collectors.toMap(
                  Map.Entry::getKey,
                  Map.Entry::getValue,
                  (left, right) -> right   // in case of duplicate keys
              ));

       /*
        tags.stream().map(t -> {
          final String name = t.getTagName();
          final String description = t.getDescription();

          System.out.printf("%s -> %s\n", name, description);
          return Map.entry(
              name,
              description);
          }).collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (left, right) -> right
          ));
        */
    }
}
