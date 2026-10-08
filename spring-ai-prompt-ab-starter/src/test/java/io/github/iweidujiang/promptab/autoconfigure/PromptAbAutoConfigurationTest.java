package io.github.iweidujiang.promptab.autoconfigure;

import io.github.iweidujiang.promptab.advisor.PromptRouterAdvisor;
import io.github.iweidujiang.promptab.experiment.ExperimentRepository;
import io.github.iweidujiang.promptab.experiment.ExperimentService;
import io.github.iweidujiang.promptab.experiment.VariantRepository;
import io.github.iweidujiang.promptab.router.PromptRouter;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import java.sql.Driver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * PromptAbAutoConfiguration 自动装配测试
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-08
 */
class PromptAbAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PromptAbAutoConfiguration.class))
            .withUserConfiguration(TestDataSourceConfig.class)
            .withPropertyValues(
                    "prompt-ab.enabled=true",
                    "prompt-ab.default-experiment-key=test-exp"
            );

    /**
     * 当 JdbcTemplate 存在时，仓储、服务类和路由器 Bean 应自动注册
     */
    @Test
    void shouldRegisterRepositoryAndRouterBeans() {
        this.contextRunner.run(context -> {
            assertThat(context).hasSingleBean(VariantRepository.class);
            assertThat(context).hasSingleBean(ExperimentRepository.class);
            assertThat(context).hasSingleBean(ExperimentService.class);
            assertThat(context).hasSingleBean(PromptRouter.class);
        });
    }

    /**
     * 当 ChatModel 不存在时，Advisor Bean 不应注册
     */
    @Test
    void shouldNotRegisterAdvisorWithoutChatModel() {
        this.contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(PromptRouterAdvisor.class);
        });
    }

    /**
     * 当 ChatModel 存在时，Advisor Bean 应自动注册
     */
    @Test
    void shouldRegisterAdvisorWhenChatModelPresent() {
        this.contextRunner
                .withUserConfiguration(MockChatModelConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(PromptRouterAdvisor.class);
                });
    }

    /**
     * 当 prompt-ab.enabled=false 时，所有 Bean 均不注册
     */
    @Test
    void shouldNotRegisterAnyBeanWhenDisabled() {
        this.contextRunner
                .withPropertyValues("prompt-ab.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(VariantRepository.class);
                    assertThat(context).doesNotHaveBean(ExperimentRepository.class);
                    assertThat(context).doesNotHaveBean(ExperimentService.class);
                    assertThat(context).doesNotHaveBean(PromptRouter.class);
                    assertThat(context).doesNotHaveBean(PromptRouterAdvisor.class);
                });
    }

    /**
     * 未配置 prompt-ab.enabled 时默认启用（matchIfMissing = true）
     */
    @Test
    void shouldEnableByDefault() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PromptAbAutoConfiguration.class))
                .withUserConfiguration(TestDataSourceConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(VariantRepository.class);
                    assertThat(context).hasSingleBean(PromptRouter.class);
                });
    }

    @Configuration
    static class TestDataSourceConfig {
        @SuppressWarnings("unchecked")
        @Bean
        JdbcTemplate jdbcTemplate() throws Exception {
            Driver driver = (Driver) Class.forName("org.h2.Driver").getDeclaredConstructor().newInstance();
            SimpleDriverDataSource dataSource = new SimpleDriverDataSource(driver, "jdbc:h2:mem:promptab-test;DB_CLOSE_DELAY=-1");
            return new JdbcTemplate(dataSource);
        }
    }

    @Configuration
    static class MockChatModelConfig {
        @Bean
        ChatModel chatModel() {
            return mock(ChatModel.class);
        }
    }
}
