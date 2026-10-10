package io.github.iweidujiang.promptab.example;

import io.github.iweidujiang.promptab.advisor.PromptRouterAdvisor;
import io.github.iweidujiang.promptab.evaluator.EvaluatorChain;
import io.github.iweidujiang.promptab.evaluator.JdbcMetricEventRepository;
import io.github.iweidujiang.promptab.evaluator.MetricEventRepository;
import io.github.iweidujiang.promptab.experiment.ExperimentRepository;
import io.github.iweidujiang.promptab.experiment.ExperimentService;
import io.github.iweidujiang.promptab.experiment.JdbcExperimentRepository;
import io.github.iweidujiang.promptab.experiment.JdbcVariantRepository;
import io.github.iweidujiang.promptab.experiment.VariantRepository;
import io.github.iweidujiang.promptab.experiment.VariantService;
import io.github.iweidujiang.promptab.router.HashPromptRouter;
import io.github.iweidujiang.promptab.router.PromptRouter;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 示例工程配置
 * <p>
 * 显式注册所有 Prompt AB 组件和 FakeChatModel。
 * 生产环境中可移除此配置，依赖自动装配即可。
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-10
 */
@Configuration
public class ExampleConfig {

    @Bean
    public ChatModel chatModel() {
        return new FakeChatModel();
    }

    @Bean
    public VariantRepository variantRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcVariantRepository(jdbcTemplate);
    }

    @Bean
    public ExperimentRepository experimentRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcExperimentRepository(jdbcTemplate);
    }

    @Bean
    public MetricEventRepository metricEventRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcMetricEventRepository(jdbcTemplate);
    }

    @Bean
    public ExperimentService experimentService(ExperimentRepository experimentRepository) {
        return new ExperimentService(experimentRepository);
    }

    @Bean
    public VariantService variantService(VariantRepository variantRepository,
                                         ExperimentRepository experimentRepository) {
        return new VariantService(variantRepository, experimentRepository);
    }

    @Bean
    public PromptRouter promptRouter(VariantRepository variantRepository) {
        return new HashPromptRouter(variantRepository);
    }

    @Bean
    public EvaluatorChain evaluatorChain(MetricEventRepository metricEventRepository) {
        return new EvaluatorChain(java.util.List.of(), metricEventRepository);
    }

    @Bean
    public PromptRouterAdvisor promptRouterAdvisor(PromptRouter promptRouter,
                                                   VariantRepository variantRepository,
                                                   EvaluatorChain evaluatorChain) {
        return new PromptRouterAdvisor(promptRouter, variantRepository,
                "demo-chat", evaluatorChain);
    }
}
