package Jaksim.jaksim_server.domain.progress.dto;

import Jaksim.jaksim_server.domain.progress.model.enums.DayResult;
import com.fasterxml.jackson.annotation.JsonAlias;

public record UpdateDayRequest(
        DayResult result,
        String memo,
        @JsonAlias({"finalizeDay","finalize_day","finalize"})
        Boolean finalizeDay
) {
    public boolean wantsFinalize() { return Boolean.TRUE.equals(finalizeDay); }
}
