import { describe, it, expect } from 'vitest';
import { getIconClass, DEFAULT_SKILL_ICON } from './iconService';

/**
 * Phase 0.28 (C1) — skill icon resolution regression tests.
 *
 * A skill with no catalogue icon (an inline-created skill, or any legacy row
 * stored with a NULL icon) used to resolve to an EMPTY class string, which
 * rendered a badge with no icon at all. Every path must now yield a visible
 * icon while keeping the existing routing rules intact.
 */
describe('iconService.getIconClass (Phase 0.28 C1)', () => {
  it('falls back to the default icon when the skill has none', () => {
    const cls = getIconClass({ name: 'ProbeSkillPhase028' });
    expect(cls).toContain(DEFAULT_SKILL_ICON);
    expect(cls.trim()).not.toBe('');
  });

  it('falls back for a blank icon string and for a missing skill', () => {
    expect(getIconClass({ name: 'X', icon: '' })).toContain(DEFAULT_SKILL_ICON);
    expect(getIconClass({ name: 'X', icon: '   ' })).toContain(DEFAULT_SKILL_ICON);
    expect(getIconClass(null)).toContain(DEFAULT_SKILL_ICON);
    expect(getIconClass(undefined)).toContain(DEFAULT_SKILL_ICON);
  });

  it('accepts a bare skill name defensively (legacy callers passed strings)', () => {
    const cls = getIconClass('Java');
    expect(cls).toContain(DEFAULT_SKILL_ICON);
    // A bare name carries no proficiency level, so no level color is appended.
    expect(cls.trim()).toBe(DEFAULT_SKILL_ICON);
  });

  it('keeps routing known brand icons to fa-brands', () => {
    expect(getIconClass({ name: 'Java', icon: 'fa-java', level: 'EXPERT' }))
      .toContain('fa-brands fa-java');
    expect(getIconClass({ name: 'Java', icon: 'fa-java', level: 'EXPERT' }))
      .toContain('text-success');
  });

  it('keeps routing unknown fa-* icons to fa-solid and passes multi-token icons through', () => {
    expect(getIconClass({ name: 'Wrench', icon: 'fa-wrench' })).toContain('fa-solid fa-wrench');
    expect(getIconClass({ name: 'Node', icon: 'fa-brands fa-node-js', level: 'EXPERT' }))
      .toBe('fa-brands fa-node-js text-success');
    expect(getIconClass({ name: 'Redis', icon: 'devicon-plain-redis' }))
      .toContain('devicon-plain-redis');
  });
});
