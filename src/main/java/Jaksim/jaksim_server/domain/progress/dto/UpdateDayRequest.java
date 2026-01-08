package Jaksim.jaksim_server.domain.progress.dto;

import Jaksim.jaksim_server.domain.progress.model.enums.DayResult;

public record UpdateDayRequest(
        DayResult result,
        String memo,
        boolean finalizeDay
) {}
