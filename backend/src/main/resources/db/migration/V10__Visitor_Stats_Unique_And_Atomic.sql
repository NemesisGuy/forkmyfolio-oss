-- Phase 7.4 (§11): visitor-stats race hardening.
--
-- Problem: VisitorStatsService incremented counts via read-modify-write
-- (SELECT -> count+1 -> UPDATE), which LOSES increments when two requests
-- increment the same row concurrently, and could INSERT duplicate rows for a
-- brand-new (type, ref_id) pair when the very first two visitors race.
--
-- Fix strategy (all steps additive / no data loss):
--   1) Collapse any historical duplicate rows, keeping the row with the
--      largest count for each (type, ref_id) pair (earliest id on ties).
--   2) Add a unique key on (type, ref_id) so duplicate rows become impossible
--      and the new atomic UPDATE-first increment (see VisitorStatsRepository
--      .incrementCount) can never double-count behind two rows.

-- Step 1: delete duplicates, keeping the highest-count (then lowest-id) row.
DELETE
FROM visitor_stats
WHERE id IN (
    SELECT id
    FROM (
             -- MySQL 8: window function inside a derived table to see all rows.
             SELECT id,
                    ROW_NUMBER() OVER (
                        PARTITION BY type, ref_id
                        ORDER BY `count` DESC, id ASC
                    ) AS rn
             FROM visitor_stats
         ) ranked
    WHERE ranked.rn > 1
);

-- Step 2: enforce one row per (type, ref_id).
ALTER TABLE visitor_stats
    ADD UNIQUE KEY uk_visitor_stats_type_ref (type, ref_id);
