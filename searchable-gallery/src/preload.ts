// See the Electron documentation for details on how to use preload scripts:
// https://www.electronjs.org/docs/latest/tutorial/process-model#preload-scripts

import { contextBridge, ipcRenderer } from 'electron';

// Expose protected methods that allow the renderer process to use
// the ipcRenderer without exposing the entire object
contextBridge.exposeInMainWorld('electronAPI', {
  getAllImages: (filterOptions?: { tags?: string[] }) => ipcRenderer.invoke('get-all-images', filterOptions),
  getAllTags: () => ipcRenderer.invoke('get-all-tags'),
  getImageData: (imagePath: string) => ipcRenderer.invoke('get-image-data', imagePath),
  getThumbnailPath: (thumbnailName: string) => ipcRenderer.invoke('get-thumbnail-path', thumbnailName),
  getPreferences: () => ipcRenderer.invoke('get-preferences'),
  onPreferenceChanged: (callback: (preference: { key: string; value: unknown }) => void) => {
    const listener = (_event: Electron.IpcRendererEvent, preference: { key: string; value: unknown }) => {
      callback(preference);
    };
    ipcRenderer.on('preference-changed', listener);
    // Return a cleanup function
    return () => {
      ipcRenderer.removeListener('preference-changed', listener);
    };
  },
});
