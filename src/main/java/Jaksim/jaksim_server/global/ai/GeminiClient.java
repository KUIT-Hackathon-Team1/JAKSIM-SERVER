package Jaksim.jaksim_server.global.ai;

import Jaksim.jaksim_server.global.ai.dto.GeminiRequest;
import Jaksim.jaksim_server.global.ai.dto.GeminiResponse;
import Jaksim.jaksim_server.global.ai.property.GeminiProperties;
import Jaksim.jaksim_server.global.ai.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final RestClient geminiRestClient;
    private final GeminiProperties props;

    public GeminiResponse generate(String prompt) {
        var request = new GeminiRequest(prompt);

        return geminiRestClient.post()
                .uri(props.api().url())
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    String body = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    throw new RuntimeException("Gemini API error: " + res.getStatusCode() + " body=" + body);
                })
                .body(GeminiResponse.class);
    }
}

