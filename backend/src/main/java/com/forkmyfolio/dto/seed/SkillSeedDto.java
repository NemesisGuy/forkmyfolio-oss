package com.forkmyfolio.dto.seed;

import com.forkmyfolio.model.enums.SkillLevel;
import lombok.Data;

/**
 * @deprecated This DTO and its corresponding seeder (SkillSeeder) have been replaced by the more efficient
 * SkillDataLoader. This class is no longer used and can be safely removed.
 */
@Data
@Deprecated
public class SkillSeedDto {
    private String name;
    private SkillLevel level;
    private boolean visible;
    private String category;
    private String icon;
    private String description;
}