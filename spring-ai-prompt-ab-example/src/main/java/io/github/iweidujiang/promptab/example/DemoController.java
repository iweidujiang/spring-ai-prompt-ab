package io.github.iweidujiang.promptab.example;

import io.github.iweidujiang.promptab.advisor.PromptRouterAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * 演示控制器
 * <p>
 * 模拟聊天接口，展示 Prompt A/B 测试的完整链路：
 * 路由选变体 → 替换 Prompt → 调用 LLM → 评估 → 返回结果。
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-10
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final ChatClient chatClient;

    public DemoController(ChatModel chatModel, PromptRouterAdvisor promptRouterAdvisor) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(promptRouterAdvisor)
                .build();
    }

    /**
     * 发送聊天消息
     * <p>
     * 请求体：{ "message": "你好", "sessionId": "user-001" }
     * sessionId 可选，不传则自动生成。
     */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody ChatRequest request) {
        String sessionId = request.sessionId() != null
                ? request.sessionId()
                : UUID.randomUUID().toString();

        ChatClientResponse response = chatClient.prompt()
                .user(request.message())
                .advisors(a -> a.param("sessionId", sessionId))
                .call()
                .chatClientResponse();

        String reply = response.chatResponse() != null
                ? response.chatResponse().getResult().getOutput().getText()
                : "";

        String variantKey = (String) response.context().get("promptab.variantKey");

        return Map.of(
                "reply", reply,
                "variantKey", variantKey,
                "sessionId", sessionId
        );
    }

    public record ChatRequest(String message, String sessionId) {
    }
}
