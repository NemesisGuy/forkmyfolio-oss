# Changelog

All notable changes to this project will be documented in this file.

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
