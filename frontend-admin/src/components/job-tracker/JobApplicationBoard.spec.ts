import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import JobApplicationBoard from './JobApplicationBoard.vue';
import type { JobApplication, ApplicationStatus } from '@/types';

/**
 * Phase 0.32 (TASKS §I2): kanban board contract — six stage columns with
 * the 15 statuses grouped correctly, native drag-and-drop emitting
 * `status-change` with the column's default status, same-column drops as
 * no-ops, archived column gated behind showArchived, exact sub-status
 * badges preserved.
 */

const app = (overrides: Partial<JobApplication> & { uuid: string; status: ApplicationStatus }): JobApplication => ({
  jobTitle: 'Developer',
  companyName: 'Acme',
  createdAt: '2026-01-01T00:00:00',
  ...overrides,
} as JobApplication);

const standardList = (): JobApplication[] => [
  app({ uuid: 'w1', status: 'INTERESTED' }),
  app({ uuid: 'w2', status: 'DRAFT' }),
  app({ uuid: 'a1', status: 'APPLIED' }),
  app({ uuid: 'i1', status: 'INTERVIEW_SCHEDULED' }),
  app({ uuid: 'i2', status: 'TECHNICAL_ASSESSMENT' }),
  app({ uuid: 'o1', status: 'OFFER' }),
  app({ uuid: 'c1', status: 'REJECTED' }),
  app({ uuid: 'x1', status: 'ARCHIVED' }),
];

type BoardWrapper = ReturnType<typeof mount>;

const mountBoard = (applications: JobApplication[] = standardList(), showArchived = false): BoardWrapper =>
  mount(JobApplicationBoard, { props: { applications, showArchived } });

const cardsIn = (wrapper: BoardWrapper, colKey: string) =>
  wrapper.find(`[data-testid="column-body-${colKey}"]`).findAll('[data-testid="board-card"]');

const dragStart = (card: ReturnType<BoardWrapper['find']>) =>
  card.trigger('dragstart', { dataTransfer: { setData: vi.fn(), effectAllowed: 'move' } });

describe('JobApplicationBoard.vue (Phase 0.32)', () => {
  it('renders five stage columns by default (archived gated off)', () => {
    const wrapper = mountBoard();
    for (const key of ['wishlist', 'applied', 'interview', 'offer', 'closed']) {
      expect(wrapper.find(`[data-testid="board-column-${key}"]`).exists()).toBe(true);
    }
    expect(wrapper.find('[data-testid="board-column-archived"]').exists()).toBe(false);
  });

  it('shows the archived column only when showArchived is true', () => {
    const wrapper = mountBoard(standardList(), true);
    expect(wrapper.find('[data-testid="board-column-archived"]').exists()).toBe(true);
    expect(cardsIn(wrapper, 'archived')).toHaveLength(1);
  });

  it('groups every status into its stage column with counts', () => {
    const wrapper = mountBoard();
    expect(cardsIn(wrapper, 'wishlist')).toHaveLength(2); // INTERESTED + DRAFT
    expect(cardsIn(wrapper, 'applied')).toHaveLength(1);
    expect(cardsIn(wrapper, 'interview')).toHaveLength(2); // SCHEDULED + ASSESSMENT
    expect(cardsIn(wrapper, 'offer')).toHaveLength(1);
    expect(cardsIn(wrapper, 'closed')).toHaveLength(1);
    expect(wrapper.find('[data-testid="column-count-wishlist"]').text()).toBe('2');
    expect(wrapper.find('[data-testid="column-count-interview"]').text()).toBe('2');
  });

  it('keeps the EXACT sub-status badge even when the column implies a stage', () => {
    const wrapper = mountBoard();
    const labels = cardsIn(wrapper, 'interview').map(c => c.find('[data-testid="board-card-status"]').text());
    expect(labels).toContain('Technical Assessment');
    expect(labels).toContain('Interview Scheduled');
  });

  it('dragging a card to another column emits status-change with that column default', async () => {
    const wrapper = mountBoard();
    await dragStart(cardsIn(wrapper, 'wishlist')[0]); // INTERESTED
    await wrapper.find('[data-testid="board-column-interview"]').trigger('drop');

    const emitted = wrapper.emitted('status-change');
    expect(emitted).toHaveLength(1);
    expect(emitted![0]).toEqual(['w1', 'INTERVIEW_SCHEDULED']);
  });

  it('dropping onto the card own column emits nothing (no-op)', async () => {
    const wrapper = mountBoard();
    await dragStart(cardsIn(wrapper, 'applied')[0]);
    await wrapper.find('[data-testid="board-column-applied"]').trigger('drop');

    expect(wrapper.emitted('status-change')).toBeUndefined();
  });

  it('a dropped offer card targets the Closed column default', async () => {
    const wrapper = mountBoard();
    await dragStart(cardsIn(wrapper, 'offer')[0]);
    await wrapper.find('[data-testid="board-column-closed"]').trigger('drop');

    expect(wrapper.emitted('status-change')![0]).toEqual(['o1', 'REJECTED']);
  });

  it('emits view and edit from the card action buttons', async () => {
    const wrapper = mountBoard();
    await cardsIn(wrapper, 'applied')[0].find('[data-testid="board-view"]').trigger('click');
    await cardsIn(wrapper, 'applied')[0].find('[data-testid="board-edit"]').trigger('click');

    expect(wrapper.emitted('view')![0]).toEqual(['a1']);
    expect(wrapper.emitted('edit')![0]).toEqual(['a1']);
  });

  it('shows an empty-state placeholder in columns with no cards', () => {
    const wrapper = mountBoard([app({ uuid: 'solo', status: 'APPLIED' })]);
    expect(wrapper.find('[data-testid="column-body-wishlist"]').text()).toContain('Nothing here yet');
    expect(cardsIn(wrapper, 'applied')).toHaveLength(1);
  });

  it('renders a drag hint explaining the default-status behaviour', () => {
    const wrapper = mountBoard();
    expect(wrapper.find('[data-testid="board-hint"]').text()).toContain('default status');
  });
});
