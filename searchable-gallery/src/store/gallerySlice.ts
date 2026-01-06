import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import type { ImageData, TagData, ImageFilterOptions } from '../types/electron';

interface GalleryState {
  images: ImageData[];
  tags: TagData[];
  status: 'idle' | 'loading' | 'succeeded' | 'failed';
  tagsStatus: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
  tagsError: string | null;
}

const initialState: GalleryState = {
  images: [],
  tags: [],
  status: 'idle',
  tagsStatus: 'idle',
  error: null,
  tagsError: null,
};

export const fetchAllImages = createAsyncThunk(
  'gallery/fetchAllImages',
  async (filterOptions?: ImageFilterOptions) => {
    if (!window.electronAPI) {
      throw new Error('Electron API not available');
    }
    const images = await window.electronAPI.getAllImages(filterOptions);
    return images;
  }
);

export const fetchAllTags = createAsyncThunk(
  'gallery/fetchAllTags',
  async () => {
    if (!window.electronAPI) {
      throw new Error('Electron API not available');
    }
    const tags = await window.electronAPI.getAllTags();
    return tags;
  }
);

const gallerySlice = createSlice({
  name: 'gallery',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchAllImages.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(fetchAllImages.fulfilled, (state, action: PayloadAction<ImageData[]>) => {
        state.status = 'succeeded';
        state.images = action.payload;
      })
      .addCase(fetchAllImages.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.error.message || 'Failed to fetch images';
      })
      .addCase(fetchAllTags.pending, (state) => {
        state.tagsStatus = 'loading';
        state.tagsError = null;
      })
      .addCase(fetchAllTags.fulfilled, (state, action: PayloadAction<TagData[]>) => {
        state.tagsStatus = 'succeeded';
        state.tags = action.payload;
      })
      .addCase(fetchAllTags.rejected, (state, action) => {
        state.tagsStatus = 'failed';
        state.tagsError = action.error.message || 'Failed to fetch tags';
      });
  },
});

export default gallerySlice.reducer;

