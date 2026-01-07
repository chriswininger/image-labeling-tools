-- Create the join table for many-to-many relationship between image_info and tags
CREATE TABLE image_info_tag_join (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    image_info_id BLOB NOT NULL,
    tag_id INTEGER NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (image_info_id) REFERENCES image_info(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE,
    UNIQUE(image_info_id, tag_id)
);

CREATE INDEX idx_image_info_tag_join_image_info_id ON image_info_tag_join(image_info_id);
CREATE INDEX idx_image_info_tag_join_tag_id ON image_info_tag_join(tag_id);

-- Migrate existing comma-separated tags to the join table
-- This uses a recursive CTE to split comma-separated tags
INSERT INTO image_info_tag_join (image_info_id, tag_id, created_at)
WITH RECURSIVE split_tags(image_id, tag_name, remaining, created_at) AS (
    -- Initial case: extract first tag from comma-separated string
    SELECT 
        id as image_id,
        trim(substr(tags || ',', 1, instr(tags || ',', ',') - 1)) as tag_name,
        substr(tags || ',', instr(tags || ',', ',') + 1) as remaining,
        created_at
    FROM image_info
    WHERE tags IS NOT NULL AND tags != ''
    
    UNION ALL
    
    -- Recursive case: extract next tag
    SELECT 
        image_id,
        trim(substr(remaining, 1, instr(remaining, ',') - 1)) as tag_name,
        substr(remaining, instr(remaining, ',') + 1) as remaining,
        created_at
    FROM split_tags
    WHERE remaining != ''
),
tag_mappings AS (
    SELECT DISTINCT
        st.image_id as image_info_id,
        t.id as tag_id,
        st.created_at
    FROM split_tags st
    INNER JOIN tags t ON trim(st.tag_name) = t.tag_name
    WHERE st.tag_name IS NOT NULL AND st.tag_name != ''
)
SELECT 
    image_info_id,
    tag_id,
    created_at
FROM tag_mappings;

-- Drop the tags column from image_info table
-- Note: SQLite doesn't support DROP COLUMN directly in older versions
-- We'll need to recreate the table
CREATE TABLE image_info_new (
    id BLOB PRIMARY KEY,
    full_path TEXT NOT NULL,
    description TEXT NOT NULL,
    thumb_nail_name TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

-- Copy data from old table to new table
INSERT INTO image_info_new (id, full_path, description, thumb_nail_name, created_at, updated_at)
SELECT id, full_path, description, thumb_nail_name, created_at, updated_at
FROM image_info;

-- Drop old table and rename new one
DROP TABLE image_info;
ALTER TABLE image_info_new RENAME TO image_info;

-- Recreate indexes
CREATE INDEX idx_image_info_full_path ON image_info(full_path);
CREATE INDEX idx_image_info_created_at ON image_info(created_at);
