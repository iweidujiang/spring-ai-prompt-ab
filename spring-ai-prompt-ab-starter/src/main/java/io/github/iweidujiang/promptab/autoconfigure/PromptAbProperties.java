package io.github.iweidujiang.promptab.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Prompt AB 配置属性
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-09-30
 */
@ConfigurationProperties(prefix = "prompt-ab")
public class PromptAbProperties {

    /** 是否启用 Prompt AB */
    private boolean enabled = true;

    /** 默认路由策略：HASH（基于 sessionId 哈希） */
    private String defaultRoutingStrategy = "HASH";

    /** 默认实验标识（当调用方未指定时使用） */
    private String defaultExperimentKey;

    /** 是否启用 Micrometer 指标采集 */
    private boolean metricsEnabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultRoutingStrategy() {
        return defaultRoutingStrategy;
    }

    public void setDefaultRoutingStrategy(String defaultRoutingStrategy) {
        this.defaultRoutingStrategy = defaultRoutingStrategy;
    }

    public String getDefaultExperimentKey() {
        return defaultExperimentKey;
    }

    public void setDefaultExperimentKey(String defaultExperimentKey) {
        this.defaultExperimentKey = defaultExperimentKey;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }
}
