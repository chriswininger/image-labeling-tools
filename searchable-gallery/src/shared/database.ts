// Shared database module for main process
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let db: any = null;

// Lazy load better-sqlite3 to avoid bundling issues with Vite
export const loadDatabase = () => {
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  return require('better-sqlite3');
};

export const getDb = () => db;

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const setDb = (database: any) => {
  db = database;
};

export const closeDb = () => {
  if (db) {
    db.close();
    db = null;
  }
};
