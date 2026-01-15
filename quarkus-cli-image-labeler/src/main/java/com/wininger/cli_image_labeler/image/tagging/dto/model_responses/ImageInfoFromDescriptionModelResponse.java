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

    @Description("Briefly explain whether the image contains any visible text and what that text is about (e.g., 'Contains a sign reading Welcome', 'Shows a book page with paragraphs of text', 'Is an explanation a concept', 'No visible text'). REQUIRED field.")
    String doesContainText
) {}
