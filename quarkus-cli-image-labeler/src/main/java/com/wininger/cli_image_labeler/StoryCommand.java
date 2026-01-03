package com.wininger.cli_image_labeler;

import com.wininger.cli_image_labeler.dto.story.Story;
import com.wininger.cli_image_labeler.dto.story.StoryInfo;
import com.wininger.cli_image_labeler.dto.transaction.TransactionInfo;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import picocli.CommandLine.Command;

@Command(name = "Story", mixinStandardHelpOptions = true)
public class StoryCommand implements Runnable {
    private static final String STORY = """
    Bob Vance's Best Day
Bob Vance ran his hand through his brown hair as he unlocked the showroom. As a refrigerator salesman at Vance Refrigeration, he’d learned that the best sales come from understanding what people need, not just what they want.
That morning, Mrs. Peterson came in looking frustrated. Her old fridge had broken, and she needed a replacement quickly.
"Tell me about your family," Bob said, pushing aside his brown hair with a practiced gesture.
"Three kids, always hungry," she laughed.
Bob showed her a model that was efficient, spacious, and within her budget. He explained the features clearly, and within an hour, she was signing the paperwork.
"Thank you, Bob. You actually listened to what I needed," she said.
As she left, Bob smiled. Being a good refrigerator salesman wasn’t about moving units—it was about solving problems, one customer at a time.        
            """;
    @Override
    public void run() {
        final ChatModel chatModel = OllamaChatModel.builder()
        .modelName("gemma3:4b")
        .baseUrl("http://localhost:11434/")
        .build();

        final Story tx = AiServices.builder(Story.class)
        .chatModel(chatModel)
        .build();

        final StoryInfo storyInfo = tx.extract(STORY);

        System.out.println("Extracted Story Info: " + storyInfo);
        System.out.println(storyInfo);
    }
}
