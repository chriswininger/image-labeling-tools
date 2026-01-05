-- Convert Unix timestamp (milliseconds) to ISO format (YYYY-MM-DD HH:MM:SS)
-- This handles existing data that was stored as Unix timestamps

UPDATE image_tags 
SET created_at = datetime(CAST(created_at AS INTEGER) / 1000.0, 'unixepoch')
WHERE created_at NOT LIKE '%-%' AND created_at NOT LIKE '%:%';

UPDATE image_tags 
SET updated_at = datetime(CAST(updated_at AS INTEGER) / 1000.0, 'unixepoch')
WHERE updated_at NOT LIKE '%-%' AND updated_at NOT LIKE '%:%';

