package com.wininger.cli_image_labeler.image.tagging.services;

import com.wininger.cli_image_labeler.image.tagging.dto.ImageInfoIsTextModelResponse;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.service.UserMessage;

public interface ImageInfoIsTextService {
  @UserMessage("""
      Determine if this image is primarily focused on text or written content.
      
      Answer YES (isText=true) if the image is:
      - A screenshot of text, code, documentation, or an article
      - A diagram with mathematical formulas, equations, or technical notation
      - A picture of a sign, label, or document
      - Any image where reading the text is the main purpose
      
      Answer NO (isText=false) if the image is:
      - A photograph of people, animals, landscapes, or objects
      - An image where text is incidental (like a logo in the background)
      
      IMPORTANT: First explain your reasoning, then set isText based on that reasoning.
      If your reasoning mentions code, formulas, documentation, or text content, isText MUST be true.
      """)
  ImageInfoIsTextModelResponse determineIfIsText(@UserMessage ImageContent imageContent);
}
