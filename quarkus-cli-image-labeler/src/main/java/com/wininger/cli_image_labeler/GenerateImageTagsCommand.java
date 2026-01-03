package com.wininger.cli_image_labeler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import com.wininger.cli_image_labeler.image.tagging.ImageInfo;
import com.wininger.cli_image_labeler.image.tagging.ImageTagger;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
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


        ImageTagger imageTagger = AiServices.builder(ImageTagger.class)
            .chatModel(model)
            .build();

        final ImageContent imageContent = getImageContent(imagePath);
        final ImageInfo imageInfo = imageTagger.generateTags(imageContent);
        System.out.println("Image Info: " + imageInfo);
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

            return ImageContent.from(base64Img, mimeType);
        } catch(IOException ex) {
            throw new RuntimeException("Could not parse image: " + imagePath);
        }
    }
}
