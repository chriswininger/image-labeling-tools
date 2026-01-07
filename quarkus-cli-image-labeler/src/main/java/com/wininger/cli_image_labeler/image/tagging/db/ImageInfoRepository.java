package com.wininger.cli_image_labeler.image.tagging.db;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ImageInfoRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Transactional
    public ImageInfoEntity save(final String fullPath, final String description, final List<TagEntity> tags) {
        return save(fullPath, description, tags, null);
    }

    @Transactional
    public ImageInfoEntity save(final String fullPath, final String description, final List<TagEntity> tags, final String thumbnailName) {
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
            "SELECT DISTINCT e FROM ImageInfoEntity e LEFT JOIN FETCH e.tags WHERE e.fullPath = :fullPath",
            ImageInfoEntity.class
        )
        .setParameter("fullPath", fullPath)
        .getResultStream()
        .findFirst()
        .orElse(null);
    }
}

