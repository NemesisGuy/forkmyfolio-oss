import { describe, it, expect } from 'vitest';
import { groupSkillsByLevel, getIconClass, SKILL_LEVEL_ORDER, type Skill } from '~/utils/skillsUtils';

describe('utils/skillsUtils', () => {
  describe('groupSkillsByLevel', () => {
    it('groups skills by level and sorts Expert -> Beginner', () => {
      const skills: Skill[] = [
        { name: 'Java', level: 'BEGINNER' },
        { name: 'Spring', level: 'EXPERT' },
        { name: 'SQL', level: 'ADVANCED' },
      ];
      const groups = groupSkillsByLevel(skills);

      expect(groups.map(g => g.levelKey)).toEqual(['EXPERT', 'ADVANCED', 'BEGINNER']);
      expect(groups[0].skills.map(s => s.name)).toEqual(['Spring']);
    });

    it('defaults missing levels to INTERMEDIATE; unknown level strings keep their own group but the Intermediate label', () => {
      const groups = groupSkillsByLevel<Skill>([
        { name: 'No Level', level: null },
        { name: 'Weird Level', level: 'PIXIE_DUST' as Skill['level'] },
      ]);

      // null/missing levels fold into the canonical INTERMEDIATE bucket...
      const canonical = groups.find(g => g.levelKey === 'INTERMEDIATE');
      expect(canonical).toBeDefined();
      expect(canonical!.skills.map(s => s.name)).toEqual(['No Level']);
      // ...while an undefined unknown level string forms its own bucket,
      // still DISPLAYED as 'Intermediate' (SKILL_LEVEL_MAP fallback)
      const weird = groups.find(g => g.levelKey === 'PIXIE_DUST');
      expect(weird).toBeDefined();
      expect(weird!.name).toBe('Intermediate');
      expect(weird!.skills.map(s => s.name)).toEqual(['Weird Level']);
      // every emitted skill carries a concrete level
      expect(groups.every(g => g.skills.every(s => !!s.level))).toBe(true);
    });

    it('tolerates null/undefined entries in the input array', () => {
      const groups = groupSkillsByLevel<Skill>([
        null,
        { name: 'Vue', level: 'EXPERT' },
        undefined,
      ]);

      expect(groups).toHaveLength(1);
      expect(groups[0].skills).toHaveLength(1);
      expect(groups[0].skills[0].name).toBe('Vue');
    });

    it('returns [] for empty/null input', () => {
      expect(groupSkillsByLevel([])).toEqual([]);
      expect(groupSkillsByLevel(null as unknown as Skill[])).toEqual([]);
    });

    it('groups share no order ambiguity with the canonical SKILL_LEVEL_ORDER', () => {
      const result = groupSkillsByLevel([
        { name: 'A', level: 'BEGINNER' },
        { name: 'B', level: 'EXPERT' },
        { name: 'C', level: 'INTERMEDIATE' },
        { name: 'D', level: 'ADVANCED' },
      ]).map(g => g.levelKey);

      expect(result).toEqual(SKILL_LEVEL_ORDER);
    });
  });

  describe('getIconClass', () => {
    it('routes known fa-* brand icons (java, docker...) to fa-brands', () => {
      expect(getIconClass({ icon: 'fa-java', level: 'EXPERT' })).toContain('fa-brands fa-java');
    });

    it('routes unknown fa-* icons to fa-solid', () => {
      expect(getIconClass({ icon: 'fa-wrench', level: 'ADVANCED' })).toContain('fa-solid fa-wrench');
    });

    it('passes devicon- and bi- prefixed icons through untouched', () => {
      expect(getIconClass({ icon: 'devicon-plain-redis', level: 'INTERMEDIATE' })).toContain('devicon-plain-redis');
      expect(getIconClass({ icon: 'bi-clipboard-data', level: 'BEGINNER' })).toContain('bi-clipboard-data');
    });

    it('multi-token icon strings pass through untouched', () => {
      expect(getIconClass({ icon: 'fa-brands fa-node-js', level: 'EXPERT' })).toBe('fa-brands fa-node-js text-success');
    });

    it('appends the level color class for known levels', () => {
      expect(getIconClass({ icon: 'fa-java', level: 'EXPERT' })).toContain('text-success');
      expect(getIconClass({ icon: 'fa-java', level: 'BEGINNER' })).toContain('text-warning');
    });

    it('falls back to the DEFAULT (muted) color for unknown levels and no skill', () => {
      expect(getIconClass({ icon: 'fa-code', level: 'PIXIE_DUST' as Skill['level'] })).toContain('text-muted');
      expect(getIconClass(null)).toContain('text-muted');
    });
  });
});
