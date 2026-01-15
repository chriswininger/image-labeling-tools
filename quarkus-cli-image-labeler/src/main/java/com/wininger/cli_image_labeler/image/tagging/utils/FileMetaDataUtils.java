package com.wininger.cli_image_labeler.image.tagging.utils;

import java.io.File;
import java.io.IOException;
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
}
