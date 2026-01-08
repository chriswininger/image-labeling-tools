import { useEffect, useState, useMemo } from 'react';
import { useAppDispatch, useAppSelector } from '../../store/hooks';
import { fetchAllImages, fetchAllTags } from '../../store/gallerySlice';
import ImageThumbnail from '../ImageThumbnail/ImageThumbnail';
import ImageModal from '../ImageModal/ImageModal';
import MultiSelect, { MultiSelectOption } from '../MultiSelect/MultiSelect';
import type { ImageData } from '../../types/electron';
import './Gallery.css'

function Gallery() {
  const dispatch = useAppDispatch();
  const { images, tags, status, tagsStatus, error } = useAppSelector((state) => state.gallery);
  const [selectedValues, setSelectedValues] = useState<string[]>([]);
  const [joinType, setJoinType] = useState<'and' | 'or'>('and');
  const [selectedImage, setSelectedImage] = useState<ImageData | null>(null);

  // Load images; fires again if filter options change
  useEffect(() => {
    const filterOptions = selectedValues.length > 0
      ? { tags: selectedValues, joinType }
      : undefined;
    dispatch(fetchAllImages(filterOptions));

  }, [dispatch, selectedValues, joinType]);

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
        <div className="gallery-search-controls-group">
          <MultiSelect
            label="Search by tags:"
            options={options}
            selectedValues={selectedValues}
            onChange={onTagsChanged}
            placeholder="tags"
          />

          <div className="gallery-join-type">
            <div className="gallery-radio-group">
              <label className="gallery-join-type-label">Join With:</label>
              <label className="gallery-radio-label">
                <input
                  type="radio"
                  name="joinType"
                  value="or"
                  checked={joinType === 'or'}
                  onChange={(e) => setJoinType(e.target.value as 'and' | 'or')}
                  className="gallery-radio-input"
                />
                <span>Or</span>
              </label>
              <label className="gallery-radio-label">
                <input
                  type="radio"
                  name="joinType"
                  value="and"
                  checked={joinType === 'and'}
                  onChange={(e) => setJoinType(e.target.value as 'and' | 'or')}
                  className="gallery-radio-input"
                />
                <span>And</span>
              </label>
            </div>
          </div>
        </div>
      </div>

      {images.length === 0 ? (
        <div className="gallery-empty">No images found</div>
      ) : (
        <div className="gallery-grid">
          {images.map((image) => (
            <ImageThumbnail
              key={image.id}
              image={image}
              onTagClick={handleTagClick}
              onClick={() => setSelectedImage(image)}
            />
          ))}
        </div>
      )}

      <ImageModal
        image={selectedImage}
        onClose={() => setSelectedImage(null)}
        onTagClick={handleModalTagClick}
      />
    </div>
  );

  function onTagsChanged(val: string[]) {
    setSelectedValues([...val]);
  }

  function handleTagClick(tag: string) {
    // Add tag to selected tags if it's not already selected
    if (!selectedValues.includes(tag)) {
      setSelectedValues([...selectedValues, tag]);
    }
  }

  function handleModalTagClick(tag: string) {
    // Add tag to selected tags if it's not already selected
    if (!selectedValues.includes(tag)) {
      setSelectedValues([...selectedValues, tag]);
    }
    // Close the modal when a tag is clicked
    setSelectedImage(null);
  }
}

export default Gallery;
