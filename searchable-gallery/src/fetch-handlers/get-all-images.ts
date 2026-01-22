import { getDb } from '../shared/database';

interface FilterOptions {
  tags?: string[];
  joinType?: 'and' | 'or';
}

export const getAllImages = async (
  _event: Electron.IpcMainInvokeEvent,
  filterOptions?: FilterOptions
) => {
  const db = getDb();
  if (!db) {
    throw new Error('Database not initialized');
  }

  try {
    const params: (string | number)[] = [];
    let query: string;

    // Apply tag filtering if tags are provided
    if (filterOptions?.tags && filterOptions.tags.length > 0) {
      const joinType = filterOptions.joinType || 'or';

      // Add tag names as parameters
      filterOptions.tags.forEach((tag) => {
        params.push(tag.trim());
      });

      if (joinType === 'and') {
        // For AND: images must have ALL the specified tags
        // Use a subquery to filter images that have all tags, then join to get tag aggregation
        query = `
          SELECT 
            ii.id, 
            ii.full_path, 
            ii.description,
            ii.short_title,
            COALESCE(GROUP_CONCAT(t.tag_name, ', '), '') as tags,
            ii.thumb_nail_name,
            ii.text_contents,
            ii.created_at, 
            ii.updated_at 
          FROM image_info ii
          INNER JOIN (
            SELECT iitj_filter.image_info_id
            FROM image_info_tag_join iitj_filter
            INNER JOIN tags t_filter ON iitj_filter.tag_id = t_filter.id
            WHERE t_filter.tag_name IN (${filterOptions.tags.map(() => '?').join(', ')})
            GROUP BY iitj_filter.image_info_id
            HAVING COUNT(DISTINCT t_filter.tag_name) = ?
          ) filtered ON ii.id = filtered.image_info_id
          LEFT JOIN image_info_tag_join iitj ON ii.id = iitj.image_info_id
          LEFT JOIN tags t ON iitj.tag_id = t.id
          GROUP BY ii.id, ii.full_path, ii.description, ii.short_title, ii.thumb_nail_name, ii.text_contents, ii.created_at, ii.updated_at
        `;
        params.push(filterOptions.tags.length);
      } else {
        // For OR: images must have AT LEAST ONE of the specified tags
        // Use a subquery to filter images, then join to get all tags for those images
        query = `
          SELECT 
            ii.id, 
            ii.full_path, 
            ii.description,
            ii.short_title,
            COALESCE(GROUP_CONCAT(t.tag_name, ', '), '') as tags,
            ii.thumb_nail_name,
            ii.text_contents,
            ii.created_at, 
            ii.updated_at 
          FROM image_info ii
          INNER JOIN (
            SELECT DISTINCT iitj_filter.image_info_id
            FROM image_info_tag_join iitj_filter
            INNER JOIN tags t_filter ON iitj_filter.tag_id = t_filter.id
            WHERE t_filter.tag_name IN (${filterOptions.tags.map(() => '?').join(', ')})
          ) filtered ON ii.id = filtered.image_info_id
          LEFT JOIN image_info_tag_join iitj ON ii.id = iitj.image_info_id
          LEFT JOIN tags t ON iitj.tag_id = t.id
          GROUP BY ii.id, ii.full_path, ii.description, ii.short_title, ii.thumb_nail_name, ii.text_contents, ii.created_at, ii.updated_at
        `;
      }
    } else {
      // No filtering: get all images with their tags aggregated
      query = `
        SELECT 
          ii.id, 
          ii.full_path, 
          ii.description,
          ii.short_title,
          COALESCE(GROUP_CONCAT(t.tag_name, ', '), '') as tags,
          ii.thumb_nail_name,
          ii.text_contents,
          ii.created_at, 
          ii.updated_at 
        FROM image_info ii
        LEFT JOIN image_info_tag_join iitj ON ii.id = iitj.image_info_id
        LEFT JOIN tags t ON iitj.tag_id = t.id
        GROUP BY ii.id, ii.full_path, ii.description, ii.short_title, ii.thumb_nail_name, ii.text_contents, ii.created_at, ii.updated_at
      `;
    }

    query += ' ORDER BY ii.created_at DESC';

    console.log('run query: ', query);
    console.log('params: ', params);
    const stmt = db.prepare(query);
    const images = stmt.all(...params);
    return images;
  } catch (error) {
    console.error('Error fetching images:', error);
    throw error;
  }
};
