package com.wininger.cli_image_labeler.image.tagging.dto;

import dev.langchain4j.model.output.structured.Description;

public record ImageInfoTitleModelResponse(
    @Description("A very short title for the image (max 100 characters). REQUIRED field.") String shortTitle
) {}
