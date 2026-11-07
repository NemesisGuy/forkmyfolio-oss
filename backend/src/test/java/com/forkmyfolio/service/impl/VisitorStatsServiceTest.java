package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.VisitorStats;
import com.forkmyfolio.model.enums.VisitorStatType;
import com.forkmyfolio.repository.VisitorStatsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitorStatsServiceTest {

    @Mock
    private VisitorStatsRepository visitorStatsRepository;

    @InjectMocks
    private VisitorStatsService visitorStatsService;

    @Test
    void getAllStats_shouldReturnAllStats() {
        // Arrange
        VisitorStats totalVisits = new VisitorStats();
        totalVisits.setType(VisitorStatType.TOTAL_VISITS);
        totalVisits.setCount(100L);

        List<VisitorStats> allStats = Arrays.asList(totalVisits);
        when(visitorStatsRepository.findAll()).thenReturn(allStats);

        // Act
        List<VisitorStats> result = visitorStatsService.getAllStats();

        // Assert
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getCount());
        verify(visitorStatsRepository, times(1)).findAll();
    }

    @Test
    void incrementStat_whenRowExists_shouldIncrementAtomicallyWithoutReadOrSave() {
        // Phase 7.4 (§11): the increment must be a single atomic UPDATE — no
        // read-modify-write (find -> count+1 -> save) that loses concurrent
        // updates.
        when(visitorStatsRepository.incrementCount(any(VisitorStatType.class), anyString()))
                .thenReturn(1); // row existed and was updated

        visitorStatsService.incrementTotalVisits();

        verify(visitorStatsRepository, times(1)).incrementCount(VisitorStatType.TOTAL_VISITS, "site_total");
        verify(visitorStatsRepository, never()).findByTypeAndRefId(any(), anyString());
        verify(visitorStatsRepository, never()).save(any());
    }

    @Test
    void incrementStat_shouldSeedRaceSafeFirstThenIncrementAtomically() {
        // Phase 7.4 (§11): EVERY increment = race-safe INSERT IGNORE seed
        // FIRST (serializes concurrent first-visit seeds on the V10 unique
        // key; update-first ordering deadlocked under MySQL gap locks), then
        // the atomic single-statement UPDATE. Same transaction — no nested
        // REQUIRES_NEW (that would double the per-increment connection demand
        // and starve the Hikari pool under bursts of async stat increments).

        visitorStatsService.incrementSkillsSectionView();

        InOrder order = inOrder(visitorStatsRepository);
        order.verify(visitorStatsRepository).seedRowIfAbsent("SKILLS_SECTION_VIEW", "skills_section");
        order.verify(visitorStatsRepository).incrementCount(VisitorStatType.SKILLS_SECTION_VIEW,
                "skills_section");
        verify(visitorStatsRepository, never()).findByTypeAndRefId(any(), anyString());
        verify(visitorStatsRepository, never()).save(any());
    }
}
