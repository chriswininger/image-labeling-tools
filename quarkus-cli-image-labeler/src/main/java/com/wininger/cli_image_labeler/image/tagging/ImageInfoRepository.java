package com.wininger.cli_image_labeler.image.tagging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

@ApplicationScoped
public class ImageInfoRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Transactional
    public ImageInfoEntity save(final String fullPath, final String description, final String tags) {
        return save(fullPath, description, tags, null);
    }

    @Transactional
    public ImageInfoEntity save(final String fullPath, final String description, final String tags, final String thumbnailName) {
        final ImageInfoEntity entity = new ImageInfoEntity(fullPath, description, tags, thumbnailName);
        // ID will be generated in @PrePersist callback
        entityManager.persist(entity);
        return entity;
    }

    @Transactional
    public ImageInfoEntity update(final ImageInfoEntity entity) {
        entity.setUpdatedAt(LocalDateTime.now());
        return entityManager.merge(entity);
    }

    @Transactional
    public ImageInfoEntity findByFullPath(final String fullPath) {
        return entityManager.createQuery(
            "SELECT e FROM ImageInfoEntity e WHERE e.fullPath = :fullPath",
            ImageInfoEntity.class
        )
        .setParameter("fullPath", fullPath)
        .getResultStream()
        .findFirst()
        .orElse(null);
    }
}

