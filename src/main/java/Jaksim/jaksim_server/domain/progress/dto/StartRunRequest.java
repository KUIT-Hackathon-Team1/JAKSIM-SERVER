package Jaksim.jaksim_server.domain.progress.dto;

import java.time.LocalDate;

public record StartRunRequest(
        Long goalId,
        LocalDate startDate // null이면 오늘
) {}
