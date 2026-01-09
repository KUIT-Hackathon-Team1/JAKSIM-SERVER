package Jaksim.jaksim_server.domain.suggestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdjustRequest(
        @NotBlank String goalTitle,
        @NotBlank String goalCategory,
        @NotNull Direction direction
) {}
