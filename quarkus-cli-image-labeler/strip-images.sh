#!/bin/bash
# Strips base64 image content from log files to make them easier to read
# Usage: ./strip-images.sh input.log > output.log
#    or: ./strip-images.sh input.log (prints to stdout)

if [ -z "$1" ]; then
    echo "Usage: $0 <logfile>" >&2
    exit 1
fi

sed -E 's/"images" : \[ "[^"]*" \]/"images" : [ "..." ]/g' "$1"
