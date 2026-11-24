package com.forkmyfolio.dto.backup;

import com.forkmyfolio.model.enums.SkillLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Backup-specific representation of a GLOBAL catalogue skill
 * (KNOWN_ISSUES &#167;10).
 *
 * <p>System backups previously exported only the skills that user data
 * happened to reference. Restoring into an empty database therefore rebuilt a
 * partial catalogue: every unreferenced skill was lost, along with
 * catalogue-owned metadata (category, icon, description, default level) and
 * the admin {@code visible} override — none of which any seed file can
 * recover after the fact.
 *
 * <p>This DTO rides in the optional {@code skills} array of a system backup
 * file. Legacy backup files carry no such field ({@code null}) and restore
 * with the old on-demand behaviour; per-user backups never carry it either.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "SkillBackupDto", description = "Global catalogue skill for system backups (KNOWN_ISSUES §10).")
public class SkillBackupDto {

    @Schema(description = "The skill's unique identifier (preserved across restore).")
    private UUID uuid;

    @Schema(description = "The globally unique skill name.", example = "Java")
    private String name;

    @Schema(description = "The catalogue category the skill belongs to.", example = "Backend Frameworks")
    private String category;

    @Schema(description = "An icon representing the skill.", example = "devicon-java-plain")
    private String icon;

    @Schema(description = "The global description of the skill.")
    private String description;

    @Schema(description = "Admin visibility override for the skill.", example = "true")
    private boolean visible;

    @Schema(description = "Suggested/default proficiency level for the skill.", example = "INTERMEDIATE")
    private SkillLevel level;
}
