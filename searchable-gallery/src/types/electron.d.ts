export interface TagData {
  id: number;
  tag_name: string;
  created_at: string;
  updated_at: string;
}

export interface ImageFilterOptions {
  tags?: string[];
}

export interface ElectronAPI {
  getAllImages: (filterOptions?: ImageFilterOptions) => Promise<ImageData[]>;
  getAllTags: () => Promise<TagData[]>;
  getImageData: (imagePath: string) => Promise<string>;
  getThumbnailPath: (thumbnailName: string) => Promise<string>;
}

export interface ImageData {
  id: string;
  full_path: string;
  description: string;
  tags: string;
  thumb_nail_name: string | null;
  created_at: string;
  updated_at: string;
}

declare global {
  interface Window {
    electronAPI: ElectronAPI;
  }
}

