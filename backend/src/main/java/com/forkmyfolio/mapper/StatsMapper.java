package com.forkmyfolio.mapper;

import com.forkmyfolio.dto.response.AdminStatsDto;
import com.forkmyfolio.model.VisitorStats;
import com.forkmyfolio.model.enums.VisitorStatType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper for aggregating VisitorStats domain entities into AdminStatsDto.
 * Adheres to NAG-01 (DBR): Aggregation logic belongs at the boundary.
 */
@Component
public class StatsMapper {

    public AdminStatsDto toAdminStatsDto(List<VisitorStats> allStats) {
        if (allStats == null) {
            return null;
        }

        Map<VisitorStatType, List<VisitorStats>> groupedStats = allStats.stream()
                .collect(Collectors.groupingBy(VisitorStats::getType));

        AdminStatsDto dto = new AdminStatsDto();
        dto.setTotalVisits(getSumForStatType(groupedStats, VisitorStatType.TOTAL_VISITS));
        dto.setProjectsSectionViews(getSumForStatType(groupedStats, VisitorStatType.PROJECTS_SECTION_VIEW));
        dto.setSkillsSectionViews(getSumForStatType(groupedStats, VisitorStatType.SKILLS_SECTION_VIEW));
        dto.setExperienceSectionViews(getSumForStatType(groupedStats, VisitorStatType.EXPERIENCE_SECTION_VIEW));
        dto.setQualificationsSectionViews(getSumForStatType(groupedStats, VisitorStatType.QUALIFICATIONS_SECTION_VIEW));
        dto.setContactMessageSubmissions(getSumForStatType(groupedStats, VisitorStatType.CONTACT_MESSAGE_SUBMISSION));
        dto.setPdfDownloads(getSumForStatType(groupedStats, VisitorStatType.PDF_DOWNLOAD));
        dto.setVcardDownloads(getSumForStatType(groupedStats, VisitorStatType.VCARD_DOWNLOAD));
        dto.setTestimonialsSectionViews(getSumForStatType(groupedStats, VisitorStatType.TESTIMONIALS_SECTION_VIEW));
        dto.setLoginSuccesses(getSumForStatType(groupedStats, VisitorStatType.LOGIN_SUCCESS));
        dto.setLoginFailures(getSumForStatType(groupedStats, VisitorStatType.LOGIN_FAILURE));
        dto.setLogoutSuccesses(getSumForStatType(groupedStats, VisitorStatType.LOGOUT_SUCCESS));

        Map<String, Long> projectViews = groupedStats.getOrDefault(VisitorStatType.PROJECT_VIEW, List.of()).stream()
                .collect(Collectors.toMap(VisitorStats::getRefId, VisitorStats::getCount));

        Map<String, Long> portfolioViews = groupedStats.getOrDefault(VisitorStatType.PORTFOLIO_VIEW, List.of()).stream()
                .collect(Collectors.toMap(VisitorStats::getRefId, VisitorStats::getCount));

        dto.setProjects(projectViews);
        dto.setPortfolios(portfolioViews);

        return dto;
    }

    private long getSumForStatType(Map<VisitorStatType, List<VisitorStats>> groupedStats, VisitorStatType type) {
        return groupedStats.getOrDefault(type, List.of()).stream()
                .mapToLong(VisitorStats::getCount)
                .sum();
    }
}
