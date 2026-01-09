package Jaksim.jaksim_server.global.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;

@NoArgsConstructor
@Getter
public class GeminiResponse {

    private List<Candidate> candidates;

    @NoArgsConstructor
    @Getter
    public static class Candidate {
        private Content content;
        private String finishReason;
        private int index;
    }

    @NoArgsConstructor
    @Getter
    public static class Content {
        private List<Part> parts;
        private String role;
    }

    @NoArgsConstructor
    @Getter
    public static class Part {
        private String text;
    }

    public Optional<String> firstText() {
        if (candidates == null || candidates.isEmpty()) return Optional.empty();
        var content = candidates.get(0).getContent();
        if (content == null || content.getParts() == null || content.getParts().isEmpty()) return Optional.empty();
        return Optional.ofNullable(content.getParts().get(0).getText());
    }
}
