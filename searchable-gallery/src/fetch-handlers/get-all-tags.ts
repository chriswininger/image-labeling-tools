import { getDb } from '../shared/database';

export const getAllTags = async () => {
  const db = getDb();
  if (!db) {
    throw new Error('Database not initialized');
  }

  try {
    const stmt = db.prepare(
      'SELECT id, tag_name, created_at, updated_at FROM tags ORDER BY tag_name ASC'
    );

    return stmt.all();
  } catch (error) {
    console.error('Error fetching tags:', error);
    throw error;
  }
};
