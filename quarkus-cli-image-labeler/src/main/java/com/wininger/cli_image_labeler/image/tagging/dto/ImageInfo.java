package com.wininger.cli_image_labeler.image.tagging.dto;

import java.util.Date;
import java.util.List;

public record ImageInfo(
   List<String> tags,
    String fullDescription,
    String shortTitle,
    Boolean isText,
    String textContents,

    // not set by the model, set by us
    String thumbnailName,

    // metadata extracted from file EXIF data
    Double gpsLatitude,
    Double gpsLongitude,
    Date imageTakenAt
) { }
