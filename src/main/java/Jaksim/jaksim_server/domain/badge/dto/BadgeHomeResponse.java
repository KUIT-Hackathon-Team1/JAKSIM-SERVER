package Jaksim.jaksim_server.domain.badge.dto;

import java.util.List;

public record BadgeHomeResponse(
        BadgeSummaryResponse summary,
        boolean hasInProgress,
        String newGoalIconKey,
        List<BadgeItemResponse> badges
) {
}
