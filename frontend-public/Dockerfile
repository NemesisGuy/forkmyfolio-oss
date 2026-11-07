# Build stage
FROM node:22-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Production stage
FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY --from=builder /app/.output ./.output
COPY --from=builder /app/package*.json ./
RUN npm ci --omit=dev
EXPOSE 3000
# Health endpoint: the Nuxt app serves the public site root ("/"). The
# original HEALTHCHECK referenced a non-existent /__nuxt_health route and
# would report the container unhealthy when the image is run standalone
# (compose-level healthchecks override this).
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=5 CMD wget --no-verbose --tries=1 --spider http://localhost:3000/ || exit 1
CMD ["node", ".output/server/index.mjs"]
