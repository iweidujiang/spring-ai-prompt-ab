package io.github.iweidujiang.promptab.evaluator;

/**
 * Prompt 评估器
 * <p>
 * 对一次 Prompt 调用的输出进行打分，分数范围 [0.0, 1.0]。
 * 支持同步规则校验（JSON Schema、关键词、延迟阈值）和异步 LLM-as-Judge。
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-09-30
 */
public interface Evaluator {

    /**
     * 评估器名称，用于持久化和日志标识
     */
    String name();

    /**
     * 对调用结果打分
     *
     * @param context 评估上下文
     * @return 分数，范围 [0.0, 1.0]
     */
    double evaluate(EvaluationContext context);
}
