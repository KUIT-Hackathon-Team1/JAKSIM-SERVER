package Jaksim.jaksim_server.global.ai.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class GeminiRequest {

    private List<Content> contents;

    public GeminiRequest(String prompt) {
        this.contents = List.of(
                new Content("user", List.of(new Part(prompt)))
        );
    }

    @Getter
    @AllArgsConstructor
    public static class Content {
        private String role; // "user"
        private List<Part> parts;
    }

    @Getter
    @AllArgsConstructor
    public static class Part {
        private String text;
    }
}

