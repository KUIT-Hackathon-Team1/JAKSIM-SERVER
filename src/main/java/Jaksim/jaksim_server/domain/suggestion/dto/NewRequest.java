package Jaksim.jaksim_server.domain.suggestion.dto;

import jakarta.validation.constraints.NotBlank;

public record NewRequest(
        @NotBlank String goalCategory
) {}