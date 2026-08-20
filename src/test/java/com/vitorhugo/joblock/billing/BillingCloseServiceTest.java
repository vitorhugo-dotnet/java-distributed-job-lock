package com.vitorhugo.joblock.billing;

import com.vitorhugo.joblock.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "job.billing-close.cron=-")
class BillingCloseServiceTest extends PostgresIntegrationTest {

    @Autowired
    BillingCloseService service;

    @Autowired
    JdbcClient jdbcClient;

    @BeforeEach
    void limparEPreparar() {
        jdbcClient.sql("DELETE FROM invoices").update();
        jdbcClient.sql("""
                INSERT INTO invoices (customer, amount, status)
                VALUES ('ana', 100.00, 'PENDING'),
                       ('bruno', 250.00, 'PENDING'),
                       ('carla', 90.00, 'CLOSED')
                """).update();
    }

    @Test
    void deveFecharApenasFaturasPendentes() {
        int processadas = service.closePendingInvoices("instance-1");

        assertThat(processadas).isEqualTo(2);

        Integer pendentes = jdbcClient.sql("SELECT count(*) FROM invoices WHERE status = 'PENDING'")
                .query(Integer.class).single();
        assertThat(pendentes).isZero();
    }

    @Test
    void deveSerIdempotenteEmExecucoesRepetidas() {
        assertThat(service.closePendingInvoices("instance-1")).isEqualTo(2);
        assertThat(service.closePendingInvoices("instance-1")).isZero();

        Integer fechadasPelaInstancia = jdbcClient
                .sql("SELECT count(*) FROM invoices WHERE closed_by = 'instance-1'")
                .query(Integer.class).single();
        assertThat(fechadasPelaInstancia).isEqualTo(2);
    }

    @Test
    void deveRegistrarQuemFechouEQuando() {
        service.closePendingInvoices("instance-7");

        var registros = jdbcClient
                .sql("SELECT closed_by FROM invoices WHERE status = 'CLOSED' AND closed_at IS NOT NULL")
                .query(String.class).list();

        assertThat(registros).contains("instance-7");
    }
}
