package com.forkmyfolio.model.enums;

/**
 * How an individual portfolio owner sources their analytics configuration.
 */
public enum AnalyticsOwnerMode {
    /** Use the platform-wide configuration (master website). */
    INHERIT,
    /** Use this owner's own provider configuration. */
    OWN,
    /** Analytics disabled for this owner's portfolio. */
    DISABLED
}
