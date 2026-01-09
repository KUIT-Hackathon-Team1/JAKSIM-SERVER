package Jaksim.jaksim_server.domain.progress.dto;

import Jaksim.jaksim_server.domain.goal.model.GoalCategory;
import Jaksim.jaksim_server.domain.progress.model.enums.*;

import java.time.LocalDate;
import java.util.List;

public record RunDetailResponse(
        Long runId, //
        Long goalId,
        String goalTitle,
        String goalIntent,
        GoalCategory category,
        String categoryIconKey,
        LocalDate startDate,
        LocalDate expectedEndDate,
        int currentDayIndex, //
        RunStatus runStatus,
        TierStatus tierStatus, // ✅ 진행중이면 null, 종료면 GOLD/BRONZE/FAIL
        List<DayDto> days
) {
    public record DayDto(
            int dayIndex,
            LocalDate date,
            DayResult result,
            boolean finalized,
            String memo
    ) {}
}
