package com.warehouse.ai;

import com.warehouse.model.dto.Result;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * Streaming AI chat via SSE (Server-Sent Events).
     * The frontend connects to this endpoint for real-time typing effect.
     */
    @PostMapping("/chat")
    public SseEmitter chat(@RequestBody Map<String, String> body) {
        String sessionId = body.getOrDefault("sessionId", "default");
        String message = body.getOrDefault("message", "");
        if (message.isBlank()) {
            throw new RuntimeException("消息内容不能为空");
        }

        SseEmitter emitter = new SseEmitter(120_000L); // 2 min timeout

        new Thread(() -> {
            try {
                aiService.chatStream(sessionId, message, chunk -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("chunk")
                                .data(chunk));
                    } catch (IOException e) {
                        // Client disconnected
                    }
                });
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("AI 服务异常: " + e.getMessage()));
                    emitter.completeWithError(e);
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        }).start();

        return emitter;
    }

    /**
     * Non-streaming AI chat. Returns the full response at once.
     */
    @PostMapping("/chat/send")
    public Result<Map<String, Object>> chatSend(@RequestBody Map<String, String> body) {
        String sessionId = body.getOrDefault("sessionId", "default");
        String message = body.getOrDefault("message", "");
        if (message.isBlank()) {
            return Result.error(400, "消息内容不能为空");
        }

        String response = aiService.chat(sessionId, message);
        return Result.ok(Map.of("reply", response, "sessionId", sessionId));
    }

    /**
     * Clear conversation history for a session.
     */
    @PostMapping("/session/clear")
    public Result<String> clearSession(@RequestBody Map<String, String> body) {
        String sessionId = body.getOrDefault("sessionId", "default");
        aiService.clearSession(sessionId);
        return Result.ok("会话已清除");
    }
}
