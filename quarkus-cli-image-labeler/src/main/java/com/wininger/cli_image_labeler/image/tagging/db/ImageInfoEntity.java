package com.wininger.cli_image_labeler.image.tagging.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "image_info")
public class ImageInfoEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BLOB", length = 16)
    private UUID id;

    @Column(nullable = false, length = 2048, name = "full_path")
    private String fullPath;

    @Column(nullable = false, length = 10000)
    private String description;

    @ManyToMany
    @JoinTable(
        name = "image_info_tag_join",
        joinColumns = @JoinColumn(name = "image_info_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<TagEntity> tags = new ArrayList<>();

    @Column(name = "thumb_nail_name")
    private String thumbnailName;

    @Column(name = "short_title", length = 100)
    private String shortTitle;

    @Column(name = "is_text")
    private Boolean isText;

    @Column(nullable = false, name = "created_at")
    @jakarta.persistence.Convert(converter = LocalDateTimeConverter.class)
    private LocalDateTime createdAt;

    @Column(nullable = false, name = "updated_at")
    @jakarta.persistence.Convert(converter = LocalDateTimeConverter.class)
    private LocalDateTime updatedAt;

    // Default constructor for JPA
    public ImageInfoEntity() {
    }

    public ImageInfoEntity(
      String fullPath,
      String description,
      List<TagEntity> tags,
      String thumbnailName,
      String shortTitle,
      Boolean isText
    ) {
        this.fullPath = fullPath;
        this.description = description;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        this.thumbnailName = thumbnailName;
        this.shortTitle = shortTitle;
        this.isText = isText != null ? isText : false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<TagEntity> getTags() {
        return tags;
    }

    public void setTags(List<TagEntity> tags) {
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getThumbnailName() {
        return thumbnailName;
    }

    public void setThumbnailName(String thumbnailName) {
        this.thumbnailName = thumbnailName;
    }

    public String getShortTitle() {
        return shortTitle;
    }

    public void setShortTitle(String shortTitle) {
        this.shortTitle = shortTitle;
    }

    public Boolean getIsText() {
        return isText;
    }

    public void setIsText(Boolean isText) {
        this.isText = isText != null ? isText : false;
    }
}

