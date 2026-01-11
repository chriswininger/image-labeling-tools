package com.wininger.cli_image_labeler.image.tagging.dto;

import dev.langchain4j.model.output.structured.Description;

import java.util.List;

public record InitialImageInfo(
    @Description("A list of tags describing the image. REQUIRED field.") List<String> tags,
    @Description("A full generic description of the image contents. REQUIRED field.") String fullDescription
    // @Description("A very short title for the image (max 100 characters). REQUIRED field.") String title
) {}
