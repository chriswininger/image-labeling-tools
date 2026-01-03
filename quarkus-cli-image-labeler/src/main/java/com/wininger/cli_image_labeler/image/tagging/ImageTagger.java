package com.wininger.cli_image_labeler.image.tagging;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

@SystemMessage("""
    You are bot that tags images. You can create your own tags based on what you see but,
    be sure to use the following tags if any apply:

    ```
    person, building, flower, flowers, tree, trees, animal, animals, chicken, bird
    ``` 
    """)
public interface ImageTagger {
    @UserMessage("Generate tags based on this image {{it}}")
    ImageInfo generateTags(ImageContent imageContent);
}
