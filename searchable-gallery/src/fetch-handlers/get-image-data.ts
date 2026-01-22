import path from 'node:path';
import fs from 'fs';

export const getImageData = async (
  _event: Electron.IpcMainInvokeEvent,
  imagePath: string
) => {
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
};
