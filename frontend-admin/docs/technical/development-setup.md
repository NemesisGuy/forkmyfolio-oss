# Local Development Guide

Follow these instructions to set up the project for local development. This setup is ideal for working on Vue components and other frontend-specific features.

## Prerequisites

- **Node.js**: version 22+ is recommended.
- **A running instance of the ForkMyFolio backend** (included in this repository under `backend/`). The frontend needs the backend API to function.

---

## Setup

1.  **Clone this repository:**
    ```bash
    git clone https://github.com/NemesisGuy/forkmyfolio-oss.git
    cd forkmyfolio-oss/frontend-admin
    ```

2.  **Install NPM dependencies:**
    This command will download all the required libraries for the project.
    ```bash
    npm install
    ```

3.  **Configure the Backend API URL (for Local Development):**
    By default, the application expects the backend to be running at `http://localhost:8080`. In local development, the `/api/v1` fallback in `src/services/api/apiClient.ts` is forwarded to the backend by the dev proxy in `vite.config.js`. If your backend runs on a different URL or port, adjust the proxy target in `vite.config.js`.

    The application does **not** use `.env` files for this purpose.

4.  **Run the Development Server:**
    ```bash
    npm run dev
    ```
    The application will now be accessible at `http://localhost:5173` (or another port if 5173 is in use).

## Available Scripts

In addition to `dev`, the following scripts are available in `package.json`:

-   `npm run build`: Compiles the application for production.
-   `npm run preview`: Serves the production build locally for testing.
-   `npm run test:unit`: Runs unit tests with Vitest.
-   `npm run test:e2e`: Runs end-to-end tests with Playwright.
-   `npm run lint`: Lints the codebase with ESLint and attempts to fix issues.
-   `npm run format`: Formats the code with Prettier.