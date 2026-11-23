import { describe, it, expect } from 'vitest';
import { sortExperiences, isAutomaticExperience, type OrderableExperience } from './experienceOrdering';

/**
 * Phase 0.29 (TASKS §G): the client mirror must behave EXACTLY like
 * backend ExperienceOrdering.java — admin list, public page and exports
 * all read from this contract on the client side.
 */
describe('experienceOrdering (backend mirror)', () => {
  const exp = (
    jobTitle: string,
    displayOrder: number | null,
    startDate: string | null,
    endDate: string | null = null,
  ): OrderableExperience => ({
    uuid: jobTitle,
    jobTitle,
    companyName: 'Ordering Corp',
    displayOrder,
    startDate,
    endDate,
  });

  const titles = (list: OrderableExperience[]) => list.map((e) => e.jobTitle);

  it('keeps manual pins in slot order regardless of dates', () => {
    const sorted = sortExperiences([
      exp('Pin New', 20, '2024-01-01'),
      exp('Pin Old', 10, '2020-01-01', '2021-01-01'),
    ]);
    expect(titles(sorted)).toEqual(['Pin Old', 'Pin New']);
  });

  it('places position-0 entries chronologically between manual pins', () => {
    const sorted = sortExperiences([
      exp('Manual Past', 20, '2020-01-01', '2021-01-31'),
      exp('Auto Mid', 0, '2022-06-01', '2022-12-31'),
      exp('Manual Now', 10, '2024-01-01'),
      exp('Auto Fresh', 0, '2025-03-01'),
    ]);
    // Same expectation as ExperienceOrderingIntegrationTest.
    expect(titles(sorted)).toEqual(['Auto Fresh', 'Manual Now', 'Auto Mid', 'Manual Past']);
  });

  it('treats null displayOrder as automatic', () => {
    expect(isAutomaticExperience(exp('A', null, '2024-01-01'))).toBe(true);
    expect(isAutomaticExperience(exp('B', 0, '2024-01-01'))).toBe(true);
    expect(isAutomaticExperience(exp('C', 5, '2024-01-01'))).toBe(false);
  });

  it('sorts a current job (null end date) ahead of an ended one with the same start', () => {
    const sorted = sortExperiences([
      exp('Ended', 0, '2022-01-01', '2023-06-30'),
      exp('Current', 0, '2022-01-01', null),
    ]);
    expect(titles(sorted)).toEqual(['Current', 'Ended']);
  });

  it('orders overlapping periods deterministically (newest start first)', () => {
    const input = [
      exp('Long Job', 0, '2022-01-01', '2023-06-30'),
      exp('Overlap Job', 0, '2022-05-01', '2022-12-31'),
      exp('Now Job', 0, '2025-01-01'),
    ];
    const expected = ['Now Job', 'Overlap Job', 'Long Job'];
    expect(titles(sortExperiences(input))).toEqual(expected);
    // Re-sorting an already-sorted array must be a no-op (reload safety).
    const once = sortExperiences(input);
    expect(titles(sortExperiences(once))).toEqual(expected);
  });

  it('never mutates the input array', () => {
    const input = [exp('A', 3, '2023-01-01'), exp('B', 1, '2024-01-01')];
    const copy = [...input];
    const sorted = sortExperiences(input);
    expect(input).toEqual(copy);
    expect(sorted).not.toBe(input);
    expect(titles(sorted)).toEqual(['B', 'A']);
  });

  it('returns an empty array for null/undefined/empty input', () => {
    expect(sortExperiences(null)).toEqual([]);
    expect(sortExperiences(undefined)).toEqual([]);
    expect(sortExperiences([])).toEqual([]);
  });
});
