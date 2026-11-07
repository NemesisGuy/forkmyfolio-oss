#!/bin/sh
# This script runs when the container starts.

# Find the config template and generate the final config.js
envsubst '${API_BASE_URL}' < /usr/share/nginx/html/config.template.js > /usr/share/nginx/html/config.js

# Echo the generated config for debugging
echo "Generated /usr/share/nginx/html/config.js with content:"
cat /usr/share/nginx/html/config.js

# Start the Nginx server
echo "Starting Nginx..."
exec nginx -g 'daemon off;'