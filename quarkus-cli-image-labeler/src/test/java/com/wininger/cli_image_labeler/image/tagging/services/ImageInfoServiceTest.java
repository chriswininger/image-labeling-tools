package com.wininger.cli_image_labeler.image.tagging.services;

import com.wininger.cli_image_labeler.image.tagging.dto.ImageInfo;
import com.wininger.cli_image_labeler.image.tagging.dto.ImageInfoIsTextModelResponse;
import com.wininger.cli_image_labeler.image.tagging.dto.InitialImageInfo;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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


    @Test
    void test_a_middle_aged_man_having_a_beer() throws IOException {
        // description: A middle-aged man sits at an outdoor table, holding a dark beer glass. He is wearing a dark
        //     t-shirt with a graphic design featuring insects. The table is surrounded by trees, creating a shaded outdoor setting.
        // tags: person, table, beer, glasses, trees, outdoor, shadow, dark, tableware, outdoor scene
        // InitialImageInfo[tags=[person, table, beer, trees, outdoor, drinks, tableware, shadow, summer, glasses, drinks], fullDescription=A man sits at an outdoor table, enjoying a beer. He is wearing a black shirt with a graphic design featuring insects. Trees surround the table, creating a shaded and natural setting. The overall impression is a relaxed and casual summer scene.]
        runWithImage("24-10-13 14-43-43 2024.jpg");
    }

    @Test
    void test_a_roaring_fire_pit() throws IOException {
        // tags: [fire, fire pit, garden, flowers, plants, night, outdoor, evening, dark, fence, vegetation, garden, night, darkness]
        // description: A dark outdoor scene featuring a fire pit with a burning fire and a decorative metal fence
        //     surrounding it. The fence is surrounded by lush green plants and flowers in pots. There are dark trees
        //     in the background, suggesting a nighttime setting. The scene is shrouded in darkness with only the fire
        //     and the lights from the fire pit providing illumination.
        runWithImage("24-10-12 19-44-41 7914.jpg");
    }

    @Test
    void test__a_moulting_chicken() throws IOException {
        // tags: [chicken, bird, animal, feathered, domestic fowl, farm animal, animal, bird, domestic]
        // description: A single, striking chicken with black and white feathered plumage and a bright red comb, is
        // standing on a patch of green grass. The chicken is positioned in the foreground, taking up a significant
        // portion of the image. The background is blurred, suggesting a grassy field. The lighting appears to be natural.
        runWithImage("24-10-29 09-02-52 8203.jpg");
    }

    @Test
    void test__a_closeup_road_island_red_says_hello_to_the_camera() throws IOException {
            // tags: [chicken, bird, animal, pet, domestic]
            // description: A close-up image of a brown chicken with a red comb. The chicken is positioned in the
            // foreground and appears to be looking directly at the camera. The background is blurred, suggesting a shallow depth of field.
            runWithImage("24-12-04 15-22-46 8617.jpg");
    }

    @Test
    void test__a_screenshot_from_a_book_discussing_distance_of_vectors() throws IOException {
        // tags: [diagram, formula, math, mathematics, equation, symbols, vectors, linear algebra, NDArray, matrix]
        // description: The image is a diagram related to linear algebra, specifically dealing with NDArrays and vectors.
        // It showcases a mathematical equation represented using symbols and vectors, likely illustrating concepts from
        // linear algebra.
        runWithImage("25-12-17 08-38-20 3818.png");
    }

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

    /**
     * Test processing of image "24-10-13 14-43-43 2024.jpg".
     *
     * This test currently just prints the output to help establish baseline
     * expectations for future similarity-based assertions.
     */
    @Disabled
    @Test
    void testProcessImage_24_10_13_14_43_43_2024() {
        // version I like: !!! INFO Test: InitialImageInfo[tags=[person, table, beer, trees, outdoor, drinks, tableware, shadow, summer, glasses, drinks], fullDescription=A man sits at an outdoor table, enjoying a beer. He is wearing a black shirt with a graphic design featuring insects. Trees surround the table, creating a shaded and natural setting. The overall impression is a relaxed and casual summer scene.]
        // Get the path to the test image
        final Path testImagePath = Paths.get("src/test/resources/test-images/24-10-13 14-43-43 2024.jpg")
                .toAbsolutePath();

        System.out.println("=".repeat(80));
        System.out.println("Processing image: " + testImagePath);
        System.out.println("=".repeat(80));

        // Process the image (don't keep thumbnails for tests)
        final ImageInfo result = imageInfoService.generateImageInfoAndMetadata(
                testImagePath.toString(),
                false
        );

        // Print the results for inspection
        System.out.println("\n" + "=".repeat(80));
        System.out.println("IMAGE INFO RESULTS");
        System.out.println("=".repeat(80));

        System.out.println("\n--- Tags ---");
        if (result.tags() != null) {
            result.tags().forEach(tag -> System.out.println("  • " + tag));
        } else {
            System.out.println("  (no tags)");
        }

        System.out.println("\n--- Short Title ---");
        System.out.println("  " + (result.shortTitle() != null ? result.shortTitle() : "(no title)"));

        System.out.println("\n--- Full Description ---");
        System.out.println("  " + (result.fullDescription() != null ? result.fullDescription() : "(no description)"));

        System.out.println("\n--- Is Text ---");
        System.out.println("  " + result.isText());

        if (Boolean.TRUE.equals(result.isText()) && result.textContents() != null) {
            System.out.println("\n--- Text Contents (OCR) ---");
            System.out.println("  " + result.textContents());
        }

        System.out.println("\n--- Thumbnail Name ---");
        System.out.println("  " + (result.thumbnailName() != null ? result.thumbnailName() : "(no thumbnail)"));

        System.out.println("\n" + "=".repeat(80));
        System.out.println("END OF RESULTS");
        System.out.println("=".repeat(80));

        // For now, we just verify we got a result without throwing an exception
        // Future: Add cosine similarity checks against baseline outputs
        assert result != null : "Result should not be null";
        assert result.tags() != null : "Tags should not be null";
        assert result.fullDescription() != null : "Description should not be null";
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

}
