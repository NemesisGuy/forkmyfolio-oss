# ForkMyFolio Frontend Architecture

This document outlines the architectural patterns, principles, and key decisions for the ForkMyFolio Vue.js frontend application. Its purpose is to guide development, ensure consistency, and facilitate onboarding for new contributors.

## 1. Guiding Philosophy

The architecture is designed to be:

-   **Scalable:** Easily accommodate new features and portfolio sections without major refactoring.
-   **Maintainable:** Promote clean, decoupled code that is easy to understand, debug, and modify.
-   **Themeable:** Support a consistent and robust light/dark mode experience across the entire application.
-   **Performant:** Minimize redundant API calls and ensure a smooth user experience through efficient state management.

## 2. Core Technologies

-   **Framework:** [Vue 3](https://vuejs.org/) (Composition API)
-   **State Management:** Custom Vue 3 Composable (Singleton Pattern)
-   **Routing:** [Vue Router](https://router.vuejs.org/)
-   **UI/Styling:** [Bootstrap 5](https://getbootstrap.com/) & Custom Glassmorphism Engine

## 3. Directory Structure Overview

The `src` directory is organized to enforce a clear separation of concerns.

-   `assets/`: Global styles (`glass.css`, `common.css`), images, and other static resources.
-   `components/`: Reusable Vue components used across multiple views (e.g., modals, forms, layout elements).
-   `composables/`: Reusable Composition API functions (e.g., `usePortfolioDownloader.js`).
-   `router/`: All routing configuration, including route definitions and navigation guards.
-   `services/`: Business logic, API communication, and interaction with browser storage. This layer is completely decoupled from the UI.
-   `stores/`: Contains the application's custom state management logic. It acts as the "single source of truth" for shared application state.
-   `views/`: Top-level page components, each corresponding to a specific route.

## 4. Architectural Patterns in Depth

### 4.1. State Management: Custom Composable Store

The application employs a lightweight, centralized state management pattern using a custom Vue 3 Composable that implements a singleton pattern. This avoids the need for an external library like Pinia or Vuex.

-   **File:** `src/stores/publicPortfolioStore.js`
-   **Concept:** This store acts as the **Single Source of Truth (SSoT)** for all data related to a publicly viewed portfolio.
-   **Mechanism:**
    -   The core state (e.g., `portfolio`, `isLoading`) is defined as a series of `ref`s *outside* the main composable function. This makes them singletons, meaning the state is created only once and shared across the entire application.
    -   The `usePublicPortfolioStore()` function provides access to this shared state and the actions (`fetchPortfolio`, `clearPortfolio`) that modify it.
    -   When a component calls `usePublicPortfolioStore()`, it receives the same instance of the state and actions.
    -   The router guard triggers the `fetchPortfolio(slug)` action when a user navigates to a portfolio URL, which fetches all data and stores it in the shared state.
-   **Benefit:** This pattern is highly efficient and minimalistic. It leverages Vue's built-in reactivity system to provide centralized state management without adding extra dependencies. It prevents redundant API calls as the user navigates between different sections of the same portfolio.

### 4.2. The Service Layer

All external interactions and complex business logic are encapsulated within services.

-   **Location:** `src/services/`
-   **Key Services:**
    -   `authService.js`: Handles all user authentication logic (login, logout, token management, session restoration).
    -   `settingsService.js`: Manages the visibility of portfolio sections, intelligently loading global or user-specific settings.
    -   `api/`: Contains modules that directly communicate with the backend REST API. Each module (`user.api.js`, `admin.api.js`, `public.api.js`) corresponds to a controller on the backend, ensuring a clean mapping.
-   **Benefit:** This decouples the UI from the implementation details of the backend. If an API endpoint changes, the fix is isolated to the relevant service module, and no UI components need to be touched.

### 4.3. Dynamic & Guarded Routing

The router is the central nervous system of the application.

-   **File:** `src/router/index.js`
-   **Key Features:**
    1.  **Dynamic Routes:** The use of the `/:slug` parameter is fundamental. It allows the application to serve an unlimited number of user portfolios from the same codebase.
    2.  **Navigation Guard (`beforeEach`):** This is the most critical piece of the routing logic. It intercepts every navigation request and performs the following tasks:
        -   Determines if the route is for a public portfolio, the main landing page, or an authenticated management page.
        -   Triggers the appropriate data-fetching actions (`fetchPortfolio`, `initializeSettings`).
        -   Enforces authentication and authorization, redirecting users if they lack the required permissions (e.g., accessing `/manage` routes without being logged in, or an admin route without admin privileges).
        -   Performs **ownership checks** to prevent users from accessing other users' management pages (e.g., `/user-a/manage/dashboard` is inaccessible to `user-b`).

### 4.4. Styling & Theming Engine

A consistent visual identity is maintained through a robust, theme-aware styling system.

-   **File:** `src/assets/glass.css`
-   **Mechanism:**
    -   The system is built on CSS Custom Properties (variables) like `--glass-bg`, `--glass-text`, etc.
    -   Two sets of these variables are defined: one in `:root` for light mode, and another in `[data-bs-theme="dark"]` for dark mode.
    -   The `themeService.js` toggles the `data-bs-theme` attribute on the `<html>` element, which instantly and globally switches the active set of variables.
-   **Best Practice:** **All components MUST use these variables.** Hardcoding colors (e.g., `background-color: #fff;`) is strictly forbidden as it breaks theming. The recent `ContactPage.vue` issue highlighted the importance of this rule. Page-specific overrides are acceptable but must be done within `<style scoped>` and must also respect the `[data-bs-theme]` attribute.

## 5. Guiding Principles & Best Practices

1.  **Smart Stores, Dumb Components:** Components should contain minimal logic. Their primary role is to display data from the store and emit events. All complex logic, state manipulation, and API calls belong in stores and services.
2.  **Embrace Composables:** For shared, reusable logic that isn't state management (e.g., downloading a file), create a composable in `src/composables/`.
3.  **Consistency is Paramount:** Always use the shared components provided in `src/components/common/` (e.g., `SuccessModal`, `ErrorModal`, `LoadingModal`) instead of creating one-off solutions.
4.  **Empty States are a Feature:** Every page that displays a list of user-generated content must have a well-designed empty state. This state should be "smart," providing a helpful call-to-action for the portfolio owner and a polite message for public visitors.
