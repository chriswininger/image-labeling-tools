-- Add text_contents column to image_info table for storing OCR results
ALTER TABLE image_info ADD COLUMN text_contents TEXT;
