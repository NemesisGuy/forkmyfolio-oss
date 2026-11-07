# Docker Compose Deployment Guide

This guide provides step-by-step instructions for deploying the entire ForkMyFolio application stack (Frontend, Backend, and Database) using Docker Compose.

---

## Prerequisites

Before you begin, ensure you have the following installed on your system:
- **Docker**: [Get Docker](https://docs.docker.com/get-docker/)
- **Docker Compose**: Included with Docker Desktop. For Linux servers, you may need to install it separately.

---

## Step 1: Create the Environment File (`.env`)

Docker Compose uses an `.env` file in the same directory as the `docker-compose.yaml` file to manage secrets and configuration.

Create a file named `.env` and paste the following content into it. You **must** fill in the placeholder values.

```env
# --- Backend Configuration ---

# This is the public-facing URL of your frontend.
# It's used by the backend for CORS configuration to allow your frontend to make API requests.
APP_CORS_ALLOWED_ORIGINS=http://localhost:8089

# A long, random string used to sign JWT tokens.
# You can generate one using: openssl rand -base64 32
JWT_SECRET_KEY=your_super_secret_jwt_key_here

# The initial password for the default 'admin' user.
# You will be forced to change this on first login.
DEFAULT_ADMIN_PASSWORD=changeme

# --- Frontend Configuration ---

# This is the URL that the frontend will use to communicate with the backend API.
# It must be the public URL of the backend service.
VITE_API_BASE_URL=http://localhost:8080/api/v1

# --- Database Configuration ---

# The username for the MySQL database. 'root' is fine for most cases.
DB_USERNAME=root

# The root password for the MySQL database.
# This MUST be a strong, secure password.
DB_PASSWORD=your_very_secure_database_password_here
```

---

## Step 2: Running the Application

Once you have created and configured your `.env` file, you can start the entire application stack with a single command:

```bash
docker-compose up -d
```

Docker Compose will now pull the required images from Docker Hub and start the three services (frontend, backend, and database). The `-d` flag runs the containers in detached mode (in the background).

---

## Step 3: Accessing the Application

-   **Frontend**: The frontend will be accessible at `http://localhost:8089`.
-   **Backend API**: The backend API will be accessible at `http://localhost:8080`.

The default admin user is `admin@forkmyfolio.com` with the password you set in the `.env` file. You will be required to change this password upon your first login.

---

## Step 4: Managing the Stack

-   **To stop the application:**
    ```bash
    docker-compose down
    ```
-   **To view logs for all services:**
    ```bash
    docker-compose logs -f
    ```
-   **To view logs for a specific service (e.g., the backend):**
    ```bash
    docker-compose logs -f app
    ```
