package com.wininger.cli_image_labeler.image.tagging.dto;

import java.util.List;

import dev.langchain4j.model.output.structured.Description;

public record ImageInfo(
    @Description("A list of tags describing the image") List<String> tags,
    @Description("A full generic description of the image contents") String fullDescription,
    @Description("A very short title for the image (max 100 characters)") String shortTitle,
    @Description("True if the main focus of the image is text") Boolean isText,

    // not set by the model, set by us
    String thumbnailName
) {
    public ImageInfo(List<String> tags, String fullDescription) {
        this(tags, fullDescription, null, null, null);
    }

    public ImageInfo(List<String> tags, String fullDescription, String shortTitle, Boolean isText) {
        this(tags, fullDescription, shortTitle, isText, null);
    }
}
