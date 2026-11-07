package com.forkmyfolio.model.enums;

/**
 * Analytics providers supported by the analytics abstraction.
 * UMAMI is the first implementation; GA4 support is planned as a future
 * adapter.
 */
public enum AnalyticsProviderType {
    NONE,
    UMAMI,
    GA4
}
