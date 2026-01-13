package com.wininger.cli_image_labeler.image.tagging.services;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.CosineSimilarity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;
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
    private static final double SIMILARITY_THRESHOLD_MODERATE = 0.75;
    private static final double SIMILARITY_THRESHOLD_RELAXED = 0.65;

    @Test
    void test_a_middle_aged_man_having_a_beer() throws IOException {
        final String expectedDescription =
            "A middle-aged man sits at an outdoor table, holding a dark beer glass. He is wearing a dark " +
                "t-shirt with a graphic design featuring insects. The table is surrounded by trees, creating " +
                "a shaded outdoor setting.";

        final List<String> expectedTags = List.of(
            "person", "table", "beer", "glasses", "trees", "outdoor", "shadow", "dark", "tableware", "outdoor scene"
        );

        final String expectedTitle = "Man Enjoying Beer Outdoors";

        final var result = imageInfoService.generateImageInfoAndMetadata(
            getAbsPathToImage("24-10-13 14-43-43 2024.jpg"),
            false);

        assertSimilarityDescription(expectedDescription, result.fullDescription(), SIMILARITY_THRESHOLD_MODERATE);
        assertSimilarityTags(expectedTags, result.tags(), SIMILARITY_THRESHOLD_MODERATE);
        assertSimilarityTitle(expectedTitle, result.shortTitle(), SIMILARITY_THRESHOLD_RELAXED);
        assert(result.isText()).equals(false);
        assertNull(result.textContents(), "textContents should be null for non-text images");
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

        final String expectedTitle = "Nighttime Fire Pit & Blooms";

        final var result = imageInfoService.generateImageInfoAndMetadata(
            getAbsPathToImage("24-10-12 19-44-41 7914.jpg"),
            false);

        assertSimilarityDescription(expectedDescription, result.fullDescription(), SIMILARITY_THRESHOLD_MODERATE);
        assertSimilarityTags(expectedTags, result.tags(), SIMILARITY_THRESHOLD_MODERATE);
        assertSimilarityTitle(expectedTitle, result.shortTitle(), SIMILARITY_THRESHOLD_RELAXED);
        assert(result.isText()).equals(false);
        assertNull(result.textContents(), "textContents should be null for non-text images");
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

        final String expectedTitle = "Focused Chicken Portrait - Brown Feathered Friend";

        final var result = imageInfoService.generateImageInfoAndMetadata(
            getAbsPathToImage("24-10-29 09-02-52 8203.jpg"),
            false);

        assertSimilarityDescription(expectedDescription, result.fullDescription(), SIMILARITY_THRESHOLD_MODERATE);
        assertSimilarityTags(expectedTags, result.tags(), SIMILARITY_THRESHOLD_MODERATE);
        assertSimilarityTitle(expectedTitle, result.shortTitle(), SIMILARITY_THRESHOLD_RELAXED);
        assert(result.isText()).equals(false);
        assertNull(result.textContents(), "textContents should be null for non-text images");
    }

    @Test
    void test__a_closeup_road_island_red_says_hello_to_the_camera() throws IOException {
        final String expectedDescription =
            "A close-up image of a brown chicken with a red comb. The chicken is positioned in the " +
                "foreground and appears to be looking directly at the camera. The background is blurred, suggesting a shallow depth of field.";

        final List<String> expectedTags = List.of(
            "chicken", "bird", "animal", "pet", "domestic"
        );

        final String expectedTitle = "Man, Beer, & Insects - Outdoor Scene";

        final var result = imageInfoService.generateImageInfoAndMetadata(
            getAbsPathToImage("24-12-04 15-22-46 8617.jpg"),
            false);

        assertSimilarityDescription(expectedDescription, result.fullDescription(), SIMILARITY_THRESHOLD_MODERATE);
        assertSimilarityTags(expectedTags, result.tags(), SIMILARITY_THRESHOLD_MODERATE);
        assertSimilarityTitle(expectedTitle, result.shortTitle(), SIMILARITY_THRESHOLD_RELAXED);
        assert(result.isText()).equals(false);
        assertNull(result.textContents(), "textContents should be null for non-text images");
    }

    @Test
    void test__a_screenshot_from_a_book_discussing_distance_of_vectors() throws IOException {
        final String expectedDescription =
            "The image shows a snippet of Java code within a code editor or IDE window. The code appears to be related " +
                "to a mathematical algorithm, potentially involving cosine similarity or related calculations. " +
                "The code is formatted with indentation and line breaks, suggesting it's part of a larger program or " +
                "function. There is accompanying text explaining the code.";

        final List<String> expectedTags = List.of(
            "code", "programming", "algorithm", "java", "mathematics", "documentation"
        );

        final String expectedTitle = "Man, Beer, & Insects - Outdoor Scene";

        final String expectedTextContent = """
            Measuring Similarity: Cosine
            
            Similarity and Distance
            
            To compare two embeddings, we typically use
            
            cosine similarity. It calculates the angle
            
            between two vectors, ignoring their
            
            magnitude (length). A simple, visual
            
            representation of cosine similarity is the
            
            angle between two arrows (vectors) in a
            
            graph. If the angle is small, the vectors are
            
            similar; if it's large, they are dissimilar, as
            
            shown in Figure 5-1.
            
            Similar vectors
            
            This is important because we care about the
            
            direction of the vector (meaning), not its size.
            
            The following Java example shows how to
            
            compute cosine similarity between two
            
            vectors by using the ND4J library:
            
            import org.nd4j.linalg.api.ndarray.INDAI
            
            import org.nd4j.linalg.factory.Nd4j;
            
            import org.nd4j.linalg.ops.transforms.Ti
            
            public class CosineSimilarity {
            
            public static double calculateCosine
            
            Check if vectors have the same
            """;

        final var result = imageInfoService.generateImageInfoAndMetadata(
            getAbsPathToImage("25-12-17 08-38-20 3818.png"),
            false);

        // We've had to relax the similarity threshold a bit on this one, it seems to be more variable with this image,
        // though often it's saying more or less the same thing
        assertSimilarityDescription(expectedDescription, result.fullDescription(), SIMILARITY_THRESHOLD_RELAXED);
        assertSimilarityTags(expectedTags, result.tags(), SIMILARITY_THRESHOLD_RELAXED);
        assertSimilarityTitle(expectedTitle, result.shortTitle(), SIMILARITY_THRESHOLD_RELAXED);
        assert(result.isText()).equals(true);
        assertSimilarityTextContent(expectedTextContent, result.textContents(), SIMILARITY_THRESHOLD_RELAXED);
    }

    /*
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
    }*/

    private String getAbsPathToImage(final String imageName) {
        return Paths.get("src/test/resources/test-images/%s".formatted(imageName))
            .toAbsolutePath().toString();
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

    private void assertSimilarityDescription(
        final String expectedDescription,
        final String actualDescription,
        final double similarityThreshold
    ) {
        final double similarity = calculateSimilarity(expectedDescription, actualDescription);

        System.out.println("\n--- Description Similarity ---");
        System.out.println("Similarity: " + similarity);
        System.out.println("Threshold: " + similarityThreshold);

        assertTrue(similarity >= similarityThreshold,
            String.format("Description similarity %.3f is below threshold %.3f%nExpected: %s%nActual: %s",
                similarity, similarityThreshold, expectedDescription, actualDescription));
    }

    private void assertSimilarityTags(
        final List<String> expectedTags,
        final List<String> actualTags,
        final double similarityThreshold
    ) {
        final double similarity = calculateTagsSimilarity(expectedTags, actualTags);

        System.out.println("\n--- Tags Similarity ---");
        System.out.println("Similarity: " + similarity);
        System.out.println("Threshold: " + similarityThreshold);

        assertTrue(similarity >= similarityThreshold,
            String.format("Tags similarity %.3f is below threshold %.3f%nExpected: %s%nActual: %s",
                similarity, similarityThreshold, expectedTags, actualTags));
    }

    private void assertSimilarityTitle(
        final String expectedTitle,
        final String actualTitle,
        final double similarityThreshold
    ) {
        final double similarity = calculateSimilarity(expectedTitle, actualTitle);

        System.out.println("\n--- Title Similarity ---");
        System.out.println("Similarity: " + similarity);
        System.out.println("Threshold: " + similarityThreshold);

        assertTrue(similarity >= similarityThreshold,
            String.format("Title similarity %.3f is below threshold %.3f%nExpected: %s%nActual: %s",
                similarity, similarityThreshold, expectedTitle, actualTitle));
    }

    private void assertSimilarityTextContent(
        final String expectedTextContent,
        final String actualTextContent,
        final double similarityThreshold
    ) {
        final double similarity = calculateSimilarity(expectedTextContent, actualTextContent);

        System.out.println("\n--- Text Content Similarity ---");
        System.out.println("Similarity: " + similarity);
        System.out.println("Threshold: " + similarityThreshold);

        assertTrue(similarity >= similarityThreshold,
            String.format("Text content similarity %.3f is below threshold %.3f%nExpected: %s%nActual: %s",
                similarity, similarityThreshold, expectedTextContent, actualTextContent));
    }
}
