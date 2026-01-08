import { useEffect, useState } from 'react';
import type { ImageData } from '../../types/electron';
import './ImageModal.css';

interface ImageModalProps {
  image: ImageData | null;
  onClose: () => void;
  onTagClick?: (tag: string) => void;
}

function ImageModal({ image, onClose, onTagClick }: ImageModalProps) {
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(loadImage, [image]);
  useEffect(registerKeyListeners, [image, onClose]);

  if (!image) {
    return null;
  }

  const tags = parseTags(image.tags || '');
  const filename = getFilename(image.full_path);

  return (
    <div className="image-modal-overlay" onClick={handleOverlayClick}>
      <div className="image-modal-content" onClick={(e) => e.stopPropagation()}>
        <button className="image-modal-close" onClick={onClose} aria-label="Close modal">
          ×
        </button>

        <div className="image-modal-header">
          <div className="image-modal-filename">{filename}</div>
        </div>

        <div className="image-modal-image-container">
          {loading ? (
            <div className="image-modal-loading">Loading image...</div>
          ) : error ? (
            <div className="image-modal-error">{error}</div>
          ) : imageUrl ? (
            <img
              src={imageUrl}
              alt={image.description || 'Image'}
              className="image-modal-image"
              onError={() => setError('Failed to load image')}
            />
          ) : null}
        </div>

        <div className="image-modal-body">
          <div className="image-modal-description">{image.description}</div>

          {tags.length > 0 && (
            <div className="image-modal-tags">
              {tags.map((tag, index) => (
                <span
                  key={index}
                  className="tag-pill"
                  style={{ backgroundColor: getTagColor(tag) }}
                  title={tag}
                  onClick={(e) => {
                    e.stopPropagation();
                    onTagClick?.(tag);
                  }}
                >
                  {tag}
                </span>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );

  function handleOverlayClick(e: React.MouseEvent<HTMLDivElement>) {
    if (e.target === e.currentTarget) {
      onClose();
    }
  }

  function getFilename(fullPath: string): string {
    const parts = fullPath.split(/[/\\]/);
    return parts[parts.length - 1] || fullPath;
  }

  function parseTags(tagsString: string): string[] {
    if (!tagsString || tagsString.trim() === '') {
      return [];
    }
    return tagsString.split(',')
      .map(tag => tag.trim())
      .filter(tag => tag.length > 0)
      .sort();
  }

  function getTagColor(tag: string): string {
    let hash = 0;
    for (let i = 0; i < tag.length; i++) {
      hash = tag.charCodeAt(i) + ((hash << 5) - hash);
    }
    const hue = Math.abs(hash) % 360;
    return `hsl(${hue}, 65%, 50%)`;
  }

  function registerKeyListeners() {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && image) {
        onClose();
      }
    };

    if (image) {
      document.addEventListener('keydown', handleEscape);
      // Prevent body scroll when modal is open
      document.body.style.overflow = 'hidden';
    }

    return () => {
      document.removeEventListener('keydown', handleEscape);
      document.body.style.overflow = '';
    };
  }

  function loadImage() {
    if (!image) {
      setImageUrl(null);
      setError(null);
      return;
    }

    setLoading(true);
    setError(null);

    // We need to wrap the async bit as described here https://devtrium.com/posts/async-functions-useeffect
    const doImageLoad = async () => {
      return await window.electronAPI.getImageData(image.full_path);
    }

    doImageLoad()
      .catch((err) => {
        console.error('Error loading image:', err);
        setError('Failed to load image');
        setLoading(false)
      })
      .then((dataUrl: string) => {
        setImageUrl(dataUrl);
        setLoading(false)
      });
  }
}

export default ImageModal;
