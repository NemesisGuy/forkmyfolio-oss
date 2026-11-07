package com.forkmyfolio.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO representing a user-facing setting with its effective value.
 *
 * <p>Primary use: the settings API aggregates each global {@code Setting}
 * definition with the authenticated user's override (if any) into this shape.
 *
 * <p>Phase 10 (Workstream 4): this DTO is also reused as the backup
 * representation of a single user-specific setting override
 * ({@code user_settings} table row). In backup payloads {@code uuid} and
 * {@code description} may be null — only {@code name} and {@code value} are
 * required, since the (user, name) pair is unique and restoring re-creates
 * one row per name. Previously these per-user portfolio display settings
 * (e.g. which sections are visible on the public portfolio) were silently
 * lost across a backup/restore round trip.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UserSettingDto", description = "A setting with its effective value (global default or user override).")
public class UserSettingDto {

    @Schema(description = "The unique identifier of the underlying global setting definition. May be null in backup payloads.", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID uuid;

    @Schema(description = "The unique name of the setting (e.g. 'portfolio.projects.show').", example = "portfolio.projects.show")
    private String name;

    @Schema(description = "The effective value: the user's override when present, otherwise the global default.", example = "false")
    private String value;

    @Schema(description = "Human-readable description of the setting. May be null in backup payloads.")
    private String description;
}
