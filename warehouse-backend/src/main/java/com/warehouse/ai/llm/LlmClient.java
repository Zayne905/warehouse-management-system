package com.warehouse.ai.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

/**
 * OpenAI-compatible LLM HTTP client.
 * Supports both streaming (SSE) and non-streaming calls.
 * <p>
 * Uses HttpURLConnection (via SimpleClientHttpRequestFactory) instead of
 * JDK HttpClient to avoid SSL/TLS compatibility issues with some API providers.
 * HttpURLConnection is the most mature and compatible Java HTTP client.
 */
@Component
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final LlmConfig config;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public LlmClient(LlmConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;

        // Use SimpleClientHttpRequestFactory which uses HttpURLConnection under the hood.
        // HttpURLConnection is the most compatible HTTP client — it handles SSL/TLS
        // negotiation reliably with all servers, unlike JDK HttpClient which can fail
        // with "Connection reset" or "received no bytes" on some endpoints.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(30_000);   // 30s connect timeout
        requestFactory.setReadTimeout(120_000);      // 120s read timeout (LLM can be slow)

        this.restClient = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();

        log.info("LlmClient initialized: baseUrl={}, model={}, client=HttpURLConnection",
                config.getBaseUrl(), config.getModel());
    }

    /**
     * Non-streaming chat completion. Returns the full response.
     */
    public ChatMessage.ChatResponse chat(List<ChatMessage.Message> messages,
                                          List<ChatMessage.ToolDef> tools) {
        ChatMessage.ChatRequest request = ChatMessage.ChatRequest.builder()
                .model(config.getModel())
                .messages(messages)
                .tools(tools != null && !tools.isEmpty() ? tools : null)
                .toolChoice(tools != null && !tools.isEmpty() ? "auto" : null)
                .stream(false)
                .maxTokens(config.getMaxTokens())
                .temperature(config.getTemperature())
                .build();

        log.debug("LLM request model={}, tools={}", config.getModel(),
                tools != null ? tools.size() : 0);

        return restClient.post()
                .uri("/chat/completions")
                .body(request)
                .retrieve()
                .body(ChatMessage.ChatResponse.class);
    }

    /**
     * Streaming chat completion. Calls the consumer for each SSE chunk.
     */
    public void chatStream(List<ChatMessage.Message> messages,
                           List<ChatMessage.ToolDef> tools,
                           Consumer<String> onChunk,
                           Consumer<String> onFinishReason) {
        ChatMessage.ChatRequest request = ChatMessage.ChatRequest.builder()
                .model(config.getModel())
                .messages(messages)
                .tools(tools != null && !tools.isEmpty() ? tools : null)
                .toolChoice(tools != null && !tools.isEmpty() ? "auto" : null)
                .stream(true)
                .maxTokens(config.getMaxTokens())
                .temperature(config.getTemperature())
                .build();

        log.debug("LLM stream request model={}", config.getModel());

        restClient.post()
                .uri("/chat/completions")
                .body(request)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange((req, resp) -> {
                    readSseStream(resp, onChunk, onFinishReason);
                    return null;
                });
    }

    private void readSseStream(ClientHttpResponse response,
                               Consumer<String> onChunk,
                               Consumer<String> onFinishReason) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    try {
                        ChatMessage.StreamChunk chunk =
                                objectMapper.readValue(data, ChatMessage.StreamChunk.class);
                        if (chunk.getChoices() != null) {
                            for (ChatMessage.Choice choice : chunk.getChoices()) {
                                if (choice.getDelta() != null) {
                                    if (choice.getDelta().getContent() != null) {
                                        onChunk.accept(choice.getDelta().getContent());
                                    }
                                    if (choice.getDelta().getToolCalls() != null) {
                                        for (ChatMessage.ToolCall tc : choice.getDelta().getToolCalls()) {
                                            if (tc.getFunction() != null
                                                    && tc.getFunction().getName() != null) {
                                                onChunk.accept("[TOOL:" + tc.getFunction().getName() + "]");
                                            }
                                        }
                                    }
                                }
                                if (choice.getFinishReason() != null) {
                                    onFinishReason.accept(choice.getFinishReason());
                                }
                            }
                        }
                    } catch (JsonProcessingException e) {
                        log.debug("Skip unparseable SSE line: {}", data);
                    }
                }
            }
        } catch (Exception e) {
            log.error("SSE stream read error", e);
            onChunk.accept("[错误] AI 服务响应异常: " + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }
}
