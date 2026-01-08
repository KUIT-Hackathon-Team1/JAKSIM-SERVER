package Jaksim.jaksim_server.domain.progress.dto;

import Jaksim.jaksim_server.domain.progress.model.enums.DayResult;

public record UpdateDayRequest(
        DayResult result, //null가능
        String memo, //null가능
        boolean finalizeDay //null가능
) {}
