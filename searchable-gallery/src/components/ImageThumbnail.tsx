import { useState, useEffect } from 'react';
import type { ImageData } from '../types/electron';

interface ImageThumbnailProps {
  image: ImageData;
}

function ImageThumbnail({ image }: ImageThumbnailProps) {
  const [thumbnailUrl, setThumbnailUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Load image data via IPC as base64 data URL
    const loadImage = async () => {
      try {
        if (!window.electronAPI) {
          throw new Error('Electron API not available');
        }
        const dataUrl = await window.electronAPI.getImageData(image.full_path);
        setThumbnailUrl(dataUrl);
      } catch (err) {
        console.error('Error loading image:', err);
        setError('Failed to load image');
      }
    };

    loadImage();
  }, [image.full_path]);

  return (
    <div className="image-thumbnail">
      {error ? (
        <div className="thumbnail-error">{error}</div>
      ) : thumbnailUrl ? (
        <img
          src={thumbnailUrl}
          alt={image.description || 'Image'}
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

