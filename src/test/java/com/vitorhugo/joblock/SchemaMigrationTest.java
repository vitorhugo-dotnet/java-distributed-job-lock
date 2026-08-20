package com.vitorhugo.joblock;

import com.vitorhugo.joblock.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "job.billing-close.cron=-")
class SchemaMigrationTest extends PostgresIntegrationTest {

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void deveCriarTabelasDoLockEDoDominio() {
        var tabelas = jdbcClient
                .sql("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")
                .query(String.class)
                .list();

        assertThat(tabelas).contains("shedlock", "invoices", "job_execution");
    }

    @Test
    void naoDeveAplicarOSeedDeDemonstracaoForaDoProfileDocker() {
        var versoes = jdbcClient.sql("SELECT version FROM flyway_schema_history")
                .query(String.class)
                .list();

        assertThat(versoes).containsExactly("1", "2", "3");
    }
}
