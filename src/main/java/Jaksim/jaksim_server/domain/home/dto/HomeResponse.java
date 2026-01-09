package Jaksim.jaksim_server.domain.home.dto;

import Jaksim.jaksim_server.domain.badge.dto.BadgeItemResponse;
import Jaksim.jaksim_server.domain.badge.dto.BadgeSummaryResponse;

import java.util.List;

public record HomeResponse(
        BadgeSummaryResponse summary,
        boolean hasInProgress,
        String newGoalIconKey, // 진행중 없을 때 "star", 있으면 null
        List<BadgeItemResponse> badges
) {}
