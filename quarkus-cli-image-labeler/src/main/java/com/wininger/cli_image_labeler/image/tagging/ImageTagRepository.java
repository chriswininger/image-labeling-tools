package com.wininger.cli_image_labeler.image.tagging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class ImageTagRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Transactional
    public ImageTagEntity save(final String fullPath, final String description, final String tags) {
        final ImageTagEntity entity = new ImageTagEntity(fullPath, description, tags);
        // ID will be generated in @PrePersist callback
        entityManager.persist(entity);
        return entity;
    }

    @Transactional
    public ImageTagEntity update(final ImageTagEntity entity) {
        entity.setUpdatedAt(LocalDateTime.now());
        return entityManager.merge(entity);
    }

    @Transactional
    public ImageTagEntity findByFullPath(final String fullPath) {
        return entityManager.createQuery(
            "SELECT e FROM ImageTagEntity e WHERE e.fullPath = :fullPath", 
            ImageTagEntity.class
        )
        .setParameter("fullPath", fullPath)
        .getResultStream()
        .findFirst()
        .orElse(null);
    }
}

