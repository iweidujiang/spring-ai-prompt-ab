package io.github.iweidujiang.promptab.autoconfigure;

import io.github.iweidujiang.promptab.advisor.PromptRouterAdvisor;
import io.github.iweidujiang.promptab.evaluator.Evaluator;
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
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Prompt AB 自动配置入口
 * <p>
 * 当 prompt-ab.enabled=true（默认）时，注册仓储、路由器和 Advisor Bean。
 * Advisor 仅在容器中存在 ChatModel 时才创建。
 * <p>
 * 仓储层使用 @ConditionalOnClass + ObjectProvider 代替 @ConditionalOnBean，
 * 避免自动配置处理顺序导致 JdbcTemplate Bean 尚未注册时条件判定为 false。
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-09-30
 */
@AutoConfiguration
@EnableConfigurationProperties(PromptAbProperties.class)
@ConditionalOnProperty(prefix = "prompt-ab", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PromptAbAutoConfiguration {

    @Bean
    @ConditionalOnClass(JdbcTemplate.class)
    public VariantRepository variantRepository(ObjectProvider<JdbcTemplate> jdbcTemplate) {
        return new JdbcVariantRepository(jdbcTemplate.getIfAvailable());
    }

    @Bean
    @ConditionalOnClass(JdbcTemplate.class)
    public ExperimentRepository experimentRepository(ObjectProvider<JdbcTemplate> jdbcTemplate) {
        return new JdbcExperimentRepository(jdbcTemplate.getIfAvailable());
    }

    @Bean
    @ConditionalOnClass(JdbcTemplate.class)
    public MetricEventRepository metricEventRepository(ObjectProvider<JdbcTemplate> jdbcTemplate) {
        return new JdbcMetricEventRepository(jdbcTemplate.getIfAvailable());
    }

    @Bean
    @ConditionalOnBean(ExperimentRepository.class)
    public ExperimentService experimentService(ExperimentRepository experimentRepository) {
        return new ExperimentService(experimentRepository);
    }

    @Bean
    @ConditionalOnBean({VariantRepository.class, ExperimentRepository.class})
    public VariantService variantService(VariantRepository variantRepository,
                                         ExperimentRepository experimentRepository) {
        return new VariantService(variantRepository, experimentRepository);
    }

    @Bean
    @ConditionalOnBean(VariantRepository.class)
    public PromptRouter promptRouter(VariantRepository variantRepository) {
        return new HashPromptRouter(variantRepository);
    }

    @Bean
    @ConditionalOnBean({MetricEventRepository.class})
    public EvaluatorChain evaluatorChain(ObjectProvider<Evaluator> evaluators,
                                         MetricEventRepository metricEventRepository,
                                         ObjectProvider<MeterRegistry> meterRegistry,
                                         PromptAbProperties properties) {
        return new EvaluatorChain(evaluators.orderedStream().toList(), metricEventRepository,
                meterRegistry.getIfAvailable(), properties.getLogLevel());
    }

    @Bean
    @ConditionalOnBean({PromptRouter.class, ChatModel.class})
    public PromptRouterAdvisor promptRouterAdvisor(PromptRouter promptRouter,
                                                   VariantRepository variantRepository,
                                                   PromptAbProperties properties,
                                                   ObjectProvider<EvaluatorChain> evaluatorChain,
                                                   ObjectProvider<MeterRegistry> meterRegistry) {
        return new PromptRouterAdvisor(promptRouter, variantRepository,
                properties.getDefaultExperimentKey(), evaluatorChain.getIfAvailable(),
                meterRegistry.getIfAvailable(), properties.getLogLevel());
    }
}
