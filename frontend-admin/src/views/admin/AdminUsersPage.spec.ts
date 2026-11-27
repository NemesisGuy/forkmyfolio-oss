import { describe, it, expect, vi, beforeEach } from 'vitest';
import { defineComponent, nextTick } from 'vue';
import { mount, flushPromises } from '@vue/test-utils';
import AdminUsersPage from './AdminUsersPage.vue';
import {
  getAdminUsers,
  deleteAdminUser,
  restoreUser,
  hardDeleteUser,
} from '@/services/api/admin.api';
import type { User } from '@/types';

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

vi.mock('@/services/api/admin.api', () => ({
  getAdminUsers: vi.fn(),
  deleteAdminUser: vi.fn(),
  restoreUser: vi.fn(),
  hardDeleteUser: vi.fn(),
}));

/**
 * ConfirmModal relies on Bootstrap's Modal class, so tests use a stub that
 * exposes show()/hide() and renders its title/message and two actions. The
 * distinct modal-id values let us target the soft-delete and hard-delete
 * dialogs independently.
 */
const ConfirmModalStub = defineComponent({
  name: 'ConfirmModalStub',
  props: {
    title: { type: String, default: '' },
    message: { type: String, default: '' },
    confirmText: { type: String, default: 'Confirm' },
    modalId: { type: String, default: '' },
  },
  emits: ['confirm', 'close'],
  setup(_, { expose }) {
    expose({ show: vi.fn(), hide: vi.fn() });
  },
  template: `
    <div class="confirm-stub" :data-modal-id="modalId">
      <span class="confirm-title">{{ title }}</span>
      <span class="confirm-message">{{ message }}</span>
      <button type="button" data-testid="confirm-action" @click="$emit('confirm')">{{ confirmText }}</button>
      <button type="button" data-testid="cancel-action" @click="$emit('close')">Cancel</button>
    </div>
  `,
});

const MessageModalStub = (testId: string) => defineComponent({
  props: {
    message: { type: String, default: '' },
    visible: { type: Boolean, default: false },
  },
  template: `<div data-testid="${testId}" :data-visible="visible">{{ message }}</div>`,
});

const mountPage = async () => {
  const wrapper = mount(AdminUsersPage, {
    global: {
      stubs: {
        LoadingModal: true,
        ErrorModal: MessageModalStub('error-modal'),
        SuccessModal: MessageModalStub('success-modal'),
        ConfirmModal: ConfirmModalStub,
        'router-link': { template: '<a><slot /></a>' },
      },
    },
  });
  await flushPromises();
  return wrapper;
};

const activeUser = {
  id: 1,
  username: 'ada',
  email: 'ada@example.com',
  firstName: 'Ada',
  lastName: 'Lovelace',
  slug: 'ada',
  roles: ['ROLE_USER'],
  active: true,
} as User;

const inactiveUser = {
  id: 2,
  username: 'grace',
  email: 'grace@example.com',
  firstName: 'Grace',
  lastName: 'Hopper',
  slug: 'grace',
  roles: ['ROLE_USER'],
  active: false,
} as User;

const rowFor = (wrapper: ReturnType<typeof mount>, id: number) =>
  wrapper.findAll('tbody tr').find((row) => row.find('td')?.text() === String(id));

const actionGroup = (wrapper: ReturnType<typeof mount>, id: number) =>
  rowFor(wrapper, id)!.find('[aria-label="User Actions"]');

const hardDeleteModal = (wrapper: ReturnType<typeof mount>) =>
  wrapper.find('[data-modal-id="hard-delete-user-modal"]');

/** Buttons that must be enabled once an action finishes (the Deactivate
 * button on an already-inactive row stays disabled by design). */
const actionButtons = (wrapper: ReturnType<typeof mount>) => [
  ...wrapper.findAll('[data-testid="edit-user"]'),
  ...wrapper.findAll('[data-testid="restore-user"]'),
  ...wrapper.findAll('[data-testid="hard-delete-user"]'),
  rowFor(wrapper, 1)!.find('[data-testid="deactivate-user"]'),
];

describe('AdminUsersPage.vue — restore and hard delete', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getAdminUsers).mockResolvedValue({
      content: [activeUser, inactiveUser],
      totalElements: 2,
      totalPages: 1,
    });
    vi.mocked(deleteAdminUser).mockResolvedValue(undefined);
    vi.mocked(restoreUser).mockResolvedValue({ ...inactiveUser, active: true });
    vi.mocked(hardDeleteUser).mockResolvedValue(undefined);
  });

  it('shows inactive users with an Inactive badge and only offers Restore on those rows', async () => {
    const wrapper = await mountPage();

    const activeRow = rowFor(wrapper, 1)!;
    const inactiveRow = rowFor(wrapper, 2)!;

    expect(wrapper.text()).toContain('Grace Hopper');
    expect(inactiveRow.text()).toContain('Inactive');
    expect(activeRow.text()).toContain('Active');
    expect(activeRow.find('[data-testid="restore-user"]').exists()).toBe(false);
    expect(inactiveRow.find('[data-testid="restore-user"]').exists()).toBe(true);
    expect(hardDeleteModal(wrapper).exists()).toBe(true);

    wrapper.unmount();
  });

  it('renders row actions as compact icon buttons in one horizontal group', async () => {
    const wrapper = await mountPage();
    const group = actionGroup(wrapper, 2);

    expect(group.classes()).toContain('btn-group');
    expect(group.classes()).toContain('btn-group-sm');
    expect(group.classes()).toContain('user-actions');

    const actionIds = [
      'view-public-profile',
      'edit-user',
      'restore-user',
      'deactivate-user',
      'hard-delete-user',
    ];
    for (const testId of actionIds) {
      const action = group.find(`[data-testid="${testId}"]`);
      expect(action.exists(), `missing ${testId}`).toBe(true);
      expect(action.classes()).toContain('btn-sm');
      expect(action.classes()).toContain('row-action');
      expect(action.find('svg').exists(), `${testId} should render an icon`).toBe(true);
      expect(action.text().trim()).toBe(''); // no oversized inline labels
      expect(action.attributes('title')).toBeTruthy();
      expect(action.attributes('aria-label')).toBe(action.attributes('title'));
    }
    expect(group.findAll('span').length).toBe(0); // no residual label spans
    expect(group.element.children.length).toBe(actionIds.length);

    wrapper.unmount();
  });

  it('keeps all three lifecycle actions with their original tooltips and visual weights', async () => {
    const wrapper = await mountPage();
    const activeRow = rowFor(wrapper, 1)!;
    const inactiveRow = rowFor(wrapper, 2)!;

    expect(activeRow.find('[data-testid="restore-user"]').exists()).toBe(false);
    expect(inactiveRow.find('[data-testid="restore-user"]').classes()).toContain('btn-outline-success');
    expect(inactiveRow.find('[data-testid="restore-user"]').attributes('title')).toBe('Restore User');
    // Deactivate stays disabled on an already-inactive row.
    expect(inactiveRow.find('[data-testid="deactivate-user"]').attributes('disabled')).toBeDefined();

    expect(activeRow.find('[data-testid="deactivate-user"]').attributes('title')).toBe(
      'Deactivate User (Soft Delete)'
    );
    expect(activeRow.find('[data-testid="deactivate-user"]').classes()).toContain('btn-outline-danger');
    expect(activeRow.find('[data-testid="hard-delete-user"]').attributes('title')).toBe(
      'Permanently Delete User (Hard Delete)'
    );
    expect(activeRow.find('[data-testid="hard-delete-user"]').classes()).toContain('btn-danger');
    expect(actionGroup(wrapper, 1).element.children.length).toBe(4); // no Restore on active rows

    wrapper.unmount();
  });

  it('restores an inactive user, refreshes the list and shows success', async () => {
    const wrapper = await mountPage();

    await rowFor(wrapper, 2)!.find('[data-testid="restore-user"]').trigger('click');
    await flushPromises();

    expect(restoreUser).toHaveBeenCalledTimes(1);
    expect(restoreUser).toHaveBeenCalledWith('2');
    expect(getAdminUsers).toHaveBeenCalledTimes(2);
    expect(wrapper.find('[data-testid="success-modal"]').text()).toContain(
      "User 'Grace Hopper' has been restored."
    );
    expect(wrapper.find('[data-testid="error-modal"]').text()).toBe('');

    wrapper.unmount();
  });

  it('restores without touching the soft-delete or hard-delete endpoints', async () => {
    const wrapper = await mountPage();

    await rowFor(wrapper, 2)!.find('[data-testid="restore-user"]').trigger('click');
    await flushPromises();

    expect(deleteAdminUser).not.toHaveBeenCalled();
    expect(hardDeleteUser).not.toHaveBeenCalled();

    wrapper.unmount();
  });

  it('shows the page error pattern when restore fails', async () => {
    vi.mocked(restoreUser).mockRejectedValue(new Error('Restore denied'));
    const wrapper = await mountPage();

    await rowFor(wrapper, 2)!.find('[data-testid="restore-user"]').trigger('click');
    await flushPromises();

    expect(wrapper.find('[data-testid="error-modal"]').text()).toBe('Restore denied');
    expect(getAdminUsers).toHaveBeenCalledTimes(1); // no refresh after a failed request

    wrapper.unmount();
  });

  it('asks for an explicit PERMANENT confirmation before hard deleting', async () => {
    const wrapper = await mountPage();

    await rowFor(wrapper, 1)!.find('[data-testid="hard-delete-user"]').trigger('click');
    const modal = hardDeleteModal(wrapper);
    expect(modal.find('.confirm-title').text()).toBe('Permanently Delete User');
    const message = modal.find('.confirm-message').text();
    expect(message).toContain('PERMANENT');
    expect(message).toContain('cannot be undone');
    expect(message).toContain('Ada Lovelace');
    expect(message).toBe(
      "PERMANENTLY DELETE user 'Ada Lovelace' (ID: 1)? This is a PERMANENT action and cannot be undone. All data belonging to this user will be removed and cannot be recovered."
    );
    expect(modal.find('[data-testid="confirm-action"]').text()).toBe('Delete Permanently');

    wrapper.unmount();
  });

  it('hard deletes only after confirmation, then refreshes and reports success', async () => {
    const wrapper = await mountPage();

    await rowFor(wrapper, 1)!.find('[data-testid="hard-delete-user"]').trigger('click');
    expect(hardDeleteUser).not.toHaveBeenCalled();

    await hardDeleteModal(wrapper).find('[data-testid="confirm-action"]').trigger('click');
    await flushPromises();

    expect(hardDeleteUser).toHaveBeenCalledTimes(1);
    expect(hardDeleteUser).toHaveBeenCalledWith('1');
    expect(getAdminUsers).toHaveBeenCalledTimes(2);
    expect(wrapper.find('[data-testid="success-modal"]').text()).toContain(
      "User 'Ada Lovelace' has been permanently deleted."
    );

    wrapper.unmount();
  });

  it('does not call the API when hard-delete confirmation is cancelled', async () => {
    const wrapper = await mountPage();

    await rowFor(wrapper, 1)!.find('[data-testid="hard-delete-user"]').trigger('click');
    await hardDeleteModal(wrapper).find('[data-testid="cancel-action"]').trigger('click');
    await flushPromises();

    expect(hardDeleteUser).not.toHaveBeenCalled();
    expect(getAdminUsers).toHaveBeenCalledTimes(1);
    expect(wrapper.find('[data-testid="success-modal"]').text()).toBe('');

    wrapper.unmount();
  });

  it('disables action buttons while a hard delete is in flight', async () => {
    let finishDelete!: () => void;
    vi.mocked(hardDeleteUser).mockImplementation(
      () => new Promise<void>((resolve) => { finishDelete = resolve; })
    );
    const wrapper = await mountPage();

    await rowFor(wrapper, 1)!.find('[data-testid="hard-delete-user"]').trigger('click');
    await hardDeleteModal(wrapper).find('[data-testid="confirm-action"]').trigger('click');
    await nextTick();

    expect(hardDeleteUser).toHaveBeenCalledWith('1');
    const buttons = actionButtons(wrapper);
    expect(buttons.length).toBeGreaterThan(0);
    for (const button of buttons) {
      expect(button.attributes('disabled')).toBeDefined();
    }

    finishDelete();
    await flushPromises();

    for (const button of actionButtons(wrapper)) {
      expect(button.attributes('disabled')).toBeUndefined();
    }

    wrapper.unmount();
  });

  it('disables action buttons while a restore is in flight', async () => {
    let finishRestore!: (user: User) => void;
    vi.mocked(restoreUser).mockImplementation(
      () => new Promise<User>((resolve) => { finishRestore = resolve; })
    );
    const wrapper = await mountPage();

    await rowFor(wrapper, 2)!.find('[data-testid="restore-user"]').trigger('click');
    await nextTick();

    for (const button of actionButtons(wrapper)) {
      expect(button.attributes('disabled')).toBeDefined();
    }

    finishRestore({ ...inactiveUser, active: true });
    await flushPromises();

    for (const button of actionButtons(wrapper)) {
      expect(button.attributes('disabled')).toBeUndefined();
    }

    wrapper.unmount();
  });

  it('keeps the soft-delete action working and visually distinct from hard delete', async () => {
    const wrapper = await mountPage();
    const activeRow = rowFor(wrapper, 1)!;

    const softButton = activeRow.find('[data-testid="deactivate-user"]');
    const hardButton = activeRow.find('[data-testid="hard-delete-user"]');
    expect(softButton.classes()).toContain('btn-outline-danger');
    expect(hardButton.classes()).toContain('btn-danger');
    expect(hardButton.classes()).not.toContain('btn-outline-danger');

    const softModal = wrapper.find('[data-modal-id="deactivate-user-modal"]');
    expect(softModal.find('.confirm-title').text()).toBe('Confirm Deactivation');
    expect(softModal.find('.confirm-message').text()).toContain('restored later');

    await softButton.trigger('click');
    expect(softModal.find('.confirm-message').text()).toBe(
      "Deactivate user 'Ada Lovelace' (ID: 1)? The account will be marked Inactive and can be restored later."
    );
    await softModal.find('[data-testid="confirm-action"]').trigger('click');
    await flushPromises();

    expect(deleteAdminUser).toHaveBeenCalledWith('1');
    expect(hardDeleteUser).not.toHaveBeenCalled();
    expect(wrapper.find('[data-testid="success-modal"]').text()).toContain('deactivated');

    wrapper.unmount();
  });
});
