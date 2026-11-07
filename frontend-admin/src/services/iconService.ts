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
export const getIconClass = (skill: Partial<Skill>): string => {
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
