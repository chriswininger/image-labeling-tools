package com.wininger.cli_image_labeler.image.tagging.dto;

import dev.langchain4j.model.output.structured.Description;

// Super helpful discussion that lead to me getting this working with ImageContent
// https://github.com/langchain4j/langchain4j/issues/938#issuecomment-2468159630
public record ImageInfoIsTextModelResponse(
    @Description("True if the main focus of the image is text, false otherwise. REQUIRED field.")
    Boolean isText) { }
