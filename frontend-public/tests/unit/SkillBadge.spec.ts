import { describe, it, expect } from 'vitest';
import { mountSuspended } from '@nuxt/test-utils/runtime';
import SkillBadge from '~/components/SkillBadge.vue';
import type { Skill } from '~/utils/skillsUtils';

/**
 * NOTE (Phase 7.3): SkillBadge is the primary reusable display component on
 * the public skills page. These tests pin the tooltip-derivation contract
 * (description > category fallback > name) and the badge markup. The dynamic
 * bootstrap Tooltip initialisation is a client enhancement — it does not
 * affect the SSR-rendered markup under test here.
 */
describe('components/SkillBadge.vue', () => {
  it('renders the skill name and carries a description tooltip when present', async () => {
    const skill: Skill = {
      name: 'Spring Boot',
      level: 'EXPERT',
      category: 'BACKEND',
      description: 'My daily driver.',
    };

    const wrapper = await mountSuspended(SkillBadge, { props: { skill } });

    expect(wrapper.classes()).toContain('skill-badge');
    expect(wrapper.text()).toContain('Spring Boot');
    expect(wrapper.attributes('title')).toBe('My daily driver.');
  });

  it('falls back to the category label when no description exists', async () => {
    const skill: Skill = { name: 'Redis', level: 'ADVANCED', category: 'DATA_STORES', description: null };

    const wrapper = await mountSuspended(SkillBadge, { props: { skill } });

    expect(wrapper.attributes('title')).toBe('Category: DATA_STORES');
  });

  it('falls back to the skill name itself when nothing else is set', async () => {
    const skill: Skill = { name: 'Git', level: null, category: null, description: null };

    const wrapper = await mountSuspended(SkillBadge, { props: { skill } });

    expect(wrapper.attributes('title')).toBe('Git');
  });
});
