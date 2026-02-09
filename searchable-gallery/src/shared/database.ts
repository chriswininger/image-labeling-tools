import { Database } from "better-sqlite3";

let db: Database | null = null;

export const getDb = (): Database | null => {
  return db
};

export const setDb = (database: Database) => {
  db = database;
};

export const closeDb = () => {
  if (db) {
    db.close();
    db = null;
  }
};
