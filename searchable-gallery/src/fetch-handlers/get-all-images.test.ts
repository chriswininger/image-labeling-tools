import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import { faker } from '@faker-js/faker';
import { setupTestDatabase } from '../test-utils/db-test-helper';
import { getDb } from '../shared/database';
import { getAllImages } from './get-all-images';

describe('getAllImages', () => {
  let cleanup: () => void;
  let imageWithNature: GeneratedImage;
  let imageWithLandscape: GeneratedImage;
  let imageWithBothTags: GeneratedImage;
  let imageWithNoTags: GeneratedImage;

  // Mock Electron event - getAllImages doesn't use it, but it's required by the signature
  let mockEvent: Electron.IpcMainInvokeEvent;


  beforeAll(() => {
    const setup = setupTestDatabase();
    cleanup = setup.cleanup;

    // Create test images with specific tag combinations
    imageWithNature = generateRandomImage(['nature'], '2026-02-03T02:10:35.657Z');
    imageWithLandscape = generateRandomImage(['landscape'], '2026-02-03T02:11:35.657Z');
    imageWithBothTags = generateRandomImage(['nature', 'landscape'], '2026-02-03T02:12:35.657Z');
    imageWithNoTags = generateRandomImage([], '2026-02-03T02:13:35.657Z');

    mockEvent = {} as Electron.IpcMainInvokeEvent;
  });

  afterAll(() => {
    cleanup();
  });

  it('should return all images when no filter is provided', async () => {
    const images = await getAllImages(mockEvent);

    // sorted by createdAt
    expect(images).toEqual([
      imageWithNoTags,
      imageWithBothTags,
      imageWithLandscape,
      imageWithNature,
    ]);
  });

  it('should return empty array when filtering by non-existent tag', async () => {
    const images = await getAllImages(mockEvent, {
      tags: ['non-existent-tag-xyz-123'],
      joinType: 'and',
    });

    expect(images).toEqual([]);
  });

  describe('tag filtering with OR join type', () => {
    it('should return images matching the tag when only one tag provided', async () => {
      const images = await getAllImages(mockEvent, {
        tags: ['nature'],
        joinType: 'or',
      });

      // Should return imageWithBothTags, imageWithNature sorted by createdAt
      expect(images).toHaveLength(2);
      expect(images).toEqual([imageWithBothTags, imageWithNature ]);
    });

    it('should return images matching either tag when multiple tags provided', async () => {
      const images = await getAllImages(mockEvent, {
        tags: ['nature', 'landscape'],
        joinType: 'or',
      });

      // Should return imageWithBothTags, imageWithLandscape, and imageWithNature sorted by createdAt
      expect(images).toHaveLength(3);
      expect(images).toEqual([ imageWithBothTags, imageWithLandscape, imageWithNature]);
    });
  });

  describe('tag filtering with AND join type', () => {
    it('should return images matching the tag when only one tag provided', async () => {
      const images = await getAllImages(mockEvent, {
        tags: ['landscape'],
        joinType: 'and',
      });

      // Should return imageWithLandscape and imageWithBothTags
      expect(images).toHaveLength(2);
      expect(images).toEqual([
        imageWithBothTags,
        imageWithLandscape
      ]);
    });

    it('should return only images that have all specified tags', async () => {
      const images = await getAllImages(mockEvent, {
        tags: ['nature', 'landscape'],
        joinType: 'and',
      });

      // Should only return imageWithBothTags
      expect(images).toHaveLength(1);

      // tags are sorted alphabetically on return
      expect(images[0]).toEqual(imageWithBothTags);
    });
  });

  interface GeneratedImage {
    id: Buffer;
    full_path: string;
    description: string;
    short_title: string;
    thumb_nail_name: string;
    text_contents: string | null;
    created_at: string;
    updated_at: string;
    tags: string[];
  }

  /**
   * Generates and persists a random image record with the specified tags.
   */
  function generateRandomImage(tags: string[], createdAt: string = new Date().toISOString()): GeneratedImage {
    const db = getDb();
    if (!db) {
      throw new Error('Database not initialized');
    }

    const id = Buffer.from(faker.string.uuid().replace(/-/g, ''), 'hex');

    const imageData = {
      id,
      full_path: faker.system.filePath(),
      description: faker.lorem.paragraph(),
      short_title: faker.lorem.sentence({ min: 2, max: 5 }),
      thumb_nail_name: `${faker.string.uuid()}.jpg`,
      text_contents: faker.datatype.boolean() ? faker.lorem.paragraphs(2) : null,
      is_text: faker.datatype.boolean() ? 1 : 0,
      gps_latitude: faker.location.latitude(),
      gps_longitude: faker.location.longitude(),
      image_taken_at: faker.date.past().toISOString(),
      file_created_at: faker.date.past().toISOString(),
      file_last_modified: faker.date.recent().toISOString(),
      created_at: createdAt,
      updated_at: new Date().toISOString(),
    };

    const insertImageStmt = db.prepare(`
    INSERT INTO image_info (
      id, full_path, description, short_title, thumb_nail_name, text_contents,
      is_text, gps_latitude, gps_longitude, image_taken_at, file_created_at,
      file_last_modified, created_at, updated_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);

    insertImageStmt.run(
        imageData.id,
        imageData.full_path,
        imageData.description,
        imageData.short_title,
        imageData.thumb_nail_name,
        imageData.text_contents,
        imageData.is_text,
        imageData.gps_latitude,
        imageData.gps_longitude,
        imageData.image_taken_at,
        imageData.file_created_at,
        imageData.file_last_modified,
        imageData.created_at,
        imageData.updated_at
    );

    const insertTagStmt = db.prepare(`
    INSERT OR IGNORE INTO tags (tag_name, created_at, updated_at)
    VALUES (?, ?, ?)
  `);

    const getTagIdStmt = db.prepare(`
    SELECT id FROM tags WHERE tag_name = ?
  `);

    const insertJoinStmt = db.prepare(`
    INSERT INTO image_info_tag_join (image_info_id, tag_id, created_at)
    VALUES (?, ?, ?)
  `);

    for (const tagName of tags) {
      const now = new Date().toISOString();
      insertTagStmt.run(tagName, now, now);
      const tagRow = getTagIdStmt.get(tagName) as { id: number };
      insertJoinStmt.run(imageData.id, tagRow.id, now);
    }

    return {
      id: imageData.id,
      full_path: imageData.full_path,
      description: imageData.description,
      short_title: imageData.short_title,
      thumb_nail_name: imageData.thumb_nail_name,
      text_contents: imageData.text_contents,
      created_at: imageData.created_at,
      updated_at: imageData.updated_at,
      tags: tags.sort(), // sort tags to make comparing easier as they will be sorted when fetched
    };
  }
});
