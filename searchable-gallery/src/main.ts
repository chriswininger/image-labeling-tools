import { app, BrowserWindow, ipcMain, protocol } from 'electron';
import path from 'node:path';
import fs from 'fs';
import started from 'electron-squirrel-startup';

// Lazy load better-sqlite3 to avoid bundling issues with Vite
const loadDatabase = () => {
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  return require('better-sqlite3');
};

// Handle creating/removing shortcuts on Windows when installing/uninstalling.
if (started) {
  app.quit();
}

let db: any = null;

// Register custom protocol for serving local images
// This must be called before app.whenReady() - called at module load time
protocol.registerSchemesAsPrivileged([
  {
    scheme: 'app',
    privileges: {
      secure: true,
      standard: true,
      supportFetchAPI: true,
      corsEnabled: true,
    },
  },
]);

// Initialize database connection
const initDatabase = () => {
  // Try multiple possible paths for the database
  const appPath = app.getAppPath();
  const possiblePaths = [
    // Development path - relative to app path (most reliable)
    path.join(appPath, 'data', 'image-tags.db'),
    // Development path - relative to project root (from build directory)
    path.join(__dirname, '..', '..', 'data', 'image-tags.db'),
    // Development path - relative to project root (alternative)
    path.join(__dirname, '..', 'data', 'image-tags.db'),
    // Absolute path from project root (if __dirname is in .vite/build/main)
    path.resolve(__dirname, '..', '..', '..', 'data', 'image-tags.db'),
    // Production path - in app resources
    path.join(process.resourcesPath || __dirname, 'data', 'image-tags.db'),
    // User data path
    path.join(app.getPath('userData'), 'data', 'image-tags.db'),
  ];

  console.log('Attempting to connect to database...');
  console.log('app.getAppPath():', appPath);
  console.log('__dirname:', __dirname);
  console.log('process.resourcesPath:', process.resourcesPath);
  console.log('app.getPath(userData):', app.getPath('userData'));

  for (const dbPath of possiblePaths) {
    try {
      const normalizedPath = path.normalize(dbPath);
      console.log(`Trying database path: ${normalizedPath}`);

      if (fs.existsSync(normalizedPath)) {
        console.log(`Database file exists at: ${normalizedPath}`);
        const DB = loadDatabase();
        db = new DB(normalizedPath);
        console.log('✅ Database connected successfully at:', normalizedPath);
        return;
      } else {
        console.log(`❌ Database file does not exist at: ${normalizedPath}`);
      }
    } catch (error) {
      console.log(`❌ Failed to connect to database at ${dbPath}:`, error);
    }
  }

  console.error('❌ Failed to connect to database at any of the attempted paths');
  console.error('Tried paths:', possiblePaths);
};

// Register custom protocol handler for images
const registerImageProtocol = () => {
  protocol.registerFileProtocol('app', (request, callback) => {
    try {
      const url = request.url.replace('app://', '');
      // Remove query parameters if any
      const filePath = url.split('?')[0];

      // Decode the path and verify it exists
      const decodedPath = decodeURIComponent(filePath);
      console.log('Protocol request:', request.url);
      console.log('Decoded path:', decodedPath);

      if (fs.existsSync(decodedPath)) {
        console.log('✅ Serving image from:', decodedPath);
        callback({ path: decodedPath });
      } else {
        console.error('❌ Image file not found:', decodedPath);
        callback({ error: -6 }); // FILE_NOT_FOUND
      }
    } catch (error) {
      console.error('❌ Error serving image:', error);
      console.error('Request URL:', request.url);
      callback({ error: -2 }); // FAILED
    }
  });
  console.log('✅ Custom protocol "app" registered');
};

// IPC handlers for database operations
ipcMain.handle('get-all-images', async (event, filterOptions?: { tags?: string[]; joinType?: 'and' | 'or' }) => {
  if (!db) {
    throw new Error('Database not initialized');
  }

  try {
    let query = 'SELECT id, full_path, description, tags, thumb_nail_name, created_at, updated_at FROM image_tags';
    const params: any[] = [];

    // Apply tag filtering if tags are provided
    if (filterOptions?.tags && filterOptions.tags.length > 0) {
      const joinType = filterOptions.joinType || 'or';
      // Use SQL to match tags in comma-separated string
      // Match whole tags by checking for comma boundaries (handles spaces around commas)
      const tagConditions = filterOptions.tags.map((tag) => {
        const trimmedTag = tag.trim();
        // Match patterns: tag at start, middle, or end of comma-separated list, or exact match
        params.push(
          `${trimmedTag},%`,      // Start: "tag, ..."
          `%, ${trimmedTag},%`,  // Middle with space: "..., tag, ..."
          `%,${trimmedTag},%`,   // Middle without space: "...,tag,..."
          `%, ${trimmedTag}`,    // End with space: "..., tag"
          `%,${trimmedTag}`,     // End without space: "...,tag"
          trimmedTag              // Exact: "tag"
        );
        return `(tags LIKE ? OR tags LIKE ? OR tags LIKE ? OR tags LIKE ? OR tags LIKE ? OR tags = ?)`;
      });
      // Join conditions with AND or OR based on joinType
      const joinOperator = joinType === 'and' ? ' AND ' : ' OR ';
      query += ` WHERE ${tagConditions.join(joinOperator)}`;
    }

    query += ' ORDER BY created_at DESC';

    console.log('run query: ', query);
    const stmt = db.prepare(query);
    const images = stmt.all(...params);
    return images;
  } catch (error) {
    console.error('Error fetching images:', error);
    throw error;
  }
});

// IPC handler to get all tags
ipcMain.handle('get-all-tags', async () => {
  if (!db) {
    throw new Error('Database not initialized');
  }

  try {
    const stmt = db.prepare('SELECT id, tag_name, created_at, updated_at FROM tags ORDER BY tag_name ASC');

    return stmt.all();
  } catch (error) {
    console.error('Error fetching tags:', error);
    throw error;
  }
});

// IPC handler to get thumbnail path
ipcMain.handle('get-thumbnail-path', async (event, thumbnailName: string) => {
  // Try multiple possible paths for the thumbnail directory (similar to database path resolution)
  const appPath = app.getAppPath();
  const possiblePaths = [
    path.join(appPath, 'data', 'thumbnails', thumbnailName),
    path.join(__dirname, '..', '..', 'data', 'thumbnails', thumbnailName),
    path.join(__dirname, '..', 'data', 'thumbnails', thumbnailName),
    path.resolve(__dirname, '..', '..', '..', 'data', 'thumbnails', thumbnailName),
    path.join(process.resourcesPath || __dirname, 'data', 'thumbnails', thumbnailName),
    path.join(app.getPath('userData'), 'data', 'thumbnails', thumbnailName),
  ];

  for (const thumbPath of possiblePaths) {
    const normalizedPath = path.normalize(thumbPath);
    if (fs.existsSync(normalizedPath)) {
      return normalizedPath;
    }
  }

  throw new Error(`Thumbnail not found: ${thumbnailName}`);
});

// IPC handler to get image as data URL
ipcMain.handle('get-image-data', async (event, imagePath: string) => {
  try {
    if (!fs.existsSync(imagePath)) {
      throw new Error(`Image file not found: ${imagePath}`);
    }

    // Read the image file and convert to base64
    const imageBuffer = fs.readFileSync(imagePath);
    const base64 = imageBuffer.toString('base64');

    // Determine MIME type from file extension
    const ext = path.extname(imagePath).toLowerCase();
    let mimeType = 'image/jpeg'; // default
    if (ext === '.png') mimeType = 'image/png';
    else if (ext === '.gif') mimeType = 'image/gif';
    else if (ext === '.webp') mimeType = 'image/webp';
    else if (ext === '.bmp') mimeType = 'image/bmp';

    return `data:${mimeType};base64,${base64}`;
  } catch (error) {
    console.error('Error reading image:', error);
    throw error;
  }
});

const createWindow = () => {
  // Create the browser window.
  const mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });

  // and load the index.html of the app.
  if (MAIN_WINDOW_VITE_DEV_SERVER_URL) {
    mainWindow.loadURL(MAIN_WINDOW_VITE_DEV_SERVER_URL);
  } else {
    mainWindow.loadFile(
      path.join(__dirname, `../renderer/${MAIN_WINDOW_VITE_NAME}/index.html`),
    );
  }

  // Open the DevTools.
  mainWindow.webContents.openDevTools();
};

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.whenReady().then(async () => {
  registerImageProtocol();
  await initDatabase();
  if (!db) {
    console.error('⚠️  Warning: Database not initialized. Some features may not work.');
  }
  createWindow();
});

// Quit when all windows are closed, except on macOS. There, it's common
// for applications and their menu bar to stay active until the user quits
// explicitly with Cmd + Q.
app.on('window-all-closed', () => {
  if (db) {
    db.close();
  }
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  // On OS X it's common to re-create a window in the app when the
  // dock icon is clicked and there are no other windows open.
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});

// In this file you can include the rest of your app's specific main process
// code. You can also put them in separate files and import them here.
