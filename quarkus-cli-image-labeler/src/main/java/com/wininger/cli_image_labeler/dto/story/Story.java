package com.wininger.cli_image_labeler.dto.story;

import dev.langchain4j.service.UserMessage;

public interface Story {
    @UserMessage("Extract information about the story, identify the main character and their attributes {{it}}")
    StoryInfo extract(String message);
}
