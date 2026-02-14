package com.wininger.cli_image_labeler.image.tagging.db;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
public class TagRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Transactional
    public TagEntity upsertTag(final String tagName) {
        final String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        entityManager.createNativeQuery(
            "INSERT INTO tags (tag_name, created_at, updated_at) " +
            "VALUES (:tagName, :now, :now) " +
            "ON CONFLICT(tag_name) DO UPDATE SET updated_at = :now")
            .setParameter("tagName", tagName)
            .setParameter("now", now)
            .executeUpdate();

        return findByTagName(tagName);
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
