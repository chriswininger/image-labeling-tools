-- Add GPS coordinates and image taken timestamp columns to image_info table
ALTER TABLE image_info ADD COLUMN gps_latitude DOUBLE;
ALTER TABLE image_info ADD COLUMN gps_longitude DOUBLE;
ALTER TABLE image_info ADD COLUMN image_taken_at TIMESTAMP;
