package Jaksim.jaksim_server.domain.suggestion.service;

import Jaksim.jaksim_server.global.ai.GeminiClient;
import Jaksim.jaksim_server.domain.suggestion.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SuggestionService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public SuggestionResponse adjust(AdjustRequest req) {
        String prompt = buildAdjustPrompt(req.goalTitle(), req.goalCategory(), req.direction());
        String text = geminiClient.generate(prompt).firstText()
                .orElseThrow(() -> new IllegalStateException("Gemini 응답이 비었습니다."));
        return parseSuggestionJson(text);
    }

    public SuggestionResponse createNew(NewRequest req) {
        String prompt = buildNewPrompt(req.goalCategory());
        String text = geminiClient.generate(prompt).firstText()
                .orElseThrow(() -> new IllegalStateException("Gemini 응답이 비었습니다."));
        return parseSuggestionJson(text);
    }

    private SuggestionResponse parseSuggestionJson(String raw) {
        try {
            String json = extractJsonObject(raw);
            return objectMapper.readValue(json, SuggestionResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Gemini JSON 파싱 실패: " + raw, e);
        }
    }

    private String extractJsonObject(String s) {
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end < 0 || start >= end) {
            throw new IllegalArgumentException("JSON 객체를 찾지 못했습니다: " + s);
        }
        return s.substring(start, end + 1);
    }

    /*
    private String buildAdjustPrompt(String goalTitle, String goalCategory, Direction direction) {
        String change = (direction == Direction.UP) ? "harder" : "easier";

        return """
            You are a 3-day goal coach. Make the goal %s but keep the intent.
            Output ONLY valid JSON (no markdown, no extra text).

            {"goalTitle":"...","goalCategory":"%s","rule":"...","rationale":"..."}

            Current:
            goalTitle="%s"
            goalCategory="%s"

            Constraints:
            - rule must be measurable for 3 days (e.g., "3 days, 25 minutes/day")
            - goalTitle should be short and action-based
            """.formatted(change, goalCategory, goalTitle, goalCategory);
    }
    */

    private String buildAdjustPrompt(String goalTitle, String goalCategory, Direction direction) {
        String change = (direction == Direction.UP) ? "harder" : "easier";

        return """
            Make this goal %s for 3 days. Return ONLY JSON:
            {"goalTitle":"...","goalCategory":"%s","rule":"3 days, .../day","rationale":"..."}
            goalTitle="%s"
            goalCategory="%s"
            """.formatted(change, goalCategory, goalTitle, goalCategory);
    }

    /*
    private String buildNewPrompt(String goalCategory) {
        return """
            You are a 3-day goal recommender.
            Create ONE goal for this category and return ONLY valid JSON (no markdown, no extra text).

            {"goalTitle":"...","goalCategory":"%s","rule":"3 days, .../day","rationale":"..."}

            Constraints:
            - goalTitle: short, action-based
            - rule: measurable and specifically for 3 days
            """.formatted(goalCategory);
    }
    */
    private String buildNewPrompt(String goalCategory) {
        return """
            Recommend ONE 3-day goal for category "%s".
            Return ONLY JSON: {"goalTitle":"...","goalCategory":"%s","rule":"3 days, .../day","rationale":"..."}
            """.formatted(goalCategory, goalCategory);
    }



}

