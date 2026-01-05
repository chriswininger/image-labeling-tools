#!/bin/bash

# Script to copy data from quarkus-cli-image-labeler/data to searchable-gallery/data
# This will overwrite any existing data in searchable-gallery/data

set -e  # Exit on error

SOURCE_DIR="quarkus-cli-image-labeler/data"
DEST_DIR="searchable-gallery/data"

# Get the script directory (project root)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

SOURCE_PATH="${SCRIPT_DIR}/${SOURCE_DIR}"
DEST_PATH="${SCRIPT_DIR}/${DEST_DIR}"

# Check if source directory exists
if [ ! -d "$SOURCE_PATH" ]; then
    echo "Error: Source directory does not exist: $SOURCE_PATH"
    exit 1
fi

# Warn the user
echo "=========================================="
echo "WARNING: This script will overwrite all"
echo "existing data in: $DEST_PATH"
echo "=========================================="
echo ""
echo "Source: $SOURCE_PATH"
echo "Destination: $DEST_PATH"
echo ""

# Check if destination already exists and has content
if [ -d "$DEST_PATH" ] && [ "$(ls -A $DEST_PATH 2>/dev/null)" ]; then
    echo "The destination directory already contains data."
    echo ""
fi

# Ask for confirmation
read -p "Do you want to continue? (yes/no): " -r
echo ""

if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    echo "Operation cancelled."
    exit 0
fi

# Create destination directory if it doesn't exist
mkdir -p "$DEST_PATH"

# Copy the data
echo "Copying data from $SOURCE_PATH to $DEST_PATH..."
echo ""

# Use rsync if available, otherwise use cp
if command -v rsync &> /dev/null; then
    rsync -av --delete "$SOURCE_PATH/" "$DEST_PATH/"
else
    # Remove existing destination content first
    if [ -d "$DEST_PATH" ]; then
        rm -rf "${DEST_PATH:?}"/*
    fi
    # Copy all contents
    cp -r "$SOURCE_PATH"/* "$DEST_PATH/" 2>/dev/null || {
        # If cp fails (maybe empty source), try copying hidden files too
        cp -r "$SOURCE_PATH"/. "$DEST_PATH/" 2>/dev/null || true
    }
fi

echo ""
echo "Data copy completed successfully!"
echo "Copied from: $SOURCE_PATH"
echo "To: $DEST_PATH"

