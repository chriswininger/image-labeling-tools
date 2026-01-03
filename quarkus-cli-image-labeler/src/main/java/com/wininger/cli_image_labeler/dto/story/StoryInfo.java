package com.wininger.cli_image_labeler.dto.story;

import java.util.List;

import dev.langchain4j.model.output.structured.Description;

public record StoryInfo(
    @Description("The main character of the story") String mainCharacterName,
    @Description("The main character's hair color") String mainCharacterHairColor,
    @Description("The main character's occupation") String mainCharacterOccupation,
    @Description("A list of short tags that describe the story") List<String> storyTags
) {}
