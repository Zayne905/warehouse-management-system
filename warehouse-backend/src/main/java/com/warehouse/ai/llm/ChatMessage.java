package com.warehouse.ai.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible chat message models.
 */
public class ChatMessage {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        private String role;       // system, user, assistant, tool
        private String content;    // text content (null when tool_calls present)
        private String name;       // optional name for tool role
        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;
        @JsonProperty("tool_call_id")
        private String toolCallId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ToolCall {
        private String id;
        private String type = "function";
        private Function function;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Function {
        private String name;
        private String arguments;  // JSON string
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChatRequest {
        private String model;
        private List<Message> messages;
        private List<ToolDef> tools;
        @JsonProperty("tool_choice")
        private Object toolChoice;  // "auto", "none", or a specific choice
        private Boolean stream;
        @JsonProperty("max_tokens")
        private Integer maxTokens;
        private Double temperature;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ToolDef {
        private String type = "function";
        private FunctionDef function;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FunctionDef {
        private String name;
        private String description;
        private Map<String, Object> parameters;  // JSON Schema object
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChatResponse {
        private String id;
        private String object;
        private Long created;
        private String model;
        private List<Choice> choices;
        private Usage usage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Choice {
        private Integer index;
        private Message delta;       // for streaming
        private Message message;     // for non-streaming
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }

    /**
     * SSE streaming chunk.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StreamChunk {
        private String id;
        private String object;
        private Long created;
        private String model;
        private List<Choice> choices;
    }
}
