import { useState, useEffect } from 'react';
import type { ImageData } from '../../types/electron';
import "./ImageThumbnail.css"

interface ImageThumbnailProps {
  image: ImageData;
}

function ImageThumbnail({ image }: ImageThumbnailProps) {
  const [thumbnailUrl, setThumbnailUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Load the thumbnail first, fall back to full image if thumbnail doesn't exist
    const loadImage = async () => {
      try {
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

  const tags = parseTags(image.tags || '');

  return (
    <div className="image-thumbnail">
      <div className="thumbnail-description" title={image.description}>
        {image.description}
      </div>
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
      {tags.length > 0 && (
        <div className="thumbnail-info">
          <div className="thumbnail-tags">
            {tags.map((tag, index) => (
              <span
                key={index}
                className="tag-pill"
                style={{backgroundColor: getTagColor(tag)}}
                title={tag}
              >
                {tag}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );

  // Parse tags from comma-separated string
  function parseTags(tagsString: string): string[] {
    if (!tagsString || tagsString.trim() === '') {
      return [];
    }
    return tagsString.split(',').map(tag => tag.trim()).filter(tag => tag.length > 0);
  }

  // Generate a color for a tag based on its hash
  function getTagColor(tag: string): string {
    // Generate a hash from the tag string
    let hash = 0;
    for (let i = 0; i < tag.length; i++) {
      hash = tag.charCodeAt(i) + ((hash << 5) - hash);
    }

    // Generate a color with good contrast (using HSL)
    const hue = Math.abs(hash) % 360;
    // Use a medium saturation and lightness for good readability
    return `hsl(${hue}, 65%, 50%)`;
  }
}

export default ImageThumbnail;

