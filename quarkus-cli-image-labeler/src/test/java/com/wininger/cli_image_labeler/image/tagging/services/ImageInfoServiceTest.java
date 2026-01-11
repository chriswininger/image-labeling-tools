package com.wininger.cli_image_labeler.image.tagging.services;

import com.wininger.cli_image_labeler.image.tagging.dto.ImageInfo;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

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

    /**
     * Test processing of image "24-10-13 14-43-43 2024.jpg".
     * 
     * This test currently just prints the output to help establish baseline
     * expectations for future similarity-based assertions.
     */
    @Test
    void testProcessImage_24_10_13_14_43_43_2024() {
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
}
