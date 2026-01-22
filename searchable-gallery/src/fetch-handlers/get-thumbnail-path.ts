import { app } from 'electron';
import path from 'node:path';
import fs from 'fs';

export const getThumbnailPath = async (
  _event: Electron.IpcMainInvokeEvent,
  thumbnailName: string
) => {
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
};
