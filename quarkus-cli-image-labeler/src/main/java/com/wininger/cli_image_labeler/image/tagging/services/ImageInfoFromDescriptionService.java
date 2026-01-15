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
      - doesContainText: Briefly explain whether the image contains any visible text and what it says \
      (e.g., "Contains a sign reading Welcome", "Shows a book page with paragraphs", "Is an explanation a concept", "No visible text")
      
      Image description:
      ======
      {{it}}
      ======
      """)
  ImageInfoFromDescriptionModelResponse extractImageInfoFromDetailedImageDescription(String detailedDescription);
}
