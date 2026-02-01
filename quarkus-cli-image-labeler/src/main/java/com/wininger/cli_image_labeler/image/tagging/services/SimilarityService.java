package com.wininger.cli_image_labeler.image.tagging.services;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.CosineSimilarity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

/**
 * Service for calculating semantic similarity between text strings using embeddings.
 * Uses cosine similarity to compare vector representations of text.
 */
@ApplicationScoped
public class SimilarityService {

    private static final String EMBEDDING_MODEL = "nomic-embed-text";

    private final EmbeddingModel embeddingModel;

    @Inject
    public SimilarityService(
        @ConfigProperty(name = "il.ollama.url", defaultValue = "http://localhost:11434/") String ollamaUrl
    ) {
        this.embeddingModel = OllamaEmbeddingModel.builder()
            .baseUrl(ollamaUrl)
            .modelName(EMBEDDING_MODEL)
            .build();
    }

    /**
     * Calculates cosine similarity between two text strings using embeddings.
     * Returns a value between 0.0 and 1.0, where 1.0 means identical meaning.
     *
     * @param text1 the first text to compare
     * @param text2 the second text to compare
     * @return similarity score between 0.0 and 1.0
     */
    public double calculateSimilarity(final String text1, final String text2) {
        final Embedding embedding1 = embeddingModel.embed(text1).content();
        final Embedding embedding2 = embeddingModel.embed(text2).content();

        return CosineSimilarity.between(embedding1, embedding2);
    }

    /**
     * Calculates cosine similarity between two lists of tags.
     * Joins tags into comma-separated strings and compares their semantic embeddings.
     *
     * @param tags1 the first list of tags
     * @param tags2 the second list of tags
     * @return similarity score between 0.0 and 1.0
     */
    public double calculateTagsSimilarity(final List<String> tags1, final List<String> tags2) {
        final String tagsStr1 = String.join(", ", tags1);
        final String tagsStr2 = String.join(", ", tags2);
        return calculateSimilarity(tagsStr1, tagsStr2);
    }
}
