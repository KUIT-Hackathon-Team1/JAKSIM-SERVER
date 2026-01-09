package Jaksim.jaksim_server.domain.goal.client;

import Jaksim.jaksim_server.domain.goal.dto.GeminiRequest;
import Jaksim.jaksim_server.domain.goal.property.GeminiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiClient {
    private final WebClient webClient;
    private final GeminiProperties props;

    public String generate(String prompt) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/gemini-2.5-flash:generateContent")
                        .queryParam("key", props.api().key())
                        .build()
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createBody(prompt))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .doOnNext(body -> log.error("Gemini error body: {}", body))
                                .map(RuntimeException::new)
                )
                .bodyToMono(String.class)
                .block();
    }

    private GeminiRequest createBody(String prompt) {
        return new GeminiRequest(
                List.of(
                        new GeminiRequest.Content(
                                List.of(new GeminiRequest.Part(prompt))
                        )
                )
        );
    }
}
