package com.forkmyfolio.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Domain-level representation of a legal policy (Terms of Service, Privacy
 * Policy).
 * 
 * Adheres to NAG-01 (DBR): Pure domain model.
 */
@Getter
@AllArgsConstructor
public class Policy {
    private final String version;
    private final String title;
    private final String content;
}
