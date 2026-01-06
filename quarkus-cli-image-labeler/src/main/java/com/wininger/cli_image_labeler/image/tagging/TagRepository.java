package com.wininger.cli_image_labeler.image.tagging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class TagRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Transactional
    public TagEntity upsertTag(final String tagName) {
        // Try to find existing tag
        TagEntity existing = findByTagName(tagName);
        
        if (existing != null) {
            // Update the updated_at timestamp
            existing.setUpdatedAt(LocalDateTime.now());
            return entityManager.merge(existing);
        } else {
            // Create new tag
            TagEntity newTag = new TagEntity(tagName);
            entityManager.persist(newTag);
            return newTag;
        }
    }

    @Transactional
    public TagEntity findByTagName(final String tagName) {
        return entityManager.createQuery(
            "SELECT t FROM TagEntity t WHERE t.tagName = :tagName", 
            TagEntity.class
        )
        .setParameter("tagName", tagName)
        .getResultStream()
        .findFirst()
        .orElse(null);
    }

    @Transactional
    public List<TagEntity> findAll() {
        return entityManager.createQuery(
            "SELECT t FROM TagEntity t ORDER BY t.tagName", 
            TagEntity.class
        )
        .getResultList();
    }
}
