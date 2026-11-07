/**
 * @file app/utils/skillsUtils.ts
 * @description Skill-related business logic and shared constants.
 * Ported from ForkMyFolio-frontend-vue/src/services/skillsService.ts and iconService.ts.
 * Nuxt auto-imports from `app/utils`, no manual import needed.
 */

export interface Skill {
  name: string;
  level?: string | null;
  category?: string | null;
  description?: string | null;
  icon?: string | null;
  visible?: boolean;
  [key: string]: unknown;
}

/**
 * An array of skill level objects, defining the value and display text.
 * Used for select dropdowns and consistent ordering.
 */
export const SKILL_LEVELS = [
  { value: 'EXPERT', text: 'Expert' },
  { value: 'ADVANCED', text: 'Advanced' },
  { value: 'INTERMEDIATE', text: 'Intermediate' },
  { value: 'BEGINNER', text: 'Beginner' }
];

/**
 * A map of skill level keys to their display names (e.g., 'EXPERT' -> 'Expert').
 */
const SKILL_LEVEL_MAP: Record<string, string> = Object.fromEntries(SKILL_LEVELS.map(l => [l.value, l.text]));

/**
 * An array of skill level keys in order of proficiency. Used for sorting.
 */
export const SKILL_LEVEL_ORDER = SKILL_LEVELS.map(l => l.value);

export interface ProficiencyGroup<T extends Skill> {
  name: string;
  levelKey: string;
  skills: T[];
}

const asSkill = (s: unknown): Skill => (s ?? {}) as Skill;

/**
 * Groups an array of skills by their proficiency level.
 * The final result is sorted by proficiency level (Expert -> Beginner).
 * @param skills - The array of skill objects to group. Each must have a `level`.
 * @returns An array of grouped skills.
 */
export const groupSkillsByLevel = <T extends Skill>(skills: (T | null | undefined)[]): ProficiencyGroup<T>[] => {
  if (!skills || skills.length === 0) {
    return [];
  }

  const grouped = (skills.filter(Boolean) as T[]).reduce((acc, raw) => {
    const skill = asSkill(raw);
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
    group.skills.push({ ...skill, level } as T);
    return acc;
  }, [] as ProficiencyGroup<T>[]);

  // Sort the final groups according to the predefined proficiency order.
  return grouped.sort((a, b) => {
    const orderA = SKILL_LEVEL_ORDER.indexOf(a.levelKey);
    const orderB = SKILL_LEVEL_ORDER.indexOf(b.levelKey);
    return orderA - orderB;
  });
};

// ---------------------------------------------------------------------------
// Icon handling (ported from iconService.ts)
// ---------------------------------------------------------------------------

const faBrandIcons = new Set([
  'fa-java', 'fa-js', 'fa-vuejs', 'fa-react', 'fa-angular', 'fa-node-js',
  'fa-python', 'fa-php', 'fa-html5', 'fa-css3-alt', 'fa-git-alt',
  'fa-github', 'fa-linkedin', 'fa-docker', 'fa-bootstrap'
]);

const levelColorClasses: Record<string, string> = {
  EXPERT: 'text-success',
  ADVANCED: 'text-info',
  INTERMEDIATE: 'text-primary-emphasis',
  BEGINNER: 'text-warning',
  DEFAULT: 'text-muted',
};

const levelDefaultIcons: Record<string, string> = {
  EXPERT: '',
  ADVANCED: '',
  INTERMEDIATE: '',
  BEGINNER: '',
  DEFAULT: '',
};

/**
 * Determines the correct CSS class for a skill's icon.
 */
export const getIconClass = (skill?: Partial<Skill> | null): string => {
  if (!skill) {
    return `${levelDefaultIcons.DEFAULT} ${levelColorClasses.DEFAULT}`;
  }

  const level = skill.level;
  const colorClass = level ? (levelColorClasses[level] || levelColorClasses.DEFAULT) : '';
  let baseIconClass: string;

  if (!skill.icon) {
    baseIconClass = level ? (levelDefaultIcons[level] || levelDefaultIcons.DEFAULT) : levelDefaultIcons.DEFAULT;
  } else {
    if (skill.icon.startsWith('devicon-') || skill.icon.startsWith('bi-') || skill.icon.includes(' ')) {
      baseIconClass = skill.icon;
    }
    else if (skill.icon.startsWith('fa-')) {
      baseIconClass = faBrandIcons.has(skill.icon) ? `fa-brands ${skill.icon}` : `fa-solid ${skill.icon}`;
    }
    else {
      baseIconClass = skill.icon;
    }
  }

  return `${baseIconClass} ${colorClass}`.trim();
};
