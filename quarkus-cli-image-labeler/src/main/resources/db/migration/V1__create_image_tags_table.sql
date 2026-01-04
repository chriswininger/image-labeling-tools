CREATE TABLE image_tags (
    id BLOB PRIMARY KEY,
    full_path TEXT NOT NULL,
    description TEXT NOT NULL,
    tags TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE INDEX idx_image_tags_full_path ON image_tags(full_path);
CREATE INDEX idx_image_tags_created_at ON image_tags(created_at);

