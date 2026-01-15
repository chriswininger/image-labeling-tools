package com.wininger.cli_image_labeler.image.tagging.dto.model_responses;

import dev.langchain4j.model.output.structured.Description;

import java.util.List;

public record ImageInfoFromDescriptionModelResponse(
    @Description("A list of tags describing the image based on the description. Include relevant keywords for subjects, objects, settings, colors, and themes. REQUIRED field.")
    List<String> tags,

    @Description("A full generic description of the image contents, summarizing the key elements. REQUIRED field.")
    String fullDescription,

    @Description("A very short title for the image (max 100 characters). REQUIRED field.")
    String shortTitle,

    @Description("Indicates whether there is substantial text visible in the image. REQUIRED field.")
    Boolean isText
) {}
