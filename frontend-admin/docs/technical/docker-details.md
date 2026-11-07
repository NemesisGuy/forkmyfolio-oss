# Standalone Docker Guide

This project is fully containerized and can be run as a standalone Docker container. This is useful for deployments where you are managing the backend and database separately.

The project uses a multi-stage `Dockerfile` with Nginx to serve the application and an entrypoint script that allows for **runtime environment variable configuration**.

This means you can configure the `API_BASE_URL` when you start the container, without having to rebuild the image.

---

## Building the Docker Image

If you want to build the image yourself instead of using the one from Docker Hub:
```bash
docker build -t forkmyfolio-frontend .
```

## Running the Docker Container

To run the container, you must provide the public URL of your backend via the `API_BASE_URL` environment variable.

```bash
docker run -d -p 8080:80 \
  -e VITE_API_BASE_URL=http://your-backend-api-url.com/api/v1 \
  --name forkmyfolio-frontend-container \
  nemesisguy/forkmyfolio-frontend:latest
```

The application will then be available on port `8080` of your host machine: `http://localhost:8080`.