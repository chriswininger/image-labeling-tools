package com.wininger.cli_image_labeler.image.tagging.services;

import com.wininger.cli_image_labeler.image.tagging.dto.model_responses.ImageInfoFromDescriptionModelResponse;
import dev.langchain4j.service.UserMessage;

public interface ImageInfoFromDescriptionService
{
  @UserMessage("""
      Based on the following detailed description of an image, extract structured information.
      
      IMPORTANT: Return valid JSON only. Use straight double quotes ("), never curly/smart quotes.
      
      Generate:
      - tags: A list of 5-15 relevant tags. Start with high-level general tags (e.g., person, people, \
      building, flower, flowers, tree, trees, animal, animals, bird, cat, dog, chicken, car, food, landscape, \
      portrait, indoor, outdoor) then add more specific tags for subjects, settings, colors, and themes.
      - fullDescription: A concise description summarizing the key elements (1-3 sentences)
      - shortTitle: A very short title (max 100 characters)
      - isText: Whether the image focus is on text. Examples: Shot of a blog, book, explanation, or photos of signs, e.g.
      
      Image description:
      ======
      {{it}}
      ======
      """)
  ImageInfoFromDescriptionModelResponse extractImageInfoFromDetailedImageDescription(String detailedDescription);
}
