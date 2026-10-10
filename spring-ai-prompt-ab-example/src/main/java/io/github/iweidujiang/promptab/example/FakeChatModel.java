package io.github.iweidujiang.promptab.example;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

/**
 * 模拟 ChatModel，用于演示环境
 * <p>
 * 无需真实 LLM API Key，直接返回包含用户输入的回复。
 * 生产环境可替换为 OpenAI / Ollama 等真实实现。
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-10
 */
public class FakeChatModel implements ChatModel {

    @Override
    public ChatResponse call(Prompt prompt) {
        String userInput = prompt.getUserMessage() != null
                ? prompt.getUserMessage().getText()
                : "";

        String reply = "收到你的消息：" + userInput + "。这是来自 FakeChatModel 的模拟回复。";
        AssistantMessage message = new AssistantMessage(reply);
        Generation generation = new Generation(message);
        return new ChatResponse(List.of(generation));
    }
}
