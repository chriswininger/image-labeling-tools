import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import type { ImageData } from '../types/electron';

interface GalleryState {
  images: ImageData[];
  status: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
}

const initialState: GalleryState = {
  images: [],
  status: 'idle',
  error: null,
};

export const fetchAllImages = createAsyncThunk(
  'gallery/fetchAllImages',
  async () => {
    if (!window.electronAPI) {
      throw new Error('Electron API not available');
    }
    const images = await window.electronAPI.getAllImages();
    return images;
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
      });
  },
});

export default gallerySlice.reducer;

