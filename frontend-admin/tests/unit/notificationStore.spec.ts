import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { notificationService } from '@/stores/notificationStore';

describe('notificationStore', () => {
  beforeEach(() => {
    // Starting clean: the singleton keeps state between tests
    notificationService.notifications.value.forEach(n => notificationService.remove(n.id));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('adds a notification with defaults and auto-dismisses after duration', () => {
    vi.useFakeTimers();
    notificationService.add({ message: 'Hello' });

    expect(notificationService.notifications.value).toHaveLength(1);
    const n = notificationService.notifications.value[0];
    expect(n.message).toBe('Hello');
    expect(n.type).toBe('info');

    vi.advanceTimersByTime(5000);
    expect(notificationService.notifications.value).toHaveLength(0);
  });

  it('honours custom type and duration', () => {
    vi.useFakeTimers();
    notificationService.add({ message: 'Oops', type: 'error', duration: 1000 });

    expect(notificationService.notifications.value[0].type).toBe('error');
    vi.advanceTimersByTime(999);
    expect(notificationService.notifications.value).toHaveLength(1);
    vi.advanceTimersByTime(1);
    expect(notificationService.notifications.value).toHaveLength(0);
  });

  it('remove() deletes only the matching notification by id', () => {
    notificationService.add({ message: 'First' });
    notificationService.add({ message: 'Second' });

    const ids = notificationService.notifications.value.map(n => n.id);
    expect(new Set(ids).size).toBe(2); // ids are unique

    notificationService.remove(ids[0]);
    const remaining = notificationService.notifications.value;
    expect(remaining).toHaveLength(1);
    expect(remaining[0].message).toBe('Second');
  });

  it('remove() with an unknown id is a no-op', () => {
    notificationService.add({ message: 'Keep me' });
    notificationService.remove(123456);
    expect(notificationService.notifications.value).toHaveLength(1);
  });
});
