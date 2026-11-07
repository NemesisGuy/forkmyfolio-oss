package com.forkmyfolio.dto.response;

import com.forkmyfolio.dto.backup.UserBackupDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A DTO that encapsulates all data for a single user for system-wide backup purposes.
 * It combines the user's core information with their complete portfolio data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UserFullBackupDto", description = "A complete backup of a single user and their portfolio.")
public class UserFullBackupDto {

    @Schema(description = "The user's account and profile information.")
    private UserBackupDto user;

    @Schema(description = "The user's complete portfolio data.")
    private PortfolioBackupDto portfolio;
}