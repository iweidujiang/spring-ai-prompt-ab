package io.github.iweidujiang.promptab.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ConfigurableLogger 单元测试
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-09
 */
class ConfigurableLoggerTest {

    /**
     * 级别为 DEBUG 时，log() 委托到 debug()
     */
    @Test
    void log_debugLevel_delegatesToDebug() {
        Logger delegate = mock(Logger.class);
        ConfigurableLogger logger = new ConfigurableLogger(delegate, "DEBUG");

        logger.log("测试消息：{}", "参数");

        verify(delegate).debug(eq("测试消息：{}"), any(Object[].class));
        verify(delegate, never()).info(anyString(), any(Object[].class));
    }

    /**
     * 级别为 INFO 时，log() 委托到 info()
     */
    @Test
    void log_infoLevel_delegatesToInfo() {
        Logger delegate = mock(Logger.class);
        ConfigurableLogger logger = new ConfigurableLogger(delegate, "INFO");

        logger.log("测试消息：{}", "参数");

        verify(delegate).info(eq("测试消息：{}"), any(Object[].class));
    }

    /**
     * 级别为 WARN 时，log() 委托到 warn()
     */
    @Test
    void log_warnLevel_delegatesToWarn() {
        Logger delegate = mock(Logger.class);
        ConfigurableLogger logger = new ConfigurableLogger(delegate, "WARN");

        logger.log("测试消息：{}", "参数");

        verify(delegate).warn(eq("测试消息：{}"), any(Object[].class));
    }

    /**
     * 级别为 ERROR 时，log() 委托到 error()
     */
    @Test
    void log_errorLevel_delegatesToError() {
        Logger delegate = mock(Logger.class);
        ConfigurableLogger logger = new ConfigurableLogger(delegate, "ERROR");

        logger.log("测试消息：{}", "参数");

        verify(delegate).error(eq("测试消息：{}"), any(Object[].class));
    }

    /**
     * 级别为 TRACE 时，log() 委托到 trace()
     */
    @Test
    void log_traceLevel_delegatesToTrace() {
        Logger delegate = mock(Logger.class);
        ConfigurableLogger logger = new ConfigurableLogger(delegate, "TRACE");

        logger.log("测试消息：{}", "参数");

        verify(delegate).trace(eq("测试消息：{}"), any(Object[].class));
    }

    /**
     * 级别为 null 时，默认使用 INFO
     */
    @Test
    void log_nullLevel_defaultsToInfo() {
        Logger delegate = mock(Logger.class);
        ConfigurableLogger logger = new ConfigurableLogger(delegate, null);

        logger.log("测试消息");

        verify(delegate).info(eq("测试消息"), any(Object[].class));
    }

    /**
     * warn() 始终委托到 warn()，不受级别配置影响
     */
    @Test
    void warn_alwaysDelegatesToWarn() {
        Logger delegate = mock(Logger.class);
        ConfigurableLogger logger = new ConfigurableLogger(delegate, "DEBUG");

        logger.warn("警告消息：{}", "参数");

        verify(delegate).warn(eq("警告消息：{}"), any(Object[].class));
    }
}
