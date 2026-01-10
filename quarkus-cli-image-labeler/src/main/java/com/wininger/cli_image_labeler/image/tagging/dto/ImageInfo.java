package com.wininger.cli_image_labeler.image.tagging.dto;

import java.util.List;

public record ImageInfo(
   List<String> tags,
    String fullDescription,
    String shortTitle,
    Boolean isText,

    // not set by the model, set by us
    String thumbnailName
) { }
