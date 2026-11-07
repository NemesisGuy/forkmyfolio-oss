/**
 * @file src/utils/skillUtils.js
 * @description Shared utility functions for skills.
 */

/**
 * Determines the Bootstrap badge background color class based on skill level.
 * @param {string} level - The skill level ('BEGINNER', 'INTERMEDIATE', 'EXPERT').
 * @returns {string} The Bootstrap background color class.
 */
export const getSkillBadgeClass = (level) => {
  switch (level?.toUpperCase()) {
    case 'EXPERT':
      return 'bg-primary';
    case 'INTERMEDIATE':
      return 'bg-success';
    case 'BEGINNER':
      return 'bg-secondary';
    default:
      return 'bg-light text-dark';
  }
};
