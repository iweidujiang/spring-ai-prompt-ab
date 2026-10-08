package io.github.iweidujiang.promptab.autoconfigure;

import io.github.iweidujiang.promptab.advisor.PromptRouterAdvisor;
import io.github.iweidujiang.promptab.experiment.ExperimentRepository;
import io.github.iweidujiang.promptab.experiment.ExperimentService;
import io.github.iweidujiang.promptab.experiment.JdbcExperimentRepository;
import io.github.iweidujiang.promptab.experiment.JdbcVariantRepository;
import io.github.iweidujiang.promptab.experiment.VariantRepository;
import io.github.iweidujiang.promptab.router.HashPromptRouter;
import io.github.iweidujiang.promptab.router.PromptRouter;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Prompt AB 自动配置入口
 * <p>
 * 当 prompt-ab.enabled=true（默认）时，注册仓储、路由器和 Advisor Bean。
 * Advisor 仅在容器中存在 ChatModel 时才创建。
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-09-30
 */
@AutoConfiguration
@EnableConfigurationProperties(PromptAbProperties.class)
@ConditionalOnProperty(prefix = "prompt-ab", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PromptAbAutoConfiguration {

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    public VariantRepository variantRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcVariantRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    public ExperimentRepository experimentRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcExperimentRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnBean(ExperimentRepository.class)
    public ExperimentService experimentService(ExperimentRepository experimentRepository) {
        return new ExperimentService(experimentRepository);
    }

    @Bean
    @ConditionalOnBean(VariantRepository.class)
    public PromptRouter promptRouter(VariantRepository variantRepository) {
        return new HashPromptRouter(variantRepository);
    }

    @Bean
    @ConditionalOnBean({PromptRouter.class, ChatModel.class})
    public PromptRouterAdvisor promptRouterAdvisor(PromptRouter promptRouter,
                                                   VariantRepository variantRepository,
                                                   PromptAbProperties properties) {
        return new PromptRouterAdvisor(promptRouter, variantRepository, properties.getDefaultExperimentKey());
    }
}
