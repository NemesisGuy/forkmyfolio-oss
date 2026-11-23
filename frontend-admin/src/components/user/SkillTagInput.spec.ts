import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import SkillTagInput from './SkillTagInput.vue';

/**
 * Phase 0.28 (C1) — job-experience skill picker regression tests.
 *
 * The picker used to receive bare skill NAMES (strings) from the experience
 * page while reading `skill.name` / `skill.uuid` / `skill.icon` off them, so:
 * - every suggestion rendered as an EMPTY chip (no icon, no name),
 * - clicking one called `addSkill(undefined)` (a silent no-op),
 * - typing a query threw inside the filter.
 *
 * The component now normalizes both accepted shapes, so a chip always shows
 * a name and an icon whichever shape the caller passes.
 */

type SkillLike = string | { name: string; icon?: string; uuid?: string };

const mountPicker = (availableSkills: SkillLike[], modelValue: string[] = []) =>
  mount(SkillTagInput, { props: { availableSkills, modelValue } });

const openSuggestions = async (availableSkills: SkillLike[], modelValue: string[] = []) => {
  const wrapper = mountPicker(availableSkills, modelValue);
  await wrapper.find('input').trigger('focus');
  return wrapper;
};

const suggestionChips = (wrapper: ReturnType<typeof mountPicker>) =>
  wrapper.findAll('button.skill-suggestion-tag');

const iconClassesOf = (chips: ReturnType<typeof suggestionChips>) =>
  chips.map((chip) => chip.find('i').classes().join(' '));

describe('SkillTagInput (Phase 0.28 C1)', () => {
  it('renders a NAMED chip for every suggestion when given bare names (no empty chips)', async () => {
    const wrapper = await openSuggestions(['Java', 'Vue', 'Docker']);

    const chips = suggestionChips(wrapper);
    expect(chips).toHaveLength(3);
    expect(chips.map((c) => c.text().trim())).toEqual(['Java', 'Vue', 'Docker']);

    // Every chip carries a visible icon class — never a blank <i>.
    for (const iconClass of iconClassesOf(chips)) {
      expect(iconClass.trim()).not.toBe('');
      expect(iconClass).toContain('fa-');
    }
  });

  it('renders name + catalogue icon when given full skill objects', async () => {
    const wrapper = await openSuggestions([
      { name: 'Java', icon: 'fa-brands fa-java', uuid: 'u1' },
      { name: 'Prompt Engineering', icon: 'fa-solid fa-terminal', uuid: 'u2' },
    ]);

    const chips = suggestionChips(wrapper);
    expect(chips.map((c) => c.text().trim())).toEqual(['Java', 'Prompt Engineering']);
    const icons = iconClassesOf(chips);
    expect(icons[0]).toContain('fa-brands fa-java');
    expect(icons[1]).toContain('fa-solid fa-terminal');
  });

  it('clicking a suggestion emits the skill NAME (never undefined)', async () => {
    const wrapper = await openSuggestions(['Java']);

    await suggestionChips(wrapper)[0].trigger('mousedown');

    expect(wrapper.emitted('update:modelValue')).toHaveLength(1);
    expect(wrapper.emitted('update:modelValue')![0][0]).toEqual(['Java']);
  });

  it('filters on typing without throwing, for both input shapes', async () => {
    for (const available of [['Java', 'Vue'], [{ name: 'Java' }], [{ name: 'Vue' }]]) {
      const wrapper = mountPicker(available as SkillLike[]);
      await wrapper.find('input').trigger('focus');
      // Previously: TypeError (skill.name.toLowerCase on a string) blew up here.
      await wrapper.find('input').setValue('jav');
      await wrapper.find('input').setValue('zzz-no-match');
      expect(wrapper.find('input').element.value).toBe('zzz-no-match');
      wrapper.unmount();
    }
  });

  it('adds the typed free-text name on Enter when nothing matches the catalogue', async () => {
    const wrapper = mountPicker(['Java']);
    const input = wrapper.find('input');
    await input.trigger('focus');
    await input.setValue('Multi-Site Operations Management');
    await input.trigger('keydown', { key: 'Enter' });

    expect(wrapper.emitted('update:modelValue')![0][0]).toEqual([
      'Multi-Site Operations Management',
    ]);
  });

  it('hides skills that are already selected', async () => {
    const wrapper = await openSuggestions(['Java', 'Vue'], ['Java']);
    expect(suggestionChips(wrapper).map((c) => c.text().trim())).toEqual(['Vue']);
  });

  it('shows the selected tag with its name and an icon', () => {
    const wrapper = mountPicker([{ name: 'Java', icon: 'fa-brands fa-java' }], ['Java']);

    const tag = wrapper.find('span.skill-tag');
    expect(tag.text()).toContain('Java');
    expect(tag.find('i').classes().join(' ')).toContain('fa-brands fa-java');
  });

  it('removes a selected tag via its close button', async () => {
    const wrapper = mountPicker(['Java'], ['Java']);
    await wrapper.find('span.skill-tag button.btn-close').trigger('click');

    expect(wrapper.emitted('update:modelValue')![0][0]).toEqual([]);
  });
});
