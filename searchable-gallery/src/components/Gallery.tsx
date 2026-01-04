import { useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { fetchAllImages } from '../store/gallerySlice';
import ImageThumbnail from './ImageThumbnail';

function Gallery() {
  const dispatch = useAppDispatch();
  const { images, status, error } = useAppSelector((state) => state.gallery);

  useEffect(() => {
    if (status === 'idle') {
      dispatch(fetchAllImages());
    }
  }, [status, dispatch]);

  if (status === 'loading') {
    return <div className="gallery-loading">Loading images...</div>;
  }

  if (status === 'failed') {
    return <div className="gallery-error">Error: {error}</div>;
  }

  if (images.length === 0) {
    return <div className="gallery-empty">No images found</div>;
  }

  return (
    <div className="gallery">
      <div className="gallery-grid">
        {images.map((image) => (
          <ImageThumbnail key={image.id} image={image} />
        ))}
      </div>
    </div>
  );
}

export default Gallery;

