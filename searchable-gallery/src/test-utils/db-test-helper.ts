import * as fs from 'fs';
import * as path from 'path';
import { v4 as uuidv4 } from 'uuid';
import { loadDatabase, setDb, closeDb, getDb } from '../shared/database';

const TEST_DATA_DIR = path.join(__dirname, '../../test-data');
const TEST_RUN_DIR = path.join(TEST_DATA_DIR, 'test-run');
const SOURCE_DB = path.join(TEST_DATA_DIR, 'image-tags.db');

/**
 * Creates a fresh copy of the test database for a test suite.
 * Returns a cleanup function that should be called after tests complete.
 */
export function setupTestDatabase(): { dbPath: string; cleanup: () => void } {
  // Ensure test-run directory exists
  if (!fs.existsSync(TEST_RUN_DIR)) {
    fs.mkdirSync(TEST_RUN_DIR, { recursive: true });
  }

  // Create a unique database file for this test run
  const testDbName = `${uuidv4()}.db`;
  const testDbPath = path.join(TEST_RUN_DIR, testDbName);

  // Copy the source database
  fs.copyFileSync(SOURCE_DB, testDbPath);

  // Initialize the database connection
  const Database = loadDatabase();
  const db = new Database(testDbPath);
  setDb(db);

  return {
    dbPath: testDbPath,
    cleanup: () => {
      closeDb();
      // Remove the test database file
      if (fs.existsSync(testDbPath)) {
        fs.unlinkSync(testDbPath);
      }
    },
  };
}

/**
 * Gets the current test database instance.
 * Throws if database is not initialized.
 */
export function getTestDb() {
  const db = getDb();
  if (!db) {
    throw new Error('Test database not initialized. Call setupTestDatabase() first.');
  }
  return db;
}
