import { useEffect, useState, useMemo } from 'react';
import { useAppDispatch, useAppSelector } from '../../store/hooks';
import { fetchAllImages, fetchAllTags } from '../../store/gallerySlice';
import ImageThumbnail from '../ImageThumbnail/ImageThumbnail';
import MultiSelect, { MultiSelectOption } from '../MultiSelect/MultiSelect';
import './Gallery.css'

function Gallery() {
  const dispatch = useAppDispatch();
  const { images, tags, status, tagsStatus, error } = useAppSelector((state) => state.gallery);
  const [selectedValues, setSelectedValues] = useState<string[]>([]);

  // Load images; fires again if filter options change
  useEffect(() => {
    const filterOptions = selectedValues.length > 0
      ? { tags: selectedValues }
      : undefined;
    dispatch(fetchAllImages(filterOptions));

  }, [dispatch, selectedValues]);

  // Load Tags -- if not already loading
  useEffect(() => {
    if (tagsStatus === 'idle') {
      dispatch(fetchAllTags());
    }
  }, [tagsStatus, dispatch]);

  // Convert tags to MultiSelectOption format (both value and label are tag_name)
  const options: MultiSelectOption[] = useMemo(() => {
    return tags.map((tag) => ({
      value: tag.tag_name,
      label: tag.tag_name,
    }));
  }, [tags]);

  if (status === 'loading') {
    return <div className="gallery-loading">Loading images...</div>;
  }

  if (status === 'failed') {
    return <div className="gallery-error">Error: {error}</div>;
  }

  return (
    <div className="gallery">
      <div className="gallery-search-controls">
        <MultiSelect
          label="Search by tags:"
          options={options}
          selectedValues={selectedValues}
          onChange={onTagsChanged}
          placeholder="tags"
        />
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

  function onTagsChanged(val: string[]) {
    setSelectedValues([...val]);
  }
}

export default Gallery;
