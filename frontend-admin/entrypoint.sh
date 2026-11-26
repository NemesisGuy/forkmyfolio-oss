#!/bin/sh

# Get the root directory of the Nginx server
ROOT_DIR=/usr/share/nginx/html

# Substitute the non-secret runtime placeholders in config.js.
# Using a different separator | for sed because URLs contain /.
# Social login is disabled by default: unset placeholders resolve to "false"
# and an empty provider list. No issuer URL or credential is ever injected here.
echo "Substituting runtime config placeholders in config.js"
sed -i "s|##VITE_API_BASE_URL##|${VITE_API_BASE_URL}|g" ${ROOT_DIR}/config.js
sed -i "s|##VITE_SOCIAL_LOGIN_ENABLED##|${VITE_SOCIAL_LOGIN_ENABLED:-false}|g" ${ROOT_DIR}/config.js
sed -i "s|##VITE_SOCIAL_PROVIDERS##|${VITE_SOCIAL_PROVIDERS:-}|g" ${ROOT_DIR}/config.js

# This line is important! It executes the command passed to the script.
# In our case, it will be `nginx -g 'daemon off;'`
exec "$@"