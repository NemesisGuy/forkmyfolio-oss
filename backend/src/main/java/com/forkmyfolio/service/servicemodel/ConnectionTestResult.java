package com.forkmyfolio.service.servicemodel;

/**
 * Result of testing connectivity to a configured analytics provider (DEF-1).
 * Service-layer domain record mapped to the API DTO at the controller
 * boundary (NAG-01 Rule 2).
 *
 * @param success        whether the provider accepted the request
 * @param message        human-readable, curated (no secrets, no URLs)
 * @param providerStatus HTTP status the provider returned, when applicable
 */
public record ConnectionTestResult(boolean success, String message, Integer providerStatus) {
}
