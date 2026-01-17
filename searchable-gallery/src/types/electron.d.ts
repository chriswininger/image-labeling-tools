export interface TagData {
  id: number;
  tag_name: string;
  created_at: string;
  updated_at: string;
}

export interface ImageFilterOptions {
  tags?: string[];
  joinType?: 'and' | 'or';
}

export interface Preferences {
  showTagsInGallery: boolean;
}

export interface PreferenceChange {
  key: string;
  value: unknown;
}

export interface ElectronAPI {
  getAllImages: (filterOptions?: ImageFilterOptions) => Promise<ImageData[]>;
  getAllTags: () => Promise<TagData[]>;
  getImageData: (imagePath: string) => Promise<string>;
  getThumbnailPath: (thumbnailName: string) => Promise<string>;
  getPreferences: () => Promise<Preferences>;
  onPreferenceChanged: (callback: (preference: PreferenceChange) => void) => () => void;
}

export interface ImageData {
  id: string;
  full_path: string;
  description: string;
  short_title: string | null;
  tags: string;
  thumb_nail_name: string | null;
  text_contents: string | null;
  created_at: string;
  updated_at: string;
}

declare global {
  interface Window {
    electronAPI: ElectronAPI;
  }
}

