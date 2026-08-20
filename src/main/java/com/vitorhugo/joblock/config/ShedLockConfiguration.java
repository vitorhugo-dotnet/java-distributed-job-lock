package com.vitorhugo.joblock.config;

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * O lock e usado pela API programatica (LockingTaskExecutor) em vez da anotacao
 * SchedulerLock: a anotacao simplesmente nao invoca o metodo quando o lock nao e
 * adquirido, e este projeto precisa registrar esse skip como SKIPPED_LOCK_NOT_ACQUIRED.
 */
@Configuration
@EnableConfigurationProperties(BillingCloseProperties.class)
public class ShedLockConfiguration {

    @Bean
    public LockProvider lockProvider(DataSource dataSource, @Value("${app.instance-id}") String instanceId) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        // O relogio de referencia passa a ser o do PostgreSQL: sem isso,
                        // drift entre as instancias vira lock expirado cedo demais.
                        .usingDbTime()
                        // Torna a coluna locked_by auditavel: quem segura o lock agora.
                        .withLockedByValue(instanceId)
                        .build());
    }

    @Bean
    public LockingTaskExecutor lockingTaskExecutor(LockProvider lockProvider) {
        return new DefaultLockingTaskExecutor(lockProvider);
    }
}
