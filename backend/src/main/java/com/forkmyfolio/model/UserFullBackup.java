package com.forkmyfolio.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A domain-level container that encapsulates all data for a single user for
 * system-wide backup purposes.
 * This is used to decouple the service layer from DTOs (Rule 2: DBR).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFullBackup {
    private User user;
    private PortfolioBackup portfolio;
}
