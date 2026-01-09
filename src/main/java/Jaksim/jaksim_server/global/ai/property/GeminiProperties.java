package Jaksim.jaksim_server.global.ai.property;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        Api api
) {
    public record Api(String url) {}
}

