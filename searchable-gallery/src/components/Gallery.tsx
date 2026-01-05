import { useEffect, useState } from 'react';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { fetchAllImages } from '../store/gallerySlice';
import ImageThumbnail from './ImageThumbnail';
import MultiSelect, { MultiSelectOption } from './MultiSelect';

function Gallery() {
  const dispatch = useAppDispatch();
  const { images, status, error } = useAppSelector((state) => state.gallery);
  const [selectedValues, setSelectedValues] = useState<string[]>([]);

  useEffect(() => {
    if (status === 'idle') {
      dispatch(fetchAllImages());
    }
  }, [status, dispatch]);

  const options: MultiSelectOption[] = [
    { value: 'angular', label: 'Angular' },
    { value: 'bootstrap', label: 'Bootstrap' },
    { value: 'react', label: 'React.js' },
    { value: 'vue', label: 'Vue.js' },
    { value: 'django', label: 'Django' },
    { value: 'laravel', label: 'Laravel' },
    { value: 'nodejs', label: 'Node.js' },
  ];

  if (status === 'loading') {
    return <div className="gallery-loading">Loading images...</div>;
  }

  if (status === 'failed') {
    return <div className="gallery-error">Error: {error}</div>;
  }

  return (
    <div className="gallery">
      <div style={{ padding: '2rem', maxWidth: '500px', marginBottom: '2rem' }}>
        <h2>MultiSelect Example</h2>
        <MultiSelect
          label="Framework"
          options={options}
          selectedValues={selectedValues}
          onChange={setSelectedValues}
          placeholder="Please select your framework."
        />
        <div style={{ marginTop: '1rem', fontSize: '0.875rem', color: '#666' }}>
          Selected: {selectedValues.length > 0 ? selectedValues.join(', ') : 'None'}
        </div>
      </div>

      {images.length === 0 ? (
        <div className="gallery-empty">No images found</div>
      ) : (
        <div className="gallery-grid">
          {images.map((image) => (
            <ImageThumbnail key={image.id} image={image} />
          ))}
        </div>
      )}
    </div>
  );
}

export default Gallery;

