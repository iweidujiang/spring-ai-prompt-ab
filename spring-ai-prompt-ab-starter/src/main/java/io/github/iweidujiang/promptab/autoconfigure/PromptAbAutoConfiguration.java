package io.github.iweidujiang.promptab.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Prompt AB 自动配置入口
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-09-30
 */
@AutoConfiguration
@EnableConfigurationProperties(PromptAbProperties.class)
public class PromptAbAutoConfiguration {

    // 后续在此注册 PromptRouter、PromptRouterAdvisor 等 Bean
}
