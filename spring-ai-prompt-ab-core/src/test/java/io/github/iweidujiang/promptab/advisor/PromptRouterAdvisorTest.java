package io.github.iweidujiang.promptab.advisor;

import io.github.iweidujiang.promptab.domain.Variant;
import io.github.iweidujiang.promptab.experiment.VariantRepository;
import io.github.iweidujiang.promptab.router.PromptRouter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PromptRouterAdvisor 单元测试
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-09-30
 */
class PromptRouterAdvisorTest {

    private PromptRouter promptRouter;
    private VariantRepository variantRepository;
    private CallAdvisorChain chain;
    private PromptRouterAdvisor advisor;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        promptRouter = mock(PromptRouter.class);
        variantRepository = mock(VariantRepository.class);
        chain = mock(CallAdvisorChain.class);
        meterRegistry = new SimpleMeterRegistry();
        advisor = new PromptRouterAdvisor(promptRouter, variantRepository, "test-exp");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    /**
     * 正常路由：替换 PromptTemplate 并将 variantKey 写入响应上下文
     */
    @Test
    void adviseCall_replacesPromptTemplateAndEnrichesContext() {
        Variant variant = createVariant("variant-a", "你好，我是变体A的Prompt");
        when(promptRouter.route(eq("test-exp"), anyMap())).thenReturn("variant-a");
        when(variantRepository.findActiveByExperimentKey("test-exp"))
                .thenReturn(List.of(variant));

        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(new UserMessage("原始用户输入")))
                .context(Map.of("sessionId", "user-123"))
                .build();

        ChatClientResponse mockResponse = ChatClientResponse.builder()
                .chatResponse(mock(ChatResponse.class))
                .build();
        when(chain.nextCall(any(ChatClientRequest.class))).thenReturn(mockResponse);

        // 执行
        ChatClientResponse result = advisor.adviseCall(request, chain);

        // 验证：chain 被调用，且请求中的用户消息被替换为变体模板
        verify(chain).nextCall(argThat(req ->
                "你好，我是变体A的Prompt".equals(req.prompt().getUserMessage().getText())));

        // 验证：响应上下文包含 variantKey
        assertNotNull(result);
        assertEquals("variant-a", result.context().get("promptab.variantKey"));
    }

    /**
     * 变体不存在时抛出异常
     */
    @Test
    void adviseCall_throwsExceptionWhenVariantNotFound() {
        when(promptRouter.route(eq("test-exp"), anyMap())).thenReturn("non-existent");
        when(variantRepository.findActiveByExperimentKey("test-exp"))
                .thenReturn(List.of());

        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(new UserMessage("用户输入")))
                .context(Map.of("sessionId", "user-123"))
                .build();

        assertThrows(IllegalStateException.class, () -> advisor.adviseCall(request, chain));
    }

    /**
     * 缺少 sessionId 时使用请求 hash 作为兜底
     */
    @Test
    void adviseCall_usesRequestHashAsFallbackSessionId() {
        Variant variant = createVariant("variant-a", "模板");
        when(promptRouter.route(eq("test-exp"), anyMap())).thenReturn("variant-a");
        when(variantRepository.findActiveByExperimentKey("test-exp"))
                .thenReturn(List.of(variant));

        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(new UserMessage("用户输入")))
                .build();

        ChatClientResponse mockResponse = ChatClientResponse.builder()
                .chatResponse(mock(ChatResponse.class))
                .build();
        when(chain.nextCall(any(ChatClientRequest.class))).thenReturn(mockResponse);

        // 不应抛出异常
        assertDoesNotThrow(() -> advisor.adviseCall(request, chain));
    }

    /**
     * 注入 MeterRegistry 后，路由决策 Counter 和延迟 Timer 被正确记录
     */
    @Test
    void adviseCall_withMeterRegistry_recordsRoutingMetrics() {
        Variant variant = createVariant("variant-a", "模板");
        when(promptRouter.route(eq("test-exp"), anyMap())).thenReturn("variant-a");
        when(variantRepository.findActiveByExperimentKey("test-exp"))
                .thenReturn(List.of(variant));

        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(new UserMessage("用户输入")))
                .context(Map.of("sessionId", "user-123"))
                .build();

        ChatClientResponse mockResponse = ChatClientResponse.builder()
                .chatResponse(mock(ChatResponse.class))
                .build();
        when(chain.nextCall(any(ChatClientRequest.class))).thenReturn(mockResponse);

        PromptRouterAdvisor advisorWithMetrics = new PromptRouterAdvisor(
                promptRouter, variantRepository, "test-exp", null, meterRegistry);
        advisorWithMetrics.adviseCall(request, chain);

        // 验证 Counter
        double counterCount = meterRegistry.find("promptab.routing.decisions")
                .tag("experimentKey", "test-exp")
                .tag("variantKey", "variant-a")
                .counter().count();
        assertEquals(1.0, counterCount, 0.001);

        // 验证 Timer
        long timerCount = meterRegistry.find("promptab.routing.latency")
                .tag("experimentKey", "test-exp")
                .tag("variantKey", "variant-a")
                .timer().count();
        assertEquals(1, timerCount);
    }

    /**
     * 未注入 MeterRegistry 时不报错（向后兼容）
     */
    @Test
    void adviseCall_withoutMeterRegistry_doesNotThrow() {
        Variant variant = createVariant("variant-a", "模板");
        when(promptRouter.route(eq("test-exp"), anyMap())).thenReturn("variant-a");
        when(variantRepository.findActiveByExperimentKey("test-exp"))
                .thenReturn(List.of(variant));

        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(new UserMessage("用户输入")))
                .build();

        ChatClientResponse mockResponse = ChatClientResponse.builder()
                .chatResponse(mock(ChatResponse.class))
                .build();
        when(chain.nextCall(any(ChatClientRequest.class))).thenReturn(mockResponse);

        assertDoesNotThrow(() -> advisor.adviseCall(request, chain));
    }

    /**
     * 调用完成后 MDC 被清理，不残留上下文
     */
    @Test
    void adviseCall_afterCall_mdcIsCleaned() {
        Variant variant = createVariant("variant-a", "模板");
        when(promptRouter.route(eq("test-exp"), anyMap())).thenReturn("variant-a");
        when(variantRepository.findActiveByExperimentKey("test-exp"))
                .thenReturn(List.of(variant));

        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(new UserMessage("用户输入")))
                .context(Map.of("sessionId", "user-123"))
                .build();

        ChatClientResponse mockResponse = ChatClientResponse.builder()
                .chatResponse(mock(ChatResponse.class))
                .build();
        when(chain.nextCall(any(ChatClientRequest.class))).thenReturn(mockResponse);

        advisor.adviseCall(request, chain);

        assertNull(MDC.get("promptab.sessionId"));
        assertNull(MDC.get("promptab.experimentKey"));
        assertNull(MDC.get("promptab.variantKey"));
    }

    /**
     * 调用异常时 MDC 同样被清理
     */
    @Test
    void adviseCall_onException_mdcIsCleaned() {
        when(promptRouter.route(eq("test-exp"), anyMap())).thenReturn("non-existent");
        when(variantRepository.findActiveByExperimentKey("test-exp"))
                .thenReturn(List.of());

        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(new UserMessage("用户输入")))
                .context(Map.of("sessionId", "user-123"))
                .build();

        assertThrows(IllegalStateException.class, () -> advisor.adviseCall(request, chain));

        assertNull(MDC.get("promptab.sessionId"));
        assertNull(MDC.get("promptab.experimentKey"));
        assertNull(MDC.get("promptab.variantKey"));
    }

    /**
     * 调用期间 chain.nextCall 中可读取到 MDC 上下文
     */
    @Test
    void adviseCall_duringCall_mdcContainsContext() {
        Variant variant = createVariant("variant-a", "模板");
        when(promptRouter.route(eq("test-exp"), anyMap())).thenReturn("variant-a");
        when(variantRepository.findActiveByExperimentKey("test-exp"))
                .thenReturn(List.of(variant));

        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(new UserMessage("用户输入")))
                .context(Map.of("sessionId", "user-123"))
                .build();

        ChatClientResponse mockResponse = ChatClientResponse.builder()
                .chatResponse(mock(ChatResponse.class))
                .build();
        when(chain.nextCall(any(ChatClientRequest.class))).thenAnswer(invocation -> {
            assertEquals("user-123", MDC.get("promptab.sessionId"));
            assertEquals("test-exp", MDC.get("promptab.experimentKey"));
            assertEquals("variant-a", MDC.get("promptab.variantKey"));
            return mockResponse;
        });

        advisor.adviseCall(request, chain);
    }

    private Variant createVariant(String variantKey, String promptTemplate) {
        Variant variant = new Variant();
        variant.setVariantKey(variantKey);
        variant.setPromptTemplate(promptTemplate);
        variant.setTrafficPct(100);
        variant.setIsActive(true);
        return variant;
    }
}
