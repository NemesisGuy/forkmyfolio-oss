# Project Features & Capabilities

This document outlines the key features and technical capabilities of the ForkMyFolio application, broken down by area.

---

## ✨ User-Facing Features (Vue.js Frontend)

This covers all functionality available to public visitors and standard authenticated users.

### Public Portfolio
- **Dynamic Content**: A fully-featured portfolio website where all content (profile, projects, skills, etc.) is fetched dynamically from the backend.
- **Section Visibility**: Portfolio sections can be toggled on or off by the owner via the dashboard.
- **Theming**: Full support for light and dark mode, controlled by the user's system preference or a manual toggle.
- **Dynamic PDF Generation**: Visitors can download a PDF of the portfolio. The template used for this PDF is selectable by the portfolio owner.
- **Responsive Design**: The entire public site is fully responsive and works on all screen sizes.

### User Dashboard (`/manage`)
A secure, authenticated area where users can manage their own portfolio content.
- **Full Content Management (CRUD)**: Users can Create, Read, Update, and Delete all of their portfolio content, including:
    - Public Profile (bio, social links, etc.)
    - Projects (with image uploads)
    - Skills (with proficiency levels)
    - Work Experience
    - Qualifications & Certifications
    - Testimonials
- **Contact Message Inbox**: View and manage messages sent through the public contact form.
- **Display & PDF Settings**: Control the visibility of public portfolio sections and select the PDF resume template.
- **Account Management**: Update account details like name and email.

---

## 👑 Administrative Features (Vue.js Frontend)

This covers all functionality available only to users with the `ADMIN` role, accessible via the `/admin` dashboard.

- **Admin Dashboard**: A central hub providing an overview of system activity.
- **User Management**: Full CRUD capabilities for all users in the system (create, view, edit, delete).
- **System-Wide Settings**: Configure global application settings, including default PDF templates.
- **Site Statistics**: View analytics on page views, engagement events, and authentication events.
- **System-Wide Messages**: View all contact messages submitted across all portfolios.

---

## 📦 Data Management & Portability

Features related to backing up, restoring, and managing portfolio data.

### For Standard Users
- **Personal Backup**: Users can download a full backup of their personal portfolio data as a single JSON file.
- **Personal Restore**: Users can restore their portfolio from a JSON backup file. This action overwrites their existing portfolio data.

### For Administrators
- **System Backup**: Admins can download a single JSON file containing all data for **all users** in the system.
- **System Restore**: Admins can restore the entire system from a system backup file. This is a destructive action that **wipes all current data**.
- **Single User Restore**: Admins can restore a single user's portfolio using a standard user backup file, without affecting other users.
- **System Wipe**: A destructive, irreversible feature to delete all users, portfolios, and data from the entire system.

---

## ⚙️ Core Backend Features (Java/Spring Boot)

This covers the key architectural features of the backend API.

- **Modern Tech Stack**: Built with Java 21, Spring Boot 3, and a Vue 3 frontend.
- **RESTful API**: A clean, well-structured API serves all data to the frontend via a versioned endpoint (`/api/v1/`).
- **JPA/Hibernate ORM**: Manages all database interactions with MySQL.
- **Dockerized for Deployment**: The entire stack is containerized and orchestrated with Docker Compose for simple, one-command deployment.
- **JWT-Based Security**:
    - **Authentication**: Secures the application using stateless JSON Web Tokens.
    - **Secure Refresh Tokens**: Implements `HttpOnly` cookies for refresh tokens to mitigate XSS risks.
    - **Role-Based Access Control (RBAC)**: Clear distinction between public endpoints and protected user/admin routes.
    - **Password Encryption**: Passwords are securely hashed using `BCryptPasswordEncoder`.
- **Visitor Analytics & Tracking**: A non-intrusive system for tracking page views, engagement, and authentication events, with logic to isolate admin activity from public metrics.
