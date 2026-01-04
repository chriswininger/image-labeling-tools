package com.wininger.cli_image_labeler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wininger.cli_image_labeler.image.tagging.ImageInfo;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.output.JsonSchemas;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "generate-image-tags", mixinStandardHelpOptions = true)
public class GenerateImageTagsCommand implements Runnable {
    @Parameters(paramLabel = "<image-path>", description = "The path to an image you want to identify")
    String imagePath;

    @Override
    public void run() {
        final var responseFormat = ResponseFormat.builder()
            .type(ResponseFormatType.JSON)
            .jsonSchema(JsonSchemas.jsonSchemaFrom(ImageInfo.class).get())
            .build();

        final var model = OllamaChatModel.builder()
            .modelName("gemma3:4b")
            .baseUrl("http://localhost:11434/")
            .responseFormat(responseFormat)
            .build();

        final ImageContent imageContent = getImageContent(imagePath);
        /*ImageTagger imageTagger = AiServices.builder(ImageTagger.class)
            .chatModel(model)
            .build();


        final ImageInfo imageInfo = imageTagger.generateTags(imageContent);
        System.out.println("Image Info: " + imageInfo);
        */

        final TextContent question = TextContent.from(
            "Generate an image description and tags based on this image. " +
            "You are a bot that tags images. You can create your own tags based on what you see but, " +
            "be sure to use the following tags if any apply: person, building, flower, flowers, tree, trees, animal, animals, chicken, bird. " +
            "Return a JSON object with 'tags' (array of strings) and 'fullDescription' (string)."
        );
        final UserMessage userMessage = UserMessage.from(question, imageContent);
        final ChatResponse chatResponse = model.chat(userMessage);
    
        // Parse the JSON response into ImageInfo
        final String jsonResponse = chatResponse.aiMessage().text();
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final ImageInfo imageInfo = mapper.readValue(jsonResponse, ImageInfo.class);
            System.out.println("Image Info: " + imageInfo);
        } catch (Exception e) {
            System.err.println("Failed to parse JSON response: " + e.getMessage());
            System.err.println("Raw response: " + jsonResponse);
            throw new RuntimeException("Could not parse image info from response", e);
        }
    }

    private String getMimeType(final String imagePath) {
        try {
            final String mimeType = Files.probeContentType(Path.of(imagePath));
            if (mimeType == null) {
                throw new RuntimeException("Couldn't extract mimetype from (value null): " + imagePath);
            }

            return mimeType;
        } catch(IOException ex) {
            throw new RuntimeException("Couldn't extract mimetype from: " + imagePath);
        }
    }

    private ImageContent getImageContent(final String imagePath) {
        try {
            final byte[] bytes = Files.readAllBytes(Paths.get(imagePath));
            final String base64Img = Base64.getEncoder().encodeToString(bytes);
            final String mimeType = getMimeType(imagePath);

            // Try using the file path directly - Ollama supports file paths
            // If that doesn't work, fall back to base64
            Path path = Paths.get(imagePath);
            if (path.isAbsolute()) {
                return ImageContent.from(path.toUri().toString());
            } else {
                return ImageContent.from(base64Img, mimeType);
            }
        } catch(IOException ex) {
            throw new RuntimeException("Could not parse image: " + imagePath);
        }
    }
}
