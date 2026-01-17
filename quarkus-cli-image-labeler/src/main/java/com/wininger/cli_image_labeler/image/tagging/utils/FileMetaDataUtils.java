package com.wininger.cli_image_labeler.image.tagging.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;

public class FileMetaDataUtils
{
  public static Map<String, Double> getGeoLocation(final String imagePath) throws ImageProcessingException, IOException {
    final Map<String, Double> result = new HashMap<>();
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

    return result;
  }

  public static Date getCreatedOn(final String imagePath) throws ImageProcessingException, IOException {
    final Map<String, Object> result = new HashMap<>();
    final File imageFile = new File(imagePath);

    final Metadata metadata = ImageMetadataReader.readMetadata(imageFile);

    // Extract EXIF data (focal length, date taken)
    final ExifSubIFDDirectory exifDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

      // Date/time original (created on)
      if (exifDirectory.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
        return exifDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
      } else {
        return null;
      }
    }

  /**
   * Gets the file creation time from the filesystem.
   * Note: On some filesystems (e.g., ext3), creation time may not be available
   * and will return the same value as last modified time.
   */
  public static Date getFileCreatedAt(final String filePath) throws IOException {
    final Path path = Path.of(filePath);
    final BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
    return new Date(attrs.creationTime().toMillis());
  }

  /**
   * Gets the last modified time from the filesystem.
   */
  public static Date getFileLastModified(final String filePath) throws IOException {
    final Path path = Path.of(filePath);
    final BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
    return new Date(attrs.lastModifiedTime().toMillis());
  }
}
