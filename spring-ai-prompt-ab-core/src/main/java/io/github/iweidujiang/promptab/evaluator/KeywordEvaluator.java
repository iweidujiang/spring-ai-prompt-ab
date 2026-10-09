package io.github.iweidujiang.promptab.evaluator;

import java.util.List;

/**
 * 关键词评估器
 * <p>
 * 检查 LLM 输出是否包含/不包含指定关键词。
 * 评分规则：每个必须包含的关键词出现得加分，每个禁止出现的关键词出现则扣分。
 * 最终分数 = (命中数 + 未命中禁止数) / 总检查数。
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
public class KeywordEvaluator implements Evaluator {

    private final List<String> requiredKeywords;
    private final List<String> forbiddenKeywords;

    public KeywordEvaluator(List<String> requiredKeywords, List<String> forbiddenKeywords) {
        this.requiredKeywords = requiredKeywords != null ? List.copyOf(requiredKeywords) : List.of();
        this.forbiddenKeywords = forbiddenKeywords != null ? List.copyOf(forbiddenKeywords) : List.of();
    }

    @Override
    public String name() {
        return "keyword";
    }

    @Override
    public double evaluate(EvaluationContext context) {
        String output = context.getOutput();
        if (output == null || output.isBlank()) {
            return 0.0;
        }

        int totalChecks = requiredKeywords.size() + forbiddenKeywords.size();
        if (totalChecks == 0) {
            return 1.0;
        }

        int passed = 0;

        for (String keyword : requiredKeywords) {
            if (output.contains(keyword)) {
                passed++;
            }
        }

        for (String keyword : forbiddenKeywords) {
            if (!output.contains(keyword)) {
                passed++;
            }
        }

        return (double) passed / totalChecks;
    }
}
