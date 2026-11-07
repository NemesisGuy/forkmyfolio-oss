# Stage 1: Build the Vue.js app
FROM node:22-alpine AS build-stage
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Serve the app using Nginx
FROM nginx:1.30-alpine AS production-stage
COPY --from=build-stage /app/dist /usr/share/nginx/html

# Add entrypoint for runtime env injection
COPY ./entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Copy a custom nginx configuration file
COPY ./nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
ENTRYPOINT ["/entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]