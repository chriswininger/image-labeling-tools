package com.wininger.cli_image_labeler.image.tagging;

import java.util.List;

import dev.langchain4j.model.output.structured.Description;

public record ImageInfo(
    @Description("A list of tags describing the image") List<String> tags,
    @Description("A full generic description of the image contents") String fullDescription,
    String thumbnailName
) {
    public ImageInfo(List<String> tags, String fullDescription) {
        this(tags, fullDescription, null);
    }
}
