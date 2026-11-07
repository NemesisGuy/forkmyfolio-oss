import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import AnnouncementDropdown from '@/components/common/AnnouncementDropdown.vue';

// Mock the API. IMPORTANT: the read-state source of truth is the BACKEND
// (read property of each announcement / announcement_reads table since
// migration V8). The component has no localStorage logic — a previous
// version of this spec asserted a localStorage-based design that no longer
// exists, which is why those assertions were replaced (Phase 7.3).
const baseAnnouncements = () => [
  {
    uuid: 'uuid-1',
    title: 'Test 1',
    message: 'Message 1',
    createdAt: new Date().toISOString(),
    senderName: 'Admin',
    targetRole: 'USER',
    read: false
  },
  {
    uuid: 'uuid-2',
    title: 'Test 2',
    message: 'Message 2',
    createdAt: new Date().toISOString(),
    senderName: 'Admin',
    targetRole: 'USER',
    read: false
  }
];

vi.mock('@/services/api/announcements.api.js', () => ({
  getMyAnnouncements: vi.fn(),
  markAnnouncementAsRead: vi.fn(() => Promise.resolve()),
  markAllAnnouncementsAsRead: vi.fn(() => Promise.resolve())
}));

import {
  getMyAnnouncements,
  markAllAnnouncementsAsRead
} from '@/services/api/announcements.api.js';

describe('AnnouncementDropdown.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getMyAnnouncements).mockResolvedValue(baseAnnouncements());
  });

  it('renders correctly and shows initial unread count', async () => {
    const wrapper = mount(AnnouncementDropdown);
    await flushPromises();

    const badge = wrapper.find('.badge.bg-danger');
    expect(badge.exists()).toBe(true);
    expect(badge.text()).toContain('2');
  });

  it('marks all as read via backend API and clears the unread badge', async () => {
    const wrapper = mount(AnnouncementDropdown);
    await flushPromises();

    expect(wrapper.find('.badge.bg-danger').text()).toContain('2');

    const actions = wrapper.findAll('a');
    const markAllReadBtn = actions.find(b => b.text().includes('Mark all as read'));
    expect(markAllReadBtn).toBeDefined();
    await markAllReadBtn!.trigger('click');
    await flushPromises();

    // The component delegates persistence to the backend
    expect(markAllAnnouncementsAsRead).toHaveBeenCalledTimes(1);

    // UI reflects cleared unread state
    expect(wrapper.find('.badge.bg-danger').exists()).toBe(false);
    expect(wrapper.find('.badge.bg-primary').exists()).toBe(false);

    // Items themselves are shown as read (no unread dots remain)
    expect(wrapper.findAll('.unread-dot').length).toBe(0);
  });

  it('respects backend read state on reload (remount), counting only unread', async () => {
    // uuid-1 is already read server-side; only uuid-2 remains unread
    vi.mocked(getMyAnnouncements).mockResolvedValue(
      baseAnnouncements().map(a =>
        a.uuid === 'uuid-1' ? { ...a, read: true } : a
      )
    );

    const wrapper = mount(AnnouncementDropdown);
    await flushPromises();

    const badge = wrapper.find('.badge.bg-danger');
    expect(badge.exists()).toBe(true);
    expect(badge.text()).toContain('1');
  });
});
