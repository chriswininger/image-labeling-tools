package com.wininger.cli_image_labeler.image.tagging.services;

import com.wininger.cli_image_labeler.image.tagging.dto.model_responses.ImageInfoFromDescriptionModelResponse;
import dev.langchain4j.service.UserMessage;

public interface ImageInfoFromDescriptionService
{
  @UserMessage("""
      Based on the following detailed description of an image, extract structured information.
      
      IMPORTANT: Return valid JSON only. Use straight double quotes ("), never curly/smart quotes.
      
      Generate:
      - tags: A list of 5-15 relevant tags (keywords for subjects, objects, settings, colors, themes)
      - fullDescription: A concise description summarizing the key elements (1-3 sentences)
      - shortTitle: A very short title (max 100 characters)
      - isText: Whether the image contains substantial visible text (true/false)
      
      Image description:
      {{it}}
      """)
  ImageInfoFromDescriptionModelResponse extractImageInfoFromDetailedImageDescription(String detailedDescription);
}
