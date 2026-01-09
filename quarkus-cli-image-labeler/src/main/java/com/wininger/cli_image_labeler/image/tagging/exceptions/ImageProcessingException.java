package com.wininger.cli_image_labeler.image.tagging.exceptions;

public class ImageProcessingException extends RuntimeException
{
  ImageProcessingException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
