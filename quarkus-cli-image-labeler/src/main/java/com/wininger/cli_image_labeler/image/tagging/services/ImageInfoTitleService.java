package com.wininger.cli_image_labeler.image.tagging.services;

import com.wininger.cli_image_labeler.image.tagging.dto.model_responses.ImageInfoTitleModelResponse;
import dev.langchain4j.service.UserMessage;

public interface ImageInfoTitleService
{
  @UserMessage("Generate a short title for an image (max 100 characters) based on this description of it  {{it}}")
  ImageInfoTitleModelResponse extractTitle(String message);
}
