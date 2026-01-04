export interface ElectronAPI {
  getAllImages: () => Promise<ImageData[]>;
  getImageData: (imagePath: string) => Promise<string>;
}

export interface ImageData {
  id: string;
  full_path: string;
  description: string;
  tags: string;
  created_at: string;
  updated_at: string;
}

declare global {
  interface Window {
    electronAPI: ElectronAPI;
  }
}

