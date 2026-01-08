-- Add short_title and is_text columns to image_info table
ALTER TABLE image_info ADD COLUMN short_title TEXT;
ALTER TABLE image_info ADD COLUMN is_text INTEGER DEFAULT 0;

-- Create index for is_text to support filtering
CREATE INDEX idx_image_info_is_text ON image_info(is_text);
