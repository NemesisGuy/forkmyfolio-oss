import { authService } from './authService';
import { setAuthService, type AuthService } from './api/apiClient';
import { settingsService } from './settingsService';

// First, inject the authService into the apiClient to resolve the circular dependency.
setAuthService(authService as unknown as AuthService);

/**
 * Orchestrates the application's startup sequence.
 */
await Promise.all([
  settingsService.initialize(), // Fetches global/user settings
  authService.initAuth()        // Attempts to restore a user session
]);
