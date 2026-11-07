package com.forkmyfolio.repository;

import com.forkmyfolio.model.VisitorStats;
import com.forkmyfolio.model.enums.VisitorStatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VisitorStatsRepository extends JpaRepository<VisitorStats, Long> {
    Optional<VisitorStats> findByTypeAndRefId(VisitorStatType type, String refId);

    /**
     * Phase 7.4 (§11): atomic increment for a stat row. The old
     * read-modify-write path (find -> count+1 -> save) lost updates when two
     * requests incremented concurrently. This single-statement UPDATE is
     * atomic at the DB level; returns the number of affected rows (0 = the
     * row does not exist yet).
     *
     * @param type  the stat type.
     * @param refId the stat reference id.
     * @return number of updated rows (0 or 1).
     */
    @Modifying
    @Query("UPDATE VisitorStats s SET s.count = s.count + 1, s.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE s.type = :type AND s.refId = :refId")
    int incrementCount(@Param("type") VisitorStatType type, @Param("refId") String refId);

    /**
     * Phase 7.4 (§11): race-safe seed of the initial zero-count row.
     * {@code INSERT IGNORE} relies on the V10 unique key
     * ({@code uk_visitor_stats_type_ref}) so exactly ONE concurrent request
     * wins and the others no-op — no duplicate rows, no exception on a lost
     * race. Runs in the SAME transaction/connection as the increment (never
     * opened a nested REQUIRES_NEW: with the default 10-connection Hikari
     * pool, bursts of async stat increments would otherwise starve the pool).
     *
     * @param type  the stat type name (native query — pass enum.name()).
     * @param refId the stat reference id.
     * @return 1 when this request created the row, 0 when it already existed
     *         (or a concurrent request won the race).
     */
    @Modifying
    @Query(value = "INSERT INTO visitor_stats (type, ref_id, `count`, updated_at) VALUES (:type, :refId, 0, NOW(6)) "
            + "ON DUPLICATE KEY UPDATE ref_id = ref_id", nativeQuery = true)
    int seedRowIfAbsent(@Param("type") String type, @Param("refId") String refId);
}
