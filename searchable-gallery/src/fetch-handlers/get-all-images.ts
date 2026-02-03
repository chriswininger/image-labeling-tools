import {getDb} from '../shared/database';
import {isNullOrUndefined} from "../utils";

interface FilterOptions {
  tags?: string[];
  joinType?: 'and' | 'or';
}

interface ImageRow {
  id: Buffer | string;
  full_path: string;
  description: string;
  short_title: string | null;
  tags: string [];
  thumb_nail_name: string | null;
  text_contents: string | null;
  created_at: string;
  updated_at: string;
}

interface ImageTagRow {
  image_info_id: Buffer | string;
  tag_name: string;
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
    const [query, params] = getBaseQuery(filterOptions);

    console.log('run query: ', query);
    console.log('params: ', params);
    const stmt = db.prepare(query);
    const images: ImageRow[] = stmt.all(...params);

    return images;

    /*
    // Get all image IDs to fetch their tags
    const imageIds = images.map((img) => img.id);

    if (imageIds.length === 0) {
      return [];
    }*/

    /*
    // Fetch all tags for these images in a single query
    const tagsQuery = `
      SELECT iitj.image_info_id, t.tag_name
      FROM image_info_tag_join iitj
      INNER JOIN tags t ON iitj.tag_id = t.id
      WHERE iitj.image_info_id IN (${imageIds.map(() => '?').join(', ')})
      ORDER BY t.tag_name ASC
    `;
    const tagsStmt = db.prepare(tagsQuery);
    const imageTags: ImageTagRow[] = tagsStmt.all(...imageIds);

    // Build a map of image_id -> tags array
    // Note: id is stored as BLOB in SQLite, so we convert to string for Map key comparison
    const tagsByImageId = new Map<string, string[]>();
    for (const row of imageTags) {
      const idKey = idToString(row.image_info_id);
      const existing = tagsByImageId.get(idKey) || [];
      existing.push(row.tag_name);
      tagsByImageId.set(idKey, existing);
    }

    // Combine images with their tags
    return images.map((image) => ({
      ...image,
      tags: tagsByImageId.get(idToString(image.id)) || [],
    }));*/
  } catch (error) {
    console.error('Error fetching images:', error);
    throw error;
  }
};

// Helper to convert BLOB id to string for Map key usage
function idToString(id: Buffer | string): string {
  if (Buffer.isBuffer(id)) {
    return id.toString('hex');
  }
  return String(id);
}

type Parameters = (string | number)[];
type QueryWithParams = [string, Parameters];
function getBaseQuery(filterOptions?: FilterOptions): QueryWithParams {
  // No filtering: get all images
  if (isNullOrUndefined(filterOptions?.tags) || filterOptions.tags.length == 0) {
    return [`
        SELECT
            ${getIncludedImageInfoAttributes()}
        FROM image_info
        LEFT JOIN image_info_tag_join iitj ON image_info.id = iitj.image_info_id
        LEFT JOIN tags ON iitj.tag_id = tags.id
        GROUP BY image_info.id
        ${getOrderByClause()}
    `, []];
  }

  // Add tag names as parameters
  const params: Parameters = filterOptions.tags.map(tag => tag.trim());

  switch (filterOptions.joinType) {
    // For AND: images must have ALL the specified tags
    case 'and':
      // for 'AND' we have to assert that every tag had a match so we include the count as a param
      // HAVING COUNT(DISTINCT t_filter.tag_name) = ?
      params.push(filterOptions.tags.length);

      return [`
          SELECT
              ${getIncludedImageInfoAttributes()}
          FROM image_info INNER JOIN (
            SELECT iitj_filter.image_info_id
            FROM image_info_tag_join iitj_filter
                   INNER JOIN tags  ON iitj_filter.tag_id = tags.id
            WHERE tags.tag_name IN (${filterOptions.tags.map(() => '?').join(', ')})
            GROUP BY iitj_filter.image_info_id
            HAVING COUNT(DISTINCT tags.tag_name) =  ?
          ) filtered_tags ON image_info.id = filtered_tags.image_info_id
                          LEFT JOIN image_info_tag_join  ON image_info.id = image_info_tag_join.image_info_id
                          LEFT JOIN tags ON image_info_tag_join.tag_id = tags.id
          GROUP BY image_info.id
          ${getOrderByClause()}
      `, params];
    case 'or':
      // For OR: images must have AT LEAST ONE of the specified tags
      return [`
          SELECT
              ${getIncludedImageInfoAttributes()}
          FROM image_info INNER JOIN (
            SELECT iitj_filter.image_info_id
            FROM image_info_tag_join iitj_filter
                   INNER JOIN tags  ON iitj_filter.tag_id = tags.id
            WHERE tags.tag_name IN (${filterOptions.tags.map(() => '?').join(', ')})
            GROUP BY iitj_filter.image_info_id
          ) filtered_tags ON image_info.id = filtered_tags.image_info_id
                          LEFT JOIN image_info_tag_join  ON image_info.id = image_info_tag_join.image_info_id
                          LEFT JOIN tags ON image_info_tag_join.tag_id = tags.id
          GROUP BY image_info.id
          ${getOrderByClause()}
      `, params];
  }
}

function getOrderByClause(): string {
  return 'ORDER BY image_info.created_at DESC';
}

function getIncludedImageInfoAttributes(): string {
  return `
    image_info.id,
    image_info.full_path,
    image_info.description,
    image_info.short_title,
    COALESCE(JSON_GROUP_ARRAY(tags.tag_name ORDER BY tags.tag_name), '[]') as tags,
    image_info.thumb_nail_name,
    image_info.text_contents,
    image_info.created_at,
    image_info.updated_at
  `
}

