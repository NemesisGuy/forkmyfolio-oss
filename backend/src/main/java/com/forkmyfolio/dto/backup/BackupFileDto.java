package com.forkmyfolio.dto.backup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}