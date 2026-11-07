import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { nextTick } from 'vue';
import ProjectFormModal from './ProjectFormModal.vue';
import type { Project } from '@/types';

/**
 * Phase 7.4 §6 — key-features editor regression tests (add / edit / remove /
 * reorder / blank-dropping / 12-limit) for the project form modal.
 * The modal is the only management surface where the structured key-features
 * list is edited; the backend rejects blank items with 400, so the modal must
 * never emit them.
 */

vi.mock('bootstrap', () => ({
  Modal: class {
    show = vi.fn();
    hide = vi.fn();
  },
}));

const baseProject: Project = {
  uuid: 'proj-1',
  title: 'My Project',
  description: 'A project',
  skills: [{ skillId: 1, name: 'Vue', category: 'Frontend' } as never],
  keyFeatures: ['Fast', 'Reliable'],
  visible: true,
  displayOrder: 1,
};

type ModalWrapper = ReturnType<typeof mountModal>;

const mountModal = () => mount(ProjectFormModal, {
  props: {
    project: null,
    userSkills: ['Vue', 'Java'],
  },
});

const openForEdit = async (wrapper: ModalWrapper, project: Project | null) => {
  // The props.project watcher only fires on change, so mount with null first.
  await wrapper.setProps({ project });
};

const featureRows = (wrapper: ModalWrapper) =>
  wrapper.findAll('.d-flex.align-items-center.gap-1');

const featureInputs = (wrapper: ModalWrapper) =>
  featureRows(wrapper).map((row) => row.find('input').element as HTMLInputElement);

const inputValues = (wrapper: ModalWrapper) =>
  featureInputs(wrapper).map((input) => input.value);

const saveButton = (wrapper: ModalWrapper) =>
  wrapper.findAll('.modal-footer button').find((b) => b.text().includes('Save Changes'));

describe('ProjectFormModal key features (Phase 7.4 §6)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('seeds the ordered key-features list and shows rows in order when editing', async () => {
    const wrapper = mountModal();
    await openForEdit(wrapper, baseProject);
    expect(inputValues(wrapper)).toEqual(['Fast', 'Reliable']);
  });

  it('shows the empty-state hint for projects without key features', async () => {
    const wrapper = mountModal();
    await openForEdit(wrapper, { ...baseProject, keyFeatures: [] });
    expect(wrapper.text()).toContain('No key features yet');
  });

  it('reorders via the move buttons and emits the reordered list on save', async () => {
    const wrapper = mountModal();
    await openForEdit(wrapper, baseProject);
    const moveUp = featureRows(wrapper)[1].findAll('button')
        .find((b) => b.attributes('title') === 'Move up');
    await moveUp!.trigger('click');
    await nextTick();
    expect(inputValues(wrapper), 'after move-up click').toEqual(['Reliable', 'Fast']);

    // After the swap the moved feature is first -> its Move-up button is disabled
    const moveUpAfter = featureRows(wrapper)[0].findAll('button')
        .find((b) => b.attributes('title') === 'Move up');
    expect((moveUpAfter!.element as HTMLButtonElement).disabled).toBe(true);

    await saveButton(wrapper)!.trigger('click');
    expect(wrapper.emitted('save')![0][0].keyFeatures).toEqual(['Reliable', 'Fast']);
  });

  it('removes a feature and emits the remaining list on save', async () => {    const wrapper = mountModal();
    await openForEdit(wrapper, baseProject);
    const removeFirst = featureRows(wrapper)[0].findAll('button')
        .find((b) => b.attributes('title') === 'Remove');
    await removeFirst!.trigger('click');
    expect(inputValues(wrapper)).toEqual(['Reliable']);

    await saveButton(wrapper)!.trigger('click');
    expect(wrapper.emitted('save')![0][0].keyFeatures).toEqual(['Reliable']);
  });

  it('trims values and drops blank entries on save (API rejects blanks with 400)', async () => {
    const wrapper = mountModal();
    await openForEdit(wrapper, baseProject);
    const rows = featureRows(wrapper);
    await rows[0].find('input').setValue('  Fast  ');
    await rows[1].find('input').setValue('   ');
    await saveButton(wrapper)!.trigger('click');
    expect(wrapper.emitted('save')![0][0].keyFeatures).toEqual(['Fast']);
  });

  it('caps the list at 12 items by disabling the add button', async () => {
    const wrapper = mountModal();
    const twelve = Array.from({ length: 12 }, (_, i) => `F${i + 1}`);
    await openForEdit(wrapper, { ...baseProject, keyFeatures: twelve });
    const addButton = wrapper.findAll('button').find((b) => b.text().includes('Add Key Feature'));
    expect((addButton!.element as HTMLButtonElement).disabled).toBe(true);
  });

  it('adds a new feature via the add button and sends it on save', async () => {
    const wrapper = mountModal();
    await openForEdit(wrapper, { ...baseProject, keyFeatures: [] });
    const addButton = wrapper.findAll('button').find((b) => b.text().includes('Add Key Feature'));
    await addButton!.trigger('click');
    expect(featureInputs(wrapper)).toHaveLength(1);
    await featureRows(wrapper)[0].find('input').setValue('New feature');
    await saveButton(wrapper)!.trigger('click');
    expect(wrapper.emitted('save')![0][0].keyFeatures).toEqual(['New feature']);
  });
});
