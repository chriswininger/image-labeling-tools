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

    // Get all image IDs to fetch their tags
    const imageIds = images.map((img) => img.id);

    if (imageIds.length === 0) {
      return [];
    }

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
    }));
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
        FROM image_info ii
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
          FROM image_info ii
                   INNER JOIN (
              SELECT iitj_filter.image_info_id
              FROM image_info_tag_join iitj_filter
                       INNER JOIN tags t_filter ON iitj_filter.tag_id = t_filter.id
              WHERE t_filter.tag_name IN (${filterOptions.tags.map(() => '?').join(', ')})
              GROUP BY iitj_filter.image_info_id
              HAVING COUNT(DISTINCT t_filter.tag_name) = ?
          ) filtered ON ii.id = filtered.image_info_id
          ${getOrderByClause()}
      `, params];
    case 'or':
      // For OR: images must have AT LEAST ONE of the specified tags
      return [`
          SELECT
              ${getIncludedImageInfoAttributes()}
          FROM image_info ii
                   INNER JOIN (
              SELECT DISTINCT iitj_filter.image_info_id
              FROM image_info_tag_join iitj_filter
                       INNER JOIN tags t_filter ON iitj_filter.tag_id = t_filter.id
              WHERE t_filter.tag_name IN (${filterOptions.tags.map(() => '?').join(', ')})
          ) filtered ON ii.id = filtered.image_info_id
          ${getOrderByClause()}
      `, params];
  }
}

function getOrderByClause(): string {
  return 'ORDER BY ii.created_at DESC';
}

function getIncludedImageInfoAttributes(): string {
  return `
            ii.id,
            ii.full_path,
            ii.description,
            ii.short_title,
            ii.thumb_nail_name,
            ii.text_contents,
            ii.created_at,
            ii.updated_at
  `
}
