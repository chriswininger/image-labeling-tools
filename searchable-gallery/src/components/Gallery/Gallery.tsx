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
  const [searchTags, setSearchTags] = useState<string[]>([]);

  // Initial fetch on mount (all images)
  useEffect(() => {
    if (status === 'idle') {
      dispatch(fetchAllImages(undefined));
    }
  }, [status, dispatch]);

  // Track if search has been used (to distinguish between initial empty state and cleared search)
  const [hasSearched, setHasSearched] = useState(false);

  // Fetch images when search tags change (triggered by Search button)
  useEffect(() => {
    if (hasSearched) {
      const filterOptions = searchTags.length > 0
        ? { tags: searchTags }
        : undefined;
      dispatch(fetchAllImages(filterOptions));
    }
  }, [dispatch, searchTags, hasSearched]);

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
          onChange={setSelectedValues}
          placeholder="tags"
        />
        <div className="gallery-search-button-wrapper">
          <button
            className="gallery-search-button"
            onClick={handleSearch}
          >
            Search
          </button>
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

  function handleSearch(){
    setSearchTags([...selectedValues]);
    setHasSearched(true);
  }
}

export default Gallery;

