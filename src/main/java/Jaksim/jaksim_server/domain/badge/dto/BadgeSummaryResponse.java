package Jaksim.jaksim_server.domain.badge.dto;

public record BadgeSummaryResponse(
        int totalRuns,
        int inProgressRuns,
        int endedRuns,
        int gold,
        int bronze,
        int fail,
        double goldRateOfEnded
) {}
