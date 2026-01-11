package com.wininger.cli_image_labeler.image.tagging.services;

import com.wininger.cli_image_labeler.image.tagging.dto.ImageInfo;
import com.wininger.cli_image_labeler.image.tagging.dto.ImageInfoIsTextModelResponse;
import com.wininger.cli_image_labeler.image.tagging.dto.InitialImageInfo;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.CosineSimilarity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

/**
 * Integration tests for ImageInfoService.
 *
 * These tests require a running Ollama instance with the required models:
 * - gemma3:4b (for image tagging and description)
 * - deepseek-ocr:3b (for OCR on text-heavy images)
 *
 * Since LLM outputs are non-deterministic, future assertions will use
 * cosine similarity to compare against expected baseline outputs rather
 * than exact string matching.
 */
@QuarkusTest
public class ImageInfoServiceTest {

    @Inject
    ImageInfoService imageInfoService;


    private static final String EMBEDDING_MODEL = "nomic-embed-text";
    private static final String OLLAMA_BASE_URL = "http://localhost:11434/";
    private static final double SIMILARITY_THRESHOLD = 0.80;

    @Test
    void test_a_middle_aged_man_having_a_beer() throws IOException {
        final String expectedDescription =
            "A middle-aged man sits at an outdoor table, holding a dark beer glass. He is wearing a dark " +
            "t-shirt with a graphic design featuring insects. The table is surrounded by trees, creating " +
            "a shaded outdoor setting.";

        final List<String> expectedTags = List.of(
            "person", "table", "beer", "glasses", "trees", "outdoor", "shadow", "dark", "tableware", "outdoor scene"
        );

        final InitialImageInfo result = runWithImage("24-10-13 14-43-43 2024.jpg");

        assertSimilarity(expectedDescription, expectedTags, result);
    }

    @Test
    void test_a_roaring_fire_pit() throws IOException {
        final String expectedDescription =
            "A dark outdoor scene featuring a fire pit with a burning fire and a decorative metal fence " +
            "surrounding it. The fence is surrounded by lush green plants and flowers in pots. There are dark trees " +
            "in the background, suggesting a nighttime setting. The scene is shrouded in darkness with only the fire " +
            "and the lights from the fire pit providing illumination.";

        final List<String> expectedTags = List.of(
            "fire", "fire pit", "garden", "flowers", "plants", "night", "outdoor", "evening", "dark", "fence", "vegetation", "darkness"
        );

        final InitialImageInfo result = runWithImage("24-10-12 19-44-41 7914.jpg");

        assertSimilarity(expectedDescription, expectedTags, result);
    }

    @Test
    void test__a_moulting_chicken() throws IOException {
        final String expectedDescription =
            "A single, striking chicken with black and white feathered plumage and a bright red comb, is " +
            "standing on a patch of green grass. The chicken is positioned in the foreground, taking up a significant " +
            "portion of the image. The background is blurred, suggesting a grassy field. The lighting appears to be natural.";

        final List<String> expectedTags = List.of(
            "chicken", "bird", "animal", "feathered", "domestic fowl", "farm animal", "domestic"
        );

        final InitialImageInfo result = runWithImage("24-10-29 09-02-52 8203.jpg");

        assertSimilarity(expectedDescription, expectedTags, result);
    }

    @Test
    void test__a_closeup_road_island_red_says_hello_to_the_camera() throws IOException {
        final String expectedDescription =
            "A close-up image of a brown chicken with a red comb. The chicken is positioned in the " +
            "foreground and appears to be looking directly at the camera. The background is blurred, suggesting a shallow depth of field.";

        final List<String> expectedTags = List.of(
            "chicken", "bird", "animal", "pet", "domestic"
        );

        final InitialImageInfo result = runWithImage("24-12-04 15-22-46 8617.jpg");

        assertSimilarity(expectedDescription, expectedTags, result);
    }

    @Test
    void test__a_screenshot_from_a_book_discussing_distance_of_vectors() throws IOException {
        final String expectedDescription =
            "The image is a diagram related to linear algebra, specifically dealing with NDArrays and vectors. " +
            "It showcases a mathematical equation represented using symbols and vectors, likely illustrating concepts from " +
            "linear algebra.";

        final List<String> expectedTags = List.of(
            "diagram", "formula", "math", "mathematics", "equation", "symbols", "vectors", "linear algebra", "NDArray", "matrix"
        );

        final InitialImageInfo result = runWithImage("25-12-17 08-38-20 3818.png");

        assertSimilarity(expectedDescription, expectedTags, result);
    }

    @Disabled
    @Test
    void test__a_screenshot_from_a_book_discussing_vector_stores() throws IOException {
        // !!! WRONG
        // tags: [chicken, rooster, farm, barn, animals, outdoor, rural, agriculture, domestic, male]
        //description: A photograph depicts a brown rooster standing prominently in front of a red barn. The rooster is
        // facing the camera, displaying its comb and wattles. The barn is a traditional wooden structure with a red roof.
        // The scene suggests a rural farm setting with livestock and agricultural activity.
        // Several chickens are visible in the background, adding to the farmyard atmosphere.
        runWithImage("25-12-17 08-50-55 3819.png");
    }

    private InitialImageInfo runWithImage(final String imageName) throws IOException {
        final Path testImagePath = Paths.get("src/test/resources/test-images/%s".formatted(imageName))
            .toAbsolutePath();

        final var originalImage = ImageIO.read(Paths.get(testImagePath.toString()).toFile());

        final ImageContent imageContent = getImageContentAndResizeIt(originalImage,
            testImagePath.toAbsolutePath().toString());

        final ChatModel chatModelTitle = OllamaChatModel.builder()
            .modelName("gemma3:4b")
            .baseUrl("http://localhost:11434/")
            .build();

        final InitialImageInfoService titleTx = AiServices.builder(InitialImageInfoService.class)
            .chatModel(chatModelTitle)
            .build();

        final InitialImageInfo info = titleTx.getImageInfo(imageContent);

        System.out.printf("== %s ==%n", imageName);
        // System.out.println("title: " + info.title());
        System.out.println("tags: " + info.tags());
        System.out.println("description: " + info.fullDescription());
        System.out.println("========");

        return info;
    }

    private ImageContent getImageContentAndResizeIt(BufferedImage originalImage, final String imagePath) {
        try {
            final int originalWidth = originalImage.getWidth();
            final int originalHeight = originalImage.getHeight();

            final BufferedImage resizedImage = resizeImage(originalImage, 1024);

            final int resizedWidth = resizedImage.getWidth();
            final int resizedHeight = resizedImage.getHeight();

            // Convert a resized image to JPEG with compression for smaller file size
            // Always use JPEG to ensure good compression regardless of source format
            final byte[] imageBytes = imageToJpegBytes(resizedImage, 0.85f); // 85% quality
            final long resizedFileSize = imageBytes.length;

            // Log the resize information
            final long originalFileSize = Files.size(Paths.get(imagePath));
            System.out.printf("Image resize: %dx%d (%.1f KB) -> %dx%d (%.1f KB)%n",
                originalWidth, originalHeight, originalFileSize / 1024.0,
                resizedWidth, resizedHeight, resizedFileSize / 1024.0);

            final String base64Img = Base64.getEncoder().encodeToString(imageBytes);
            final long base64Size = base64Img.length();
            System.out.printf("Base64 size: %.1f KB%n", base64Size / 1024.0);

            return ImageContent.from(base64Img, "image/jpeg");
        }
        catch (IOException ex) {
            throw new RuntimeException("Could not parse image: " + imagePath, ex);
        }
    }

    private BufferedImage resizeImage(final BufferedImage originalImage, final int maxDimension) {
        final int originalWidth = originalImage.getWidth();
        final int originalHeight = originalImage.getHeight();

        // If image is already smaller than max dimension, return original
        if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
            return originalImage;
        }

        // Calculate new dimensions maintaining aspect ratio
        final double scale = Math.min(
            (double) maxDimension / originalWidth,
            (double) maxDimension / originalHeight
        );

        final int newWidth = (int) (originalWidth * scale);
        final int newHeight = (int) (originalHeight * scale);

        // Create resized image with better quality rendering
        final BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

        final Graphics2D g2d = resizedImage.createGraphics();
        try {
            // Use high-quality rendering hints
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        }
        finally {
            g2d.dispose();
        }

        return resizedImage;
    }

    /**
     * Converts a BufferedImage to a JPEG byte array with specified quality. This ensures good compression regardless of
     * source format.
     */
    private byte[] imageToJpegBytes(final BufferedImage image, final float quality) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Get JPEG writer
        final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        final ImageWriteParam param = writer.getDefaultWriteParam();

        // Enable compression
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        }
        finally {
            writer.dispose();
        }

        return baos.toByteArray();
    }

    /**
     * Calculates cosine similarity between two text strings using embeddings.
     * Returns a value between 0.0 and 1.0, where 1.0 means identical meaning.
     */
    private double calculateSimilarity(final String text1, final String text2) {
        final EmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
            .baseUrl(OLLAMA_BASE_URL)
            .modelName(EMBEDDING_MODEL)
            .build();

        final Embedding embedding1 = embeddingModel.embed(text1).content();
        final Embedding embedding2 = embeddingModel.embed(text2).content();

        return CosineSimilarity.between(embedding1, embedding2);
    }

    /**
     * Calculates cosine similarity between two lists of tags.
     * Joins tags into comma-separated strings and compares their semantic embeddings.
     */
    private double calculateTagsSimilarity(final List<String> expectedTags, final List<String> actualTags) {
        final String expectedTagsStr = String.join(", ", expectedTags);
        final String actualTagsStr = String.join(", ", actualTags);
        return calculateSimilarity(expectedTagsStr, actualTagsStr);
    }

    private void assertSimilarity(
        final String expectedDescription,
        final List<String> expectedTags,
        final InitialImageInfo result
    ) {
        // Check semantic similarity of description
        final double descriptionSimilarity = calculateSimilarity(expectedDescription, result.fullDescription());

        // Check semantic similarity of tags
        final double tagsSimilarity = calculateTagsSimilarity(expectedTags, result.tags());

        printSimilarityResults(descriptionSimilarity, tagsSimilarity);

        assertTrue(descriptionSimilarity >= SIMILARITY_THRESHOLD,
            String.format("Description similarity %.3f is below threshold %.3f%nExpected: %s%nActual: %s",
                descriptionSimilarity, SIMILARITY_THRESHOLD, expectedDescription, result.fullDescription()));

        assertTrue(tagsSimilarity >= SIMILARITY_THRESHOLD,
            String.format("Tags similarity %.3f is below threshold %.3f%nExpected: %s%nActual: %s",
                tagsSimilarity, SIMILARITY_THRESHOLD, expectedTags, result.tags()));
    }

    private void printSimilarityResults(final double descriptionSimilarity, final double tagsSimilarity) {
        System.out.println("\n--- Similarity Results ---");
        System.out.println("Description similarity: " + descriptionSimilarity);
        System.out.println("Tags similarity: " + tagsSimilarity);
        System.out.println("Threshold: " + SIMILARITY_THRESHOLD);
    }
}
