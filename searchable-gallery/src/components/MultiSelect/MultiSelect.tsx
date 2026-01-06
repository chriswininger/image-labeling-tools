import { useState, useRef, useEffect } from 'react';
import './MultiSelect.css';

export interface MultiSelectOption {
  value: string;
  label: string;
}

interface MultiSelectProps {
  options: MultiSelectOption[];
  selectedValues: string[];
  onChange: (selectedValues: string[]) => void;
  placeholder?: string;
  label?: string;
  className?: string;
}

function MultiSelect({
  options,
  selectedValues,
  onChange,
  placeholder = 'Select options...',
  label,
  className = '',
}: MultiSelectProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const wrapperRef = useRef<HTMLDivElement>(null);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  // Filter options based on search query
  const filteredOptions = options.filter((option) =>
    option.label.toLowerCase().includes(searchQuery.toLowerCase()));

  // Get selected options
  const selectedOptions = options.filter((option) => selectedValues.includes(option.value));

  const allFilteredSelected = filteredOptions.length > 0
    && filteredOptions.every((option) => selectedValues.includes(option.value));
  
  const someFilteredSelected = filteredOptions.some((option) =>
    selectedValues.includes(option.value));


  return (
    <div className={`multi-select-wrapper ${className}`} ref={wrapperRef}>
      {label && <label className="multi-select-label">{label}</label>}
      <div
        className={`multi-select ${isOpen ? 'multi-select-open' : ''}`}
        onClick={() => setIsOpen(!isOpen)}
      >
        <div className="multi-select-input">
          <SelectedOptions handleRemoveTag={handleRemoveTag} selectedOptions={selectedOptions} />

          <input
            type="text"
            className="multi-select-search"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onClick={(e) => e.stopPropagation()}
            onFocus={() => setIsOpen(true)}
            placeholder={selectedOptions.length > 0 ? '' : placeholder}
          />
        </div>

        {/* This gets roted when the parent dive has the multi-select-open class applied by
            a CSS transfrom transform: rotate(180deg); */}
        <DropDownChevron />
      </div>

      <DropDownMenu
        isOpen={isOpen}
        filteredOptions={filteredOptions}
        someFilteredSelected={someFilteredSelected}
        selectedValues={selectedValues}
        handleSelectAll={handleSelectAll}
        handleDeselectAll={handleDeselectAll}
        handleToggleOption={handleToggleOption}
      />
    </div>
  );

  function handleToggleOption(value: string) {
    if (selectedValues.includes(value)) {
      onChange(selectedValues.filter((v) => v !== value));
    } else {
      onChange([...selectedValues, value]);
      // Clear search input when selecting an item
      setSearchQuery('');
    }
  }

  function handleRemoveTag(value: string, event: React.MouseEvent) {
    event.stopPropagation();
    onChange(selectedValues.filter((v) => v !== value));
  }

  function handleClearAll(event: React.MouseEvent) {
    event.stopPropagation();
    onChange([]);
  }

  function handleSelectAll() {
    const allValues = filteredOptions.map((option) => option.value);
    const newSelectedValues = [...new Set([...selectedValues, ...allValues])];
    onChange(newSelectedValues);
  }

  function handleDeselectAll() {
    const filteredValues = filteredOptions.map((option) => option.value);
    onChange(selectedValues.filter((value) => !filteredValues.includes(value)));
  };
}

function SelectedOptions(
  { selectedOptions, handleRemoveTag } :
  { selectedOptions: MultiSelectOption [], handleRemoveTag: (value: string, event: React.MouseEvent) => void}
) {
  if (selectedOptions.length <= 0) {
    return null;
  }

  return <>
    {selectedOptions.map((option) => (
      <span key={option.value} className="multi-select-tag">
        {option.label}
        <button
          type="button"
          className="multi-select-tag-close"
          onClick={(e) => handleRemoveTag(option.value, e)}
          aria-label={`Remove ${option.label}`}
        >
          ×
        </button>
      </span>
    ))}
  </>
}

function DropDownChevron() {
  return <div className="multi-select-indicator">
    <svg
      width="12"
      height="8"
      viewBox="0 0 12 8"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d="M1 1L6 6L11 1"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  </div>;
}

function DropDownMenu(
  { isOpen, filteredOptions, someFilteredSelected, selectedValues, handleSelectAll, handleDeselectAll, handleToggleOption } :
  {
    isOpen: boolean,
    filteredOptions: MultiSelectOption [],
    someFilteredSelected: boolean,
    selectedValues: string[],
    handleSelectAll: () => void,
    handleDeselectAll: () => void,
    handleToggleOption: (val: string) => void,
  }
) {
  if (!isOpen) {
    return null;
  }

  return <div className="multi-select-dropdown">
    <div className="multi-select-dropdown-header">
      <button
        type="button"
        className="multi-select-select-all"
        onClick={handleSelectAll}
      >
        Select all options
      </button>
      {someFilteredSelected && (
        <button
          type="button"
          className="multi-select-deselect-all"
          onClick={handleDeselectAll}
        >
          Deselect all
        </button>
      )}
    </div>
    <div className="multi-select-options">
      {filteredOptions.length === 0 ? (
        <div className="multi-select-no-results">No results found</div>
      ) : (
        filteredOptions.map((option) => {
          const isSelected = selectedValues.includes(option.value);
          return (
            <label
              key={option.value}
              className={`multi-select-option ${isSelected ? 'multi-select-option-selected' : ''}`}
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  handleToggleOption(option.value);
                }
              }}
            >
              <input
                type="checkbox"
                checked={isSelected}
                onChange={() => handleToggleOption(option.value)}
                onClick={(e) => e.stopPropagation()}
              />
              <span className="multi-select-option-label">{option.label}</span>
            </label>
          );
        })
      )}
    </div>
  </div>
}

export default MultiSelect;
