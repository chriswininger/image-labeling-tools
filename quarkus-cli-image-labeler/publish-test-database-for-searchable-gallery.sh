#!/usr/bin/env bash
set -e

# This script is used to update the schema for tests in image-labeling-tool
# It assumes you've already built quarkus-cli-image-labeler
# You run this after you've updated the schema through migrations in this module and are ready to make the
# searchable-gallery aware of those changes

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

IL_DATA_LOCATION="${SCRIPT_DIR}/../searchable-gallery/test-data/" \
  java -jar "${SCRIPT_DIR}/build/quarkus-app/quarkus-run.jar" run-migrations-command
