# Local Development Guide

Follow these instructions to set up the project for local development. This setup is ideal for working on Vue components and other frontend-specific features.

## Prerequisites

- **Node.js**: version 22+ is recommended.
- **A running instance of the [ForkMyFolio Backend](https://github.com/NemesisGuy/ForkMyFolio-backend)**. The frontend needs the backend API to function.

---

## Setup

1.  **Clone this repository:**
    ```bash
    git clone https://github.com/NemesisGuy/ForkMyFolio-frontend.git
    cd ForkMyFolio-frontend
    ```

2.  **Install NPM dependencies:**
    This command will download all the required libraries for the project.
    ```bash
    npm install
    ```

3.  **Configure the Backend API URL (for Local Development):**
    By default, the application expects the backend to be running at `http://localhost:8080`. If your backend is on a different URL, you must manually change the fallback value in `src/services/api/apiClient.js`.

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