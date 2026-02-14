# Usage

All commands are run via:

```bash
java -jar ./build/quarkus-app/quarkus-run.jar <command> [options]
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `IL_DATA_LOCATION` | Override the data directory (database, thumbnails, logs) | `data` |

## Commands

### generate-image-tags

Generates tags, descriptions, and titles for images using AI (Ollama) and prints the results to stdout. Does not persist anything to the database.

```bash
java -jar ./build/quarkus-app/quarkus-run.jar generate-image-tags <path>
```

| Argument | Description |
|----------|-------------|
| `<path>` | Path to an image file or a directory of images |

### write-tags-to-local-db

Generates tags, descriptions, and titles for images and saves the results to the local SQLite database. Also generates thumbnails.

```bash
java -jar ./build/quarkus-app/quarkus-run.jar write-tags-to-local-db [options] <path>
```

| Argument | Description |
|----------|-------------|
| `<path>` | Path to an image file or a directory of images |

| Option | Description | Default |
|--------|-------------|---------|
| `--update-existing` | Re-process and update images that already exist in the database | `false` |
| `--parallelism <n>` | Number of images to process concurrently | `1` |

When processing a directory, failed images are logged to `<data-dir>/failed-image-processing-<timestamp>.log`.

**Note:** When using `--parallelism` greater than 1, ensure Ollama is configured with a matching `OLLAMA_NUM_PARALLEL` value. See [Ollama_Notes.md](Ollama_Notes.md) for details.

### read-file-metadata

Reads and displays all EXIF/metadata from an image file.

```bash
java -jar ./build/quarkus-app/quarkus-run.jar read-file-metadata <image-path>
```

| Argument | Description |
|----------|-------------|
| `<image-path>` | Path to an image file |

### randomize-gps-coordinates

Creates copies of JPEG images with GPS coordinates replaced by fake coordinates from famous landmarks. Original files are not modified. Output files are named `<original>-safe.<ext>`.

```bash
java -jar ./build/quarkus-app/quarkus-run.jar randomize-gps-coordinates [options] <path>
```

| Argument | Description |
|----------|-------------|
| `<path>` | Path to a JPEG file or a directory containing JPEGs |

| Option | Description | Default |
|--------|-------------|---------|
| `-r`, `--recursive` | Process directories recursively | `false` |

Prompts for confirmation before processing.

### run-migrations

Runs Flyway database migrations. Migrations also run automatically at application startup, so this command is primarily useful for verifying migration status.

```bash
java -jar ./build/quarkus-app/quarkus-run.jar run-migrations
```

No arguments or options. Displays the last applied migration and a summary of applied/pending migrations.

## Supported Image Formats

Commands that process images support: **jpg, jpeg, png, gif, bmp, webp, tiff, tif**

The `randomize-gps-coordinates` command only supports **jpg, jpeg** (due to EXIF rewriting limitations).
