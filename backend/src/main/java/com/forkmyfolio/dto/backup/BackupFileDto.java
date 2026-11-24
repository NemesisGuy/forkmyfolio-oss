package com.forkmyfolio.dto.backup;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A generic DTO representing the entire structure of a backup file.
 * It contains a metadata block and a generic data payload.
 *
 * @param <T> The type of the data being backed up (e.g., PortfolioBackupDto, List<PortfolioBackupDto>).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackupFileDto<T> {
    private BackupMetaDto meta;
    private T data;

    /**
     * Optional global skills catalogue (system backups only,
     * KNOWN_ISSUES &#167;10). {@code null} in legacy backup files and in
     * per-user backups — a missing catalogue means "nothing to restore",
     * never an error. Omitted from the JSON entirely when null so existing
     * file formats stay byte-identical.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<SkillBackupDto> skills;

    /** Legacy two-field constructor (meta + data) used by per-user backups. */
    public BackupFileDto(BackupMetaDto meta, T data) {
        this.meta = meta;
        this.data = data;
    }
}
