package io.github.iweidujiang.promptab.logging;

import org.slf4j.Logger;

/**
 * 支持运行时配置日志级别的 Logger 包装器
 * <p>
 * 根据构造时指定的级别字符串，将所有日志调用委托到对应级别的 SLF4J 方法。
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
public class ConfigurableLogger {

    private final Logger delegate;
    private final String level;

    public ConfigurableLogger(Logger delegate, String level) {
        this.delegate = delegate;
        this.level = level != null ? level.toUpperCase() : "INFO";
    }

    public void log(String format, Object... args) {
        switch (level) {
            case "TRACE" -> delegate.trace(format, args);
            case "DEBUG" -> delegate.debug(format, args);
            case "WARN" -> delegate.warn(format, args);
            case "ERROR" -> delegate.error(format, args);
            default -> delegate.info(format, args);
        }
    }

    public void warn(String format, Object... args) {
        delegate.warn(format, args);
    }
}
