import { describe, it, expect } from 'vitest';
import { sortExperiences, isAutomaticExperience, type OrderableExperience } from '../../app/utils/experienceOrdering';

/**
 * Phase 0.29 (TASKS §G): the Nuxt mirror of backend ExperienceOrdering.java.
 * The public page must render the same order as the admin list and exports.
 */
describe('experienceOrdering (backend mirror, public site)', () => {
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
    expect(titles(sorted)).toEqual(['Auto Fresh', 'Manual Now', 'Auto Mid', 'Manual Past']);
  });

  it('treats null displayOrder as automatic', () => {
    expect(isAutomaticExperience(exp('A', null, '2024-01-01'))).toBe(true);
    expect(isAutomaticExperience(exp('B', 0, '2024-01-01'))).toBe(true);
    expect(isAutomaticExperience(exp('C', 5, '2024-01-01'))).toBe(false);
  });

  it('orders overlapping periods deterministically across reads', () => {
    const input = [
      exp('Long Job', 0, '2022-01-01', '2023-06-30'),
      exp('Overlap Job', 0, '2022-05-01', '2022-12-31'),
      exp('Now Job', 0, '2025-01-01'),
    ];
    expect(titles(sortExperiences(input))).toEqual(['Now Job', 'Overlap Job', 'Long Job']);
    expect(titles(sortExperiences([...input].reverse()))).toEqual(['Now Job', 'Overlap Job', 'Long Job']);
  });

  it('never mutates the input array and tolerates empty input', () => {
    const input = [exp('A', 3, '2023-01-01'), exp('B', 1, '2024-01-01')];
    const copy = [...input];
    sortExperiences(input);
    expect(input).toEqual(copy);
    expect(sortExperiences(null)).toEqual([]);
    expect(sortExperiences([])).toEqual([]);
  });
});
