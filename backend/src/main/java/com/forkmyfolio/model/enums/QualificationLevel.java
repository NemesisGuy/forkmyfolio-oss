package com.forkmyfolio.model.enums;

/**
 * Defines the academic level of a qualification.
 */
public enum QualificationLevel {

    HIGH_SCHOOL,
    CERTIFICATE,
    DIPLOMA,
    ASSOCIATE,
    ADVANCED_DIPLOMA,
    BTECH,
    BACHELORS,
    POST_GRADUATE_DIPLOMA,
    HONOURS,
    MASTERS,
    DOCTORATE;

    public boolean isBlank() {
        return this == null || this.name().isBlank();

    }
}