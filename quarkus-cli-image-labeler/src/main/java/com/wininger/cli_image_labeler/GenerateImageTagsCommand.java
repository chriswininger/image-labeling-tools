package com.wininger.cli_image_labeler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import com.wininger.cli_image_labeler.image.tagging.ImageInfo;
import com.wininger.cli_image_labeler.image.tagging.ImageTagger;

import dev.langchain4j.data.image.Image;
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

        final Image image = getImage(imagePath);
        final ImageInfo imageInfo = imageTagger.generateTags(image);
        System.out.println("Image Info: " + imageInfo);
    }

    private String encodeInputStreamToBase64(final File file) {
        try (final FileInputStream fis = new FileInputStream(file)) {
            byte[] fileBytes = new byte[(int) file.length()];
            fis.read(fileBytes); // Read bytes from the stream

            // Encode the byte array to a Base64 string
            return Base64.getEncoder().encodeToString(fileBytes);
        } catch (IOException e) {
            throw new RuntimeException("Coule not open the file: " + file.getName());
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

    private Image getImage(final String imagePath) {
        try {
            final byte[] bytes = Files.readAllBytes(Paths.get(imagePath));
            final String base64Img = Base64.getEncoder().encodeToString(bytes);
            final String mimeType = getMimeType(imagePath);

            return Image.builder()
                .base64Data(base64Img)
                .mimeType(mimeType)   // e.g. "image/png" or "image/jpeg"
                .build();
        } catch(IOException ex) {
            throw new RuntimeException("Could not parse image: " + imagePath);
        }
    }
}
