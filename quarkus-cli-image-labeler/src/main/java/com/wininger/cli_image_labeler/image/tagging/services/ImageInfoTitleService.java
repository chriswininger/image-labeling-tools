package com.wininger.cli_image_labeler.image.tagging.services;

import com.wininger.cli_image_labeler.image.tagging.dto.ImageInfoTitleModelResponse;
import dev.langchain4j.service.UserMessage;

public interface ImageInfoTitleService
{
  @UserMessage("Generate a short title for an image (max 100 characters) based on this desciption of it  {{it}}")
  ImageInfoTitleModelResponse extractTitle(String message);
}
