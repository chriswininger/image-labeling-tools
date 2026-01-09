package com.wininger.cli_image_labeler.image.tagging.exceptions;

public class ImageWriteException
    extends ImageProcessingException
{
  public ImageWriteException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
