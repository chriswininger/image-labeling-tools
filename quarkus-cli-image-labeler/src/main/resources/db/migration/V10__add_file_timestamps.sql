-- Add filesystem timestamp columns to image_info table
ALTER TABLE image_info ADD COLUMN file_created_at TIMESTAMP;
ALTER TABLE image_info ADD COLUMN file_last_modified TIMESTAMP;
