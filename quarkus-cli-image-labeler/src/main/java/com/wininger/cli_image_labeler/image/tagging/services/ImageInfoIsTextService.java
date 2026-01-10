package com.wininger.cli_image_labeler.image.tagging.services;

import com.wininger.cli_image_labeler.image.tagging.dto.ImageInfoIsTextModelResponse;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.service.UserMessage;

public interface ImageInfoIsTextService {
  @UserMessage("Determine if the focus of this image is primarily text, for example a screen shot of an article, " +
      "or a picture of a sign or product")
  ImageInfoIsTextModelResponse determineIfIsText(@UserMessage ImageContent imageContent);
}
