package com.forkmyfolio.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class AdminStatsDto {
    private long totalVisits;
    private Map<String, Long> projects;
    private Map<String, Long> portfolios; // Map of slug to view count
    private long projectsSectionViews;
    private long skillsSectionViews;
    private long experienceSectionViews;
    private long qualificationsSectionViews;
    private long contactMessageSubmissions;
    private long pdfDownloads;
    private long vcardDownloads;
    private long testimonialsSectionViews;
    private long loginSuccesses;
    private long loginFailures;
    private long logoutSuccesses;
}