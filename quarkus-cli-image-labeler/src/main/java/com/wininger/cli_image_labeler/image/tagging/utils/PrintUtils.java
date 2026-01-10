package com.wininger.cli_image_labeler.image.tagging.utils;

import com.wininger.cli_image_labeler.image.tagging.dto.ImageInfo;

public class PrintUtils
{
  public static void printImageInfoResults(
      final ImageInfo imageInfo,
      final long startTime
  ) {
    final String tagsString = String.join(", ", imageInfo.tags());
    System.out.println("Title: " + imageInfo.shortTitle());
    System.out.println("Description: " + imageInfo.fullDescription());
    System.out.println("Tags: " + tagsString);
    System.out.println("isText: " + imageInfo.isText());
    if (imageInfo.isText()) {
      System.out.println("textContent: " + imageInfo.textContents());
    }
    System.out.println("Time Taken: " + getTimeTakenMessage(startTime, System.currentTimeMillis()));
  }

  public static String getTimeTakenMessage(final long startTime, final long endTime) {
    final long totalTimeMs = endTime - startTime;
    final double totalTimeSeconds = totalTimeMs / 1000.0D;
    final long totalTimeMinutes = (long) Math.floor(totalTimeSeconds / 60.0D);
    final long remainingSeconds = (long) (totalTimeSeconds % 60.0D);

    if (totalTimeMinutes > 0) {
      return "%s minutes and %s seconds".formatted(totalTimeMinutes, remainingSeconds);
    } else if (totalTimeSeconds > 0) {
      return "%s seconds".formatted(remainingSeconds);
    } else {
      return "%s milliseconds".formatted(totalTimeMs);
    }
  }
}
