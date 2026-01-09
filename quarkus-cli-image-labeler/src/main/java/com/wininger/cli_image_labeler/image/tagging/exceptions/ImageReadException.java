package com.wininger.cli_image_labeler.image.tagging.exceptions;

public class ImageReadException
    extends ImageProcessingException
{
  public ImageReadException(final String imagePath, final Throwable cause) {
    super("Could not read image file: '" + imagePath + "'", cause);
  }
}
