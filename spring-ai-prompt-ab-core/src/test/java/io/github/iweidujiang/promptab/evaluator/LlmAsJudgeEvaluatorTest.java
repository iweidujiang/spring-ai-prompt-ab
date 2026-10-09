package io.github.iweidujiang.promptab.evaluator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LlmAsJudgeEvaluator 单元测试
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
@ExtendWith(MockitoExtension.class)
class LlmAsJudgeEvaluatorTest {

    @Mock
    private ChatModel chatModel;

    /**
     * 评分模型返回合法 JSON，正确解析分数
     */
    @Test
    void evaluate_validJsonResponse_parsesScore() {
        mockChatResponse("{\"score\": 0.85}");

        var evaluator = new LlmAsJudgeEvaluator(chatModel);
        var context = buildContext("你好", "你好！有什么可以帮你的？");

        double score = evaluator.evaluate(context);

        assertEquals(0.85, score, 0.001);
        verify(chatModel).call(any(Prompt.class));
    }

    /**
     * 评分模型返回纯数字，正确解析
     */
    @Test
    void evaluate_plainNumberResponse_parsesScore() {
        mockChatResponse("0.75");

        var evaluator = new LlmAsJudgeEvaluator(chatModel);
        var context = buildContext("问题", "回答");

        double score = evaluator.evaluate(context);

        assertEquals(0.75, score, 0.001);
    }

    /**
     * 分数超出范围时截断到 [0.0, 1.0]
     */
    @Test
    void evaluate_scoreOutOfRange_clampsToValidRange() {
        mockChatResponse("{\"score\": 1.5}");

        var evaluator = new LlmAsJudgeEvaluator(chatModel);
        var context = buildContext("问题", "回答");

        double score = evaluator.evaluate(context);

        assertEquals(1.0, score, 0.001);
    }

    /**
     * 评分模型返回空响应，得零分
     */
    @Test
    void evaluate_nullResponse_returnsZero() {
        when(chatModel.call(any(Prompt.class))).thenReturn(null);

        var evaluator = new LlmAsJudgeEvaluator(chatModel);
        var context = buildContext("问题", "回答");

        double score = evaluator.evaluate(context);

        assertEquals(0.0, score);
    }

    /**
     * 评分模型返回无法解析的内容，得零分
     */
    @Test
    void evaluate_unparseableResponse_returnsZero() {
        mockChatResponse("这不是一个有效的分数");

        var evaluator = new LlmAsJudgeEvaluator(chatModel);
        var context = buildContext("问题", "回答");

        double score = evaluator.evaluate(context);

        assertEquals(0.0, score);
    }

    /**
     * 评分模型抛异常，得零分
     */
    @Test
    void evaluate_chatModelThrows_returnsZero() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("模型调用失败"));

        var evaluator = new LlmAsJudgeEvaluator(chatModel);
        var context = buildContext("问题", "回答");

        double score = evaluator.evaluate(context);

        assertEquals(0.0, score);
    }

    /**
     * 自定义评估器名称
     */
    @Test
    void evaluate_customName_returnsCorrectName() {
        var evaluator = new LlmAsJudgeEvaluator("custom_judge", chatModel, "评分：{input} {output}");

        assertEquals("custom_judge", evaluator.name());
    }

    /**
     * 自定义评分 Prompt 模板
     */
    @Test
    void evaluate_customPrompt_usesTemplate() {
        mockChatResponse("{\"score\": 0.9}");

        var customTemplate = "请评估以下内容的质量。输入：{input}，输出：{output}。返回 JSON：{\"score\": 分数}";
        var evaluator = new LlmAsJudgeEvaluator("custom", chatModel, customTemplate);
        var context = buildContext("测试输入", "测试输出");

        double score = evaluator.evaluate(context);

        assertEquals(0.9, score, 0.001);
    }

    private void mockChatResponse(String content) {
        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage assistantMessage = mock(AssistantMessage.class);

        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(assistantMessage);
        when(assistantMessage.getText()).thenReturn(content);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
    }

    private EvaluationContext buildContext(String input, String output) {
        var context = new EvaluationContext();
        context.setInput(input);
        context.setOutput(output);
        return context;
    }
}
