package com.wininger.cli_image_labeler.image.tagging;

import java.util.List;

import dev.langchain4j.model.output.structured.Description;

public record ImageInfo(@Description("A list of tags describing the image") List<String> tags) {}
