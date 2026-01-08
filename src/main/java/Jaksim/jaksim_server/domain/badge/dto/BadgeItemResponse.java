package Jaksim.jaksim_server.domain.badge.dto;

import Jaksim.jaksim_server.domain.progress.model.enums.RunStatus;
import Jaksim.jaksim_server.domain.progress.model.enums.TierStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BadgeItemResponse(
        Long runId,
        Long goalId,
        String goalTitle,
        String iconKey,
        RunStatus runStatus,      // IN_PROGRESS / ENDED
        TierStatus tierStatus,    // 진행중이면 null, 종료면 GOLD/BRONZE/FAIL
        LocalDate startDate,
        LocalDate expectedEndDate,
        LocalDateTime endedAt
) {
    public boolean inProgress() {
        return runStatus == RunStatus.IN_PROGRESS;
    }
}
