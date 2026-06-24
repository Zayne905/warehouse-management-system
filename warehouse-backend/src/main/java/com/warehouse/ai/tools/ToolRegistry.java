package com.warehouse.ai.tools;

import com.warehouse.ai.llm.ChatMessage;

import java.util.*;
import java.util.function.Function;

/**
 * Registry for AI Function Calling tools.
 * Each tool has a name, description, JSON Schema parameters, and a handler function.
 */
public class ToolRegistry {

    private final Map<String, ToolEntry> tools = new LinkedHashMap<>();

    /**
     * Register a tool.
     *
     * @param name        tool function name
     * @param description what the tool does
     * @param parameters  JSON Schema for parameters
     * @param handler     function that takes arguments JSON string and returns result string
     */
    public void register(String name, String description,
                         Map<String, Object> parameters,
                         Function<String, String> handler) {
        tools.put(name, new ToolEntry(name, description, parameters, handler));
    }

    /**
     * Get all tool definitions for the LLM API request.
     */
    public List<ChatMessage.ToolDef> getToolDefs() {
        List<ChatMessage.ToolDef> defs = new ArrayList<>();
        for (ToolEntry entry : tools.values()) {
            ChatMessage.FunctionDef funcDef = ChatMessage.FunctionDef.builder()
                    .name(entry.name)
                    .description(entry.description)
                    .parameters(entry.parameters)
                    .build();
            defs.add(ChatMessage.ToolDef.builder().function(funcDef).build());
        }
        return defs;
    }

    /**
     * Execute a tool by name with JSON arguments.
     */
    public String execute(String name, String arguments) {
        ToolEntry entry = tools.get(name);
        if (entry == null) {
            return "{\"error\": \"未知工具: " + name + "\"}";
        }
        try {
            return entry.handler.apply(arguments);
        } catch (Exception e) {
            return "{\"error\": \"工具执行异常: " + e.getMessage() + "\"}";
        }
    }

    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    public Set<String> getToolNames() {
        return Collections.unmodifiableSet(tools.keySet());
    }

    private static class ToolEntry {
        final String name;
        final String description;
        final Map<String, Object> parameters;
        final Function<String, String> handler;

        ToolEntry(String name, String description, Map<String, Object> parameters,
                  Function<String, String> handler) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
            this.handler = handler;
        }
    }
}
