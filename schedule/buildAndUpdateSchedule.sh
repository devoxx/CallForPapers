#!/usr/bin/env bash

echo "Deleting Schedule 'dist' folder, if present"
rm -fr dist || true

echo "Building Schedule app via 'ng build'"
time ng build

if [ -d "dist" ]; then
  echo "Copying Schedule 'dist' folder, into ../public/schedule except ./dist/index.html"
  time rsync --progress --human-readable --archive --verbose --update --exclude *index.html ./dist/*.* ../public/schedule/
else
  echo "'dist' folder is missing, maybe the 'ng build' process failed."
fi
