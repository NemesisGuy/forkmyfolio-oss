-- OIDC/social login: allow accounts provisioned by the optional Keycloak
-- registration to be persisted. Never edit V1 — migrations are immutable.
ALTER TABLE users
    MODIFY provider ENUM('GITHUB', 'GOOGLE', 'LINKEDIN', 'LOCAL', 'KEYCLOAK') NOT NULL;
