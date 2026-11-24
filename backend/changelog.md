# Changelog

All notable changes to this project will be documented in this file.

## [1.2.10] - 2026-11-24

### 🐛 Bug Fixes

- **Portfolio visibility is no longer silently changed by profile saves.**
  Saving the public profile or cover letter used to silently **unpublish** the
  portfolio: the profile-update payload declared `visible`/`publiclyVisible` as
  primitive booleans, so any partial save that omitted those keys (which every
  profile/cover-letter save did) wrote `is_public = 0 AND visible = 0` while
  still returning HTTP 200. The visibility fields have been **removed from the
  profile-save endpoint entirely** — a profile save can no longer touch
  visibility under any circumstances. Visibility now has exactly two writers:
  `PUT /api/v1/me/profile/visibility` and `PUT /api/v1/me/profile/indexing`.
  Requests that still send the old keys are safely ignored.
- **Contact-message updates no longer fail with HTTP 500.** Every
  `PUT /api/v1/me/contact-messages/{uuid}` (mark-as-read, mark-as-replied,
  priority, archive) returned 500 with a `LazyInitializationException`: the
  handler had no `@Transactional`, so with `spring.jpa.open-in-view=false` the
  session closed before the response DTO was mapped and the lazy-loaded message
  owner could not be read. The transaction boundary now covers response
  mapping, matching the read endpoints.
- **Admin contact-message list no longer fails with HTTP 500.**
  `GET /api/v1/admin/contact-messages` had the identical lazy-mapping defect
  and is fixed the same way.
- **Display Settings no longer shows a false "data mismatch" error.** Saving
  with no effective change (e.g. toggling a section off then back on) now
  reports success instead of "Could not save settings due to a data mismatch".
  Genuine mismatches (a changed setting missing from the server) still error.
- **Visibility copy clarified** on Display Settings: the master "Portfolio is
  Public" and search-indexing switches are documented as saving automatically.

### ✅ Testing

- 10 new full-context integration regression tests through real HTTP against
  real MySQL, deliberately run **without** an ambient test transaction so they
  reproduce (and therefore pin) the exact production conditions of both bugs:
  `ProfileVisibilityPartialUpdateIntegrationTest` (4) and
  `ContactMessageUpdateIntegrationTest` (6).
- Frontend regression specs for the visibility switch contract and the
  no-op save path (admin suite now 132 tests; public suite 69).
- Backend suite: **409/409** green.

## [1.2.9] - 2026-11-23

### ✨ Features

- **Skills catalogue review + business/operations skills**: expanded seeded
  skills catalogue, owner-focused business/operations skill entries, and the
  matching loader/test coverage.

### 🐛 Bug Fixes

- **Experience ordering**: manual experience positions now persist reliably
  (an immutable-set merge failure previously rolled back entire saves of
  skill-less entries), position `0` gains automatic chronological placement,
  and ordering is now consistent across admin, public pages, API and all PDF/
  Markdown exports.

### 📚 Documentation

- Full-system feature list (`docs/FEATURES.md`) and architecture diagrams
  (`docs/DIAGRAMS.md`).

Earlier releases (v1.0.0 – v1.2.8) are documented by their git tags.

## [Unreleased] - 2026-01-14

### 🎨 UI & UX Improvements

- **Navbar Layout Correction**: Resolved a large gap in the navbar by removing an incorrect `min-width` restriction on the `AnnouncementDropdown` component.
- **Theme-Adaptive Styling**: Standardized all public pages (Home, Skills, Experience, etc.) to use semantic glassmorphism classes (`glass-title`, `glass-description`), ensuring perfect contrast and visibility in both light and dark modes.
- **Registration Flow Stability**: Refactored the Terms & Conditions modal to ensure reliable policy loading and prevent navigation loops during onboarding.

### 🐛 Bug Fixes

- **Private Profile Graceful Handling**: Fixed a `TypeError` when guests view private portfolios by implementing optional chaining throughout the `HomePage.vue`.
- **Database Schema Compliance**: Resolved a 500 error in visitor stats by expanding the `type` column length to accommodate longer event names.
- **Owner Access to Private Portfolios**: Updated backend and frontend logic to allow authenticated owners to view their own private content while maintaining privacy for visitors.

## [Unreleased] - 2025-12-18

### 🧪 Testing & Quality

- **Test Suite Stabilization**: Successfully stabilized the entire backend test suite, resulting in 94/94 passing tests.
- **Enhanced Service Testing**: Fixed multiple `NullPointerException`s in `RestoreServiceImplTest` by ensuring nested collections in DTOs are correctly initialized and all required mappers are mocked.
- **Controller Serialization Reliability**: Resolved Jackson serialization errors for `LocalDate` and `Unpaged` objects in `AdminControllerTest` and `ExperienceManagementControllerTest` by explicitly configuring `JavaTimeModule` and using `PageRequest`.
- **Standardized API Response Testing**: Updated test assertions to correctly handle the `ApiResponseWrapper` structure, ensuring thorough validation of the unified JSON response format.
- **Graceful Exception Assertion**: Improved `BackupControllerTest` to verify that invalid file type uploads are handled by the `GlobalExceptionHandler`, returning a consistent `400 Bad Request` instead of allowing `ServletException` to propagate.

### 🚀 Infrastructure & Reliability

- **Cross-Platform Flyway Support**: Disabled Flyway checksum validation to allow seamless deployment between Windows development environments and Linux Docker containers, bypassing line-ending mismatches.
- **Idempotent Database Migrations**: Implemented a robust, idempotent migration strategy for MySQL 8.0 using stored procedures. New columns are now added safely only if they do not already exist.
- **Automatic Schema Repair**: Added a `FlywayMigrationStrategy` bean to automatically run `flyway.repair()` on application startup, clearing failed migration states and ensuring system stability.
- **Production Configuration Optimization**: Updated `application.properties` to set `hibernate.ddl-auto=none`, delegating all schema management to Flyway for better production reliability.

### 🐛 Bug Fixes

- **Explicit Password Endpoint Mapping**: Resolved a `NoResourceFoundException` on the password change endpoint by making Controller mappings more explicit.
- **Global Exception Handling**: Enhanced the global exception handler to gracefully handle `NoResourceFoundException`, returning `404 Not Found` instead of a `500 Internal Server Error`.

### ✨ Features

- **Full CRUD for Qualifications**: Implemented a complete set of endpoints for users to manage their academic and professional qualifications.

  - Enhanced the `Qualification` entity with detailed fields like `fieldOfStudy`, `level`, `credentialUrl`, and `institutionLogoUrl`.
  - Created a full vertical slice including a controller, service, repository, mapper, and DTOs, all adhering to the project's clean architecture rules.

- **OAuth2 Authentication**: Implemented a complete, stateless OAuth2 login flow.

  - Users can now register and authenticate using external providers (e.g., Google).
  - Added `HttpCookieOAuth2AuthorizationRequestRepository` to manage the authorization flow without using HTTP sessions, preserving the stateless nature of the API.
  - Created `CustomOAuth2UserService` to process user information from providers, linking them to or creating new `User` accounts.
  - Implemented `OAuth2AuthenticationSuccessHandler` to generate JWT and refresh tokens upon successful login, seamlessly integrating with the existing security model.
  - Implemented `OAuth2AuthenticationFailureHandler` for graceful error handling and redirection.

- **Custom Date Range Validation**: Added a reusable custom validator for date ranges.

  - Created the `@ValidDateRange` annotation to ensure an end date is not before a start date.
  - Implemented `DateRangeValidator` to provide the validation logic.
  - Applied this validation to the `CreateExperienceRequest` and `UpdateExperienceRequest` DTOs to enforce data integrity at the API boundary.

- **Enhanced Data Model Relationships**: Improved data model integrity by establishing clear relationships between entities.
  - Linked `Project` and `Experience` entities to `Skill` via a many-to-many relationship, replacing previous plain-text fields.
  - Updated `Project` and `Experience` DTOs to accept a set of `skillUuids`, allowing for robust relationship management through the API.

### ♻️ Refactoring

- **Architectural Alignment for User Management**: Refactored the admin user management endpoints to strictly adhere to the project's clean architecture principles.

  - All business logic for creating and updating users is now fully encapsulated within the `UserServiceImpl`.
  - The `AdminController` is now a thin layer that delegates directly to the service, passing primitive data from DTOs rather than domain entities.
  - Removed mapping logic from the controller and simplified the `UserMapper`, ensuring a clear separation of concerns.

- **Service Layer Cohesion**: Updated the `UserService` interface to be perfectly in sync with its implementation (`UserServiceImpl`), reinforcing the "program to an interface" principle.

### 🚀 Infrastructure & Configuration

- **Security Configuration**: Updated `SecurityConfig` to integrate and configure the entire OAuth2 login flow, including all custom handlers and services.
- **CORS Configuration**: Improved and centralized CORS configuration within `SecurityConfig` to use the standard `CorsConfigurationSource` bean, enhancing security and maintainability.

### 🗃️ Database

- **Flyway Migration (V8)**: Added a migration to enhance the `qualifications` table with new columns for richer academic data and support for ongoing studies.

- **Flyway Migration (V7)**: Added a new database migration to support OAuth2.
  - Added `provider` (e.g., 'LOCAL', 'GOOGLE') and `provider_id` columns to the `users` table.
  - Modified the `password` column to be nullable to accommodate users who sign up via an external provider and do not have a locally stored password.
- **Flyway Migrations (V4, V5)**: Added migrations for `project_skills` and `experience_skills` join tables to support the new many-to-many relationships.

---
