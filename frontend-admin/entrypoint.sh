#!/bin/sh

# Get the root directory of the Nginx server
ROOT_DIR=/usr/share/nginx/html

# Replace the placeholder in the config.js file
# Using a different separator | for sed because URLs contain /
echo "Replacing placeholder in config.js with value: ${VITE_API_BASE_URL}"
sed -i "s|##VITE_API_BASE_URL##|${VITE_API_BASE_URL}|g" ${ROOT_DIR}/config.js

# This line is important! It executes the command passed to the script.
# In our case, it will be `nginx -g 'daemon off;'`
exec "$@"