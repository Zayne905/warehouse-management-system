package com.warehouse.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehouse.ai.llm.ChatMessage;
import com.warehouse.ai.llm.LlmClient;
import com.warehouse.ai.llm.LlmConfig;
import com.warehouse.ai.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Core AI service — orchestrates LLM calls with Function Calling tool loop.
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final LlmClient llmClient;
    private final LlmConfig llmConfig;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    // Simple in-memory conversation store (sessionId -> message list)
    private final Map<String, List<ChatMessage.Message>> conversations = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY = 40; // keep last 40 messages per session

    public AiService(LlmClient llmClient, LlmConfig llmConfig,
                     ToolRegistry toolRegistry, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.llmConfig = llmConfig;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    /**
     * Non-streaming chat. Returns the full AI response text.
     */
    public String chat(String sessionId, String userMessage) {
        List<ChatMessage.Message> messages = getOrCreateHistory(sessionId);
        messages.add(createUserMessage(userMessage));

        String response = chatWithTools(messages);
        messages.add(createAssistantMessage(response));
        trimHistory(messages);
        return response;
    }

    /**
     * Streaming chat. Calls onChunk for each text fragment.
     * Tool calls are handled internally (not streamed to user).
     */
    public void chatStream(String sessionId, String userMessage, Consumer<String> onChunk) {
        List<ChatMessage.Message> messages = getOrCreateHistory(sessionId);
        messages.add(createUserMessage(userMessage));

        String fullResponse = chatWithToolsStreaming(messages, onChunk);
        messages.add(createAssistantMessage(fullResponse));
        trimHistory(messages);
    }

    /**
     * Clear a conversation session.
     */
    public void clearSession(String sessionId) {
        conversations.remove(sessionId);
    }

    // --- Internal ---

    /**
     * Non-streaming tool loop: send to LLM, if tool_calls come back, execute and retry.
     */
    private String chatWithTools(List<ChatMessage.Message> messages) {
        int maxIterations = 5; // prevent infinite loops

        for (int i = 0; i < maxIterations; i++) {
            ChatMessage.ChatResponse response = llmClient.chat(messages, toolRegistry.getToolDefs());

            if (response.getChoices() == null || response.getChoices().isEmpty()) {
                return "AI 未返回有效响应。";
            }

            ChatMessage.Choice choice = response.getChoices().get(0);
            ChatMessage.Message msg = choice.getMessage();

            // If the model wants to call tools
            if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                // Add the assistant's tool_calls message to history
                messages.add(msg);

                // Execute each tool and add results
                for (ChatMessage.ToolCall tc : msg.getToolCalls()) {
                    String toolName = tc.getFunction().getName();
                    String toolArgs = tc.getFunction().getArguments();
                    log.info("Tool call: {} args={}", toolName, toolArgs);

                    String toolResult = toolRegistry.execute(toolName, toolArgs);

                    messages.add(ChatMessage.Message.builder()
                            .role("tool")
                            .toolCallId(tc.getId())
                            .name(toolName)
                            .content(toolResult)
                            .build());
                }
                // Continue the loop — LLM will process tool results
                continue;
            }

            // Normal text response
            return msg.getContent() != null ? msg.getContent() : "";
        }

        return "AI 处理达到最大轮次限制，请简化您的问题。";
    }

    /**
     * Streaming tool loop. Handles tool calls internally while streaming text to the user.
     */
    private String chatWithToolsStreaming(List<ChatMessage.Message> messages,
                                           Consumer<String> onChunk) {
        int maxIterations = 5;

        for (int i = 0; i < maxIterations; i++) {
            // For streaming with potential tool calls, we use non-streaming internally
            // when tools are involved, and stream only the final text response.
            // This simplifies handling — we detect tool_calls via non-streaming first.
            ChatMessage.ChatResponse response = llmClient.chat(messages, toolRegistry.getToolDefs());

            if (response.getChoices() == null || response.getChoices().isEmpty()) {
                onChunk.accept("AI 未返回有效响应。");
                return "";
            }

            ChatMessage.Choice choice = response.getChoices().get(0);
            ChatMessage.Message msg = choice.getMessage();

            // Tool calls needed — execute and retry (not streamed to user)
            if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                messages.add(msg);

                for (ChatMessage.ToolCall tc : msg.getToolCalls()) {
                    String toolName = tc.getFunction().getName();
                    String toolArgs = tc.getFunction().getArguments();
                    log.info("Tool call (stream mode): {} args={}", toolName, toolArgs);

                    onChunk.accept("\n\n🔍 *正在查询" + toolName + "...*\n\n");

                    String toolResult = toolRegistry.execute(toolName, toolArgs);
                    messages.add(ChatMessage.Message.builder()
                            .role("tool")
                            .toolCallId(tc.getId())
                            .name(toolName)
                            .content(toolResult)
                            .build());
                }
                continue;
            }

            // Final text response — stream it character by character for typing effect
            String content = msg.getContent() != null ? msg.getContent() : "";
            if (!content.isEmpty()) {
                // Stream word by word for a typing effect
                streamText(content, onChunk);
            }
            return content;
        }

        onChunk.accept("AI 处理达到最大轮次限制，请简化您的问题。");
        return "";
    }

    /**
     * Simulate streaming by sending text in small chunks.
     */
    private void streamText(String text, Consumer<String> onChunk) {
        // Split by characters and send in small groups for typing effect
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            buffer.append(text.charAt(i));
            // Flush every 3 chars or at punctuation
            if (buffer.length() >= 3 || i == text.length() - 1
                    || "，。！？\n,.;!?".indexOf(text.charAt(i)) >= 0) {
                onChunk.accept(buffer.toString());
                buffer.setLength(0);
                // Small sleep for typing effect
                try { Thread.sleep(20); } catch (InterruptedException ignored) {}
            }
        }
        if (buffer.length() > 0) {
            onChunk.accept(buffer.toString());
        }
    }

    private List<ChatMessage.Message> getOrCreateHistory(String sessionId) {
        return conversations.computeIfAbsent(sessionId, k -> {
            List<ChatMessage.Message> list = new ArrayList<>();
            // Add system prompt
            list.add(ChatMessage.Message.builder()
                    .role("system")
                    .content(llmConfig.getSystemPrompt())
                    .build());
            return list;
        });
    }

    private ChatMessage.Message createUserMessage(String text) {
        return ChatMessage.Message.builder()
                .role("user")
                .content(text)
                .build();
    }

    private ChatMessage.Message createAssistantMessage(String text) {
        return ChatMessage.Message.builder()
                .role("assistant")
                .content(text)
                .build();
    }

    private void trimHistory(List<ChatMessage.Message> messages) {
        // Keep system prompt + last MAX_HISTORY messages
        if (messages.size() > MAX_HISTORY + 1) {
            ChatMessage.Message system = messages.get(0);
            List<ChatMessage.Message> recent = new ArrayList<>(messages.subList(
                    messages.size() - MAX_HISTORY, messages.size()));
            messages.clear();
            messages.add(system);
            messages.addAll(recent);
        }
    }
}
