import type { Skill } from '@/types';

/**
 * @file src/services/iconService.ts
 * @description Centralized logic for determining and rendering icons for skills and categories.
 */

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

/**
 * Phase 0.28 (C1): icon used when a skill carries no catalogue icon.
 * Inline-created skills (added straight from a job experience) used to store
 * a NULL icon, which made `getIconClass` return '' and rendered an EMPTY
 * `<i>` — a badge with no visible icon at all. Every badge now always has
 * something to show.
 */
export const DEFAULT_SKILL_ICON = 'fa-solid fa-tag';

/**
 * Determines the correct CSS class for a skill's icon.
 *
 * Accepts a skill object or a bare name string (defensive: some callers
 * historically passed names), and never returns an empty icon class.
 */
export const getIconClass = (skill: Partial<Skill> | string | null | undefined): string => {
  if (typeof skill === 'string') {
    skill = {name: skill};
  }
  if (!skill) {
    return `${DEFAULT_SKILL_ICON} ${levelColorClasses.DEFAULT}`;
  }

  const level = skill.level;
  const colorClass = level ? (levelColorClasses[level] || levelColorClasses.DEFAULT) : '';
  let baseIconClass: string;

  // Blank/whitespace-only icons count as "no icon" so they fall back too.
  const icon = (skill.icon ?? '').trim();
  if (!icon) {
    baseIconClass = DEFAULT_SKILL_ICON;
  } else if (icon.startsWith('devicon-') || icon.startsWith('bi-') || icon.includes(' ')) {
    baseIconClass = icon;
  } else if (icon.startsWith('fa-')) {
    baseIconClass = faBrandIcons.has(icon) ? `fa-brands ${icon}` : `fa-solid ${icon}`;
  } else {
    baseIconClass = icon;
  }

  return `${baseIconClass} ${colorClass}`.trim();
};

const categoryIcons: Record<string, string> = {
  'Frontend': '',
  'Backend': '',
  'Programming Language': '',
  'Framework': '',
  'DevOps': '',
  'Database': '',
  'Methodology': '',
  'default': ''
};

/**
 * Retrieves the appropriate icon class for a given skill category.
 */
export const getCategoryIcon = (category: string): string => {
  if (!category) return '';
  const key = Object.keys(categoryIcons).find(k => k.toLowerCase() === category.toLowerCase().trim());
  return key ? (categoryIcons[key] || categoryIcons.default) : categoryIcons.default;
};
