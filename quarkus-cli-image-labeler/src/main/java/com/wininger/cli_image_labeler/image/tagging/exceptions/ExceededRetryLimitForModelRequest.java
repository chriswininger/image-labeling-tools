package com.wininger.cli_image_labeler.image.tagging.exceptions;

public class ExceededRetryLimitForModelRequest extends ImageProcessingException
{
  public ExceededRetryLimitForModelRequest(final String message) {
    super(message, null);
  }
}
