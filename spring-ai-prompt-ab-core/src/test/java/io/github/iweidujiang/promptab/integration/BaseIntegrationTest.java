package io.github.iweidujiang.promptab.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;

/**
 * 集成测试基类
 * <p>
 * 启动 MySQL Testcontainer（reuse 模式）并执行 Flyway 迁移。
 * 测试结束后容器不销毁，可手动连接查看数据。
 * <p>
 * 子类通过 {@link #getJdbcTemplate()} 获取数据库连接。
 * <p>
 * 手动销毁容器：
 * <pre>
 *   docker ps --filter "ancestor=mysql:8.4.8" --format "{{.ID}}"
 *   docker stop &lt;容器ID&gt;
 * </pre>
 *
 * @author 微信公众号:苏渡苇 GitHub: https://github.com/iweidujiang
 * @since 2026-10-10
 */
@TestInstance(Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    private static final MySQLContainer<?> MYSQL = createContainer();

    private static JdbcTemplate jdbcTemplate;

    private static MySQLContainer<?> createContainer() {
        MySQLContainer<?> container = new MySQLContainer<>("mysql:8.4.8")
                .withReuse(true);
        return container;
    }

    @BeforeAll
    void initDatabase() {
        if (!MYSQL.isRunning()) {
            MYSQL.start();
        }

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(MYSQL.getJdbcUrl());
        dataSource.setUsername(MYSQL.getUsername());
        dataSource.setPassword(MYSQL.getPassword());

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        jdbcTemplate = new JdbcTemplate(dataSource);

        System.out.println();
        System.out.println("===========================================");
        System.out.println(" MySQL 容器保持运行，测试结束后不销毁");
        System.out.println("===========================================");
        System.out.println(" Host     : " + MYSQL.getHost());
        System.out.println(" Port     : " + MYSQL.getMappedPort(3306));
        System.out.println(" Database : " + MYSQL.getDatabaseName());
        System.out.println(" Username : " + MYSQL.getUsername());
        System.out.println(" Password : " + MYSQL.getPassword());
        System.out.println(" JDBC URL : " + MYSQL.getJdbcUrl());
        System.out.println("===========================================");
        System.out.println(" 手动销毁: docker stop " + MYSQL.getContainerId().substring(0, 12));
        System.out.println("===========================================");
        System.out.println();
    }

    protected JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
}
