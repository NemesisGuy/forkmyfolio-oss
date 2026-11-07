/**
 * @file src/services/skillsService.ts
 * @description Service for skill-related business logic and shared constants.
 */

import type { Skill } from '@/types';

/**
 * An array of skill level objects, defining the value and display text.
 * Used for select dropdowns and consistent ordering.
 */
export const SKILL_LEVELS = [
  {value: 'EXPERT', text: 'Expert'},
  {value: 'ADVANCED', text: 'Advanced'},
  {value: 'INTERMEDIATE', text: 'Intermediate'},
  {value: 'BEGINNER', text: 'Beginner'}
];

/**
 * A map of skill level keys to their display names (e.g., 'EXPERT' -> 'Expert').
 */
const SKILL_LEVEL_MAP: Record<string, string> = Object.fromEntries(SKILL_LEVELS.map(l => [l.value, l.text]));

/**
 * An array of skill level keys in order of proficiency. Used for sorting.
 */
export const SKILL_LEVEL_ORDER = SKILL_LEVELS.map(l => l.value);

export interface LevelGroup<T extends Skill> {
  name: string;
  skills: T[];
}

export interface CategoryGroup<T extends Skill> {
  category: string;
  levels: LevelGroup<T>[];
}

/**
 * Groups an array of skills by category, and then by proficiency level within each category.
 * The final result is sorted alphabetically by category.
 *
 * @param skills - The array of skill objects to group. Each object must have a `level` property
 * and may have a `category` property.
 * @returns An array of grouped skills.
 */
export const groupSkills = <T extends Skill>(skills: T[]): CategoryGroup<T>[] => {
  if (!skills || skills.length === 0) {
    return [];
  }

  // Group all skills by their category first.
  const skillsByCategory = skills.reduce((acc, skill) => {
    // Use a default category if none is provided to handle legacy data.
    const category = skill.category || 'General';
    if (!acc[category]) {
      acc[category] = [];
    }
    acc[category].push(skill);
    return acc;
  }, {} as Record<string, T[]>);

  // Now, process each category group.
  const groupedResult = Object.entries(skillsByCategory).map(([category, skillsInCat]) => {
    const levels: Record<string, T[]> = {
      EXPERT: [],
      ADVANCED: [],
      INTERMEDIATE: [],
      BEGINNER: []
    };

    // Sort skills within the category alphabetically before assigning to levels.
    const sortedSkills = [...skillsInCat].sort((a, b) => a.name.localeCompare(b.name));

    sortedSkills.forEach(skill => {
      if (levels[skill.level]) {
        levels[skill.level].push(skill);
      }
    });

    // Format the levels for the current category, filtering out empty ones.
    const categoryLevels = SKILL_LEVEL_ORDER.map(levelKey => ({
      name: SKILL_LEVEL_MAP[levelKey],
      skills: levels[levelKey] || []
    })).filter(level => level.skills.length > 0);

    return {category, levels: categoryLevels};
  });

  // Sort the final categories alphabetically.
  return groupedResult.sort((a, b) => a.category.localeCompare(b.category));
};

export interface ProficiencyGroup<T extends Skill> {
  name: string;
  levelKey: string;
  skills: T[];
}

/**
 * Groups an array of skills by their proficiency level.
 * The final result is sorted by proficiency level (Expert -> Beginner).
 *
 * @param skills - The array of skill objects to group. Each must have a `level`.
 * @returns An array of grouped skills.
 */
export const groupSkillsByLevel = <T extends Skill>(skills: T[]): ProficiencyGroup<T>[] => {
  if (!skills || skills.length === 0) {
    return [];
  }

  const grouped = skills.reduce((acc, skill) => {
    // Ensure every skill has a valid level, defaulting to 'INTERMEDIATE'.
    const level = skill.level || 'INTERMEDIATE';

    // Find the group for the current skill's level.
    let group = acc.find(g => g.levelKey === level);

    // If the group doesn't exist, create it.
    if (!group) {
      group = {
        name: SKILL_LEVEL_MAP[level] || 'Intermediate',
        levelKey: level,
        skills: []
      };
      acc.push(group);
    }

    // Ensure the skill object in the view has a non-null level.
    group.skills.push({...skill, level});
    return acc;
  }, [] as ProficiencyGroup<T>[]);

  // Sort the final groups according to the predefined proficiency order.
  return grouped.sort((a, b) => {
    const orderA = SKILL_LEVEL_ORDER.indexOf(a.levelKey);
    const orderB = SKILL_LEVEL_ORDER.indexOf(b.levelKey);
    return orderA - orderB;
  });
};
