-- Rename table from image_tags to image_info
ALTER TABLE image_tags RENAME TO image_info;

-- Rename indexes to match new table name
DROP INDEX IF EXISTS idx_image_tags_full_path;
DROP INDEX IF EXISTS idx_image_tags_created_at;

CREATE INDEX idx_image_info_full_path ON image_info(full_path);
CREATE INDEX idx_image_info_created_at ON image_info(created_at);
