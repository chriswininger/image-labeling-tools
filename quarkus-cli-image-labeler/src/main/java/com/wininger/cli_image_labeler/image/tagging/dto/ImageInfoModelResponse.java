package com.wininger.cli_image_labeler.image.tagging.dto;

import java.util.List;

import dev.langchain4j.model.output.structured.Description;

/**
 * DTO for model responses. This excludes thumbnailName which is set by the application, not the model.
 */
public record ImageInfoModelResponse(
    @Description("A list of tags describing the image. REQUIRED field.") List<String> tags,
    @Description("A full generic description of the image contents. REQUIRED field.") String fullDescription,
    @Description("A very short title for the image (max 100 characters). REQUIRED field.") String shortTitle,
    @Description("True if the main focus of the image is text, false otherwise. REQUIRED field.") Boolean isText
) {
}
