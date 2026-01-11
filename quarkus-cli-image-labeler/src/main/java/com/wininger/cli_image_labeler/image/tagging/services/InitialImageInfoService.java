package com.wininger.cli_image_labeler.image.tagging.services;

import com.wininger.cli_image_labeler.image.tagging.dto.InitialImageInfo;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.service.UserMessage;

public interface InitialImageInfoService {
  @UserMessage("Generate an image description and tags based on this image.\n" +
      "\n" +
      "      * You are a bot that tags images. You can create your own tags based on what you see but,\n" +
      "      * be sure to use the following tags if any apply: person, building, flower, flowers, tree, trees, animal, animals, chicken, bird.")
  InitialImageInfo getImageInfo(@UserMessage ImageContent imageContent);
}
