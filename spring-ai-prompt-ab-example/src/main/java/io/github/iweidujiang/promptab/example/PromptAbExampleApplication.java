package io.github.iweidujiang.promptab.example;

import io.github.iweidujiang.promptab.autoconfigure.PromptAbAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 示例工程启动类
 * <p>
 * 排除 PromptAbAutoConfiguration，由 ExampleConfig 显式注册所有 Bean，
 * 避免与自动配置产生 BeanDefinition 冲突。
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-09-30
 */
@SpringBootApplication(exclude = {PromptAbAutoConfiguration.class})
public class PromptAbExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromptAbExampleApplication.class, args);
    }
}
