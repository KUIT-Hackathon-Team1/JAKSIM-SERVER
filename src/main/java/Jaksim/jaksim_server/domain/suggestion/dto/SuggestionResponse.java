package Jaksim.jaksim_server.domain.suggestion.dto;

public record SuggestionResponse(
        String goalTitle,
        String goalCategory,
        String rationale  // 한 줄 사유
) {}
