import { app, BrowserWindow, ipcMain, protocol, Menu, MenuItemConstructorOptions } from 'electron';
import path from 'node:path';
import fs from 'fs';
import started from 'electron-squirrel-startup';
import { getDb, setDb, closeDb } from './shared/database';
import { getAllImages, getAllTags, getThumbnailPath, getImageData } from './fetch-handlers';
import Database from 'better-sqlite3';

// Handle creating/removing shortcuts on Windows when installing/uninstalling.
if (started) {
  app.quit();
}

let mainWindow: BrowserWindow | null = null;

// Preferences state
let showTagsInGallery = true;

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
        setDb(new Database(normalizedPath));
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
ipcMain.handle('get-all-images', getAllImages);
ipcMain.handle('get-all-tags', getAllTags);
ipcMain.handle('get-thumbnail-path', getThumbnailPath);
ipcMain.handle('get-image-data', getImageData);

// Create the application menu
const createMenu = () => {
  const template: MenuItemConstructorOptions[] = [
    {
      label: 'File',
      submenu: [
        {
          label: 'Preferences',
          submenu: [
            {
              label: 'Show Tags In Gallery',
              type: 'checkbox',
              checked: showTagsInGallery,
              click: (menuItem) => {
                showTagsInGallery = menuItem.checked;
                // Send preference change to renderer
                if (mainWindow && !mainWindow.isDestroyed()) {
                  mainWindow.webContents.send('preference-changed', {
                    key: 'showTagsInGallery',
                    value: showTagsInGallery,
                  });
                }
              },
            },
          ],
        },
        { type: 'separator' },
        { role: 'quit' },
      ],
    },
    {
      label: 'Edit',
      submenu: [
        { role: 'undo' },
        { role: 'redo' },
        { type: 'separator' },
        { role: 'cut' },
        { role: 'copy' },
        { role: 'paste' },
        { role: 'selectAll' },
      ],
    },
    {
      label: 'View',
      submenu: [
        { role: 'reload' },
        { role: 'forceReload' },
        { role: 'toggleDevTools' },
        { type: 'separator' },
        { role: 'resetZoom' },
        { role: 'zoomIn' },
        { role: 'zoomOut' },
        { type: 'separator' },
        { role: 'togglefullscreen' },
      ],
    },
    {
      label: 'Window',
      submenu: [
        { role: 'minimize' },
        { role: 'close' },
      ],
    },
  ];

  const menu = Menu.buildFromTemplate(template);
  Menu.setApplicationMenu(menu);
};

// IPC handler to get current preferences
ipcMain.handle('get-preferences', async () => {
  return {
    showTagsInGallery,
  };
});

const createWindow = () => {
  // Create the browser window.
  mainWindow = new BrowserWindow({
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

  // Clean up reference when window is closed
  mainWindow.on('closed', () => {
    mainWindow = null;
  });
};

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.whenReady().then(async () => {
  registerImageProtocol();
  await initDatabase();
  if (!getDb()) {
    console.error('⚠️  Warning: Database not initialized. Some features may not work.');
  }
  createMenu();
  createWindow();
});

// Quit when all windows are closed, except on macOS. There, it's common
// for applications and their menu bar to stay active until the user quits
// explicitly with Cmd + Q.
app.on('window-all-closed', () => {
  closeDb();
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
