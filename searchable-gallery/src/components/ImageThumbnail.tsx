import { useState, useEffect } from 'react';
import type { ImageData } from '../types/electron';

interface ImageThumbnailProps {
  image: ImageData;
}

function ImageThumbnail({ image }: ImageThumbnailProps) {
  const [thumbnailUrl, setThumbnailUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Load thumbnail first, fall back to full image if thumbnail doesn't exist
    const loadImage = async () => {
      try {
        if (!window.electronAPI) {
          throw new Error('Electron API not available');
        }
        
        // Try to load thumbnail first if available
        if (image.thumb_nail_name) {
          try {
            // Construct thumbnail path relative to data directory
            // The thumbnail is stored at data/thumbnails/{thumb_nail_name}
            const thumbnailPath = await window.electronAPI.getThumbnailPath(image.thumb_nail_name);
            const dataUrl = await window.electronAPI.getImageData(thumbnailPath);
            setThumbnailUrl(dataUrl);
            return;
          } catch (thumbErr) {
            console.warn('Failed to load thumbnail, falling back to full image:', thumbErr);
            // Fall through to load full image
          }
        }
        
        // Fall back to full image
        const dataUrl = await window.electronAPI.getImageData(image.full_path);
        setThumbnailUrl(dataUrl);
      } catch (err) {
        console.error('Error loading image:', err);
        setError('Failed to load image');
      }
    };

    loadImage();
  }, [image.full_path, image.thumb_nail_name]);

  return (
    <div className="image-thumbnail">
      {error ? (
        <div className="thumbnail-error">{error}</div>
      ) : thumbnailUrl ? (
        <img
          src={thumbnailUrl}
          alt={image.description || 'Image'}
          title={image.description || ''}
          className="thumbnail-image"
          onError={() => setError('Failed to load image')}
        />
      ) : (
        <div className="thumbnail-loading">Loading...</div>
      )}
      <div className="thumbnail-info">
        <div className="thumbnail-description" title={image.description}>
          {image.description}
        </div>
      </div>
    </div>
  );
}

export default ImageThumbnail;

