package com.vitorhugo.joblock.job;

import com.vitorhugo.joblock.support.PostgresIntegrationTest;
import com.vitorhugo.joblock.support.ShedLockFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "app.instance-id=instance-test",
        "job.billing-close.lock-at-least-for=PT0S",
        "job.billing-close.cron=-"
})
class BillingCloseLockContentionTest extends PostgresIntegrationTest {

    @Autowired
    BillingCloseRunner runner;

    @Autowired
    ShedLockFixture shedLock;

    @Autowired
    JdbcClient jdbcClient;

    @BeforeEach
    void limpar() {
        jdbcClient.sql("DELETE FROM job_execution").update();
        jdbcClient.sql("DELETE FROM invoices").update();
        shedLock.liberar("billing-close");
        jdbcClient.sql("""
                INSERT INTO invoices (customer, amount, status)
                SELECT 'cliente-' || g, 10.00, 'PENDING' FROM generate_series(1, 5) AS g
                """).update();
    }

    @Test
    void apenasUmaDasTresChamadasSimultaneasDeveExecutar() throws Exception {
        var resultados = dispararTresChamadasSimultaneas();

        assertThat(resultados).filteredOn(e -> e.status() == JobStatus.SUCCESS).hasSize(1);
        assertThat(resultados).filteredOn(e -> e.status() == JobStatus.SKIPPED_LOCK_NOT_ACQUIRED)
                .hasSize(2);

        Integer pendentes = jdbcClient.sql("SELECT count(*) FROM invoices WHERE status = 'PENDING'")
                .query(Integer.class).single();
        assertThat(pendentes).isZero();
    }

    @Test
    void deveGravarUmaLinhaDeAuditoriaPorTentativa() throws Exception {
        dispararTresChamadasSimultaneas();

        var statuses = jdbcClient.sql("SELECT status FROM job_execution WHERE job_name = 'billing-close'")
                .query(String.class).list();

        assertThat(statuses).hasSize(3)
                .containsOnlyOnce("SUCCESS")
                .filteredOn("SKIPPED_LOCK_NOT_ACQUIRED"::equals).hasSize(2);
    }

    private List<JobExecution> dispararTresChamadasSimultaneas() throws Exception {
        int instancias = 3;
        var barreira = new CyclicBarrier(instancias);
        List<Callable<JobExecution>> tarefas = List.of(
                chamada(barreira), chamada(barreira), chamada(barreira));

        try (ExecutorService pool = Executors.newFixedThreadPool(instancias)) {
            List<Future<JobExecution>> futuros = pool.invokeAll(tarefas);
            return futuros.stream().map(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).toList();
        }
    }

    private Callable<JobExecution> chamada(CyclicBarrier barreira) {
        return () -> {
            barreira.await();
            return runner.run(TriggerSource.SCHEDULED);
        };
    }
}
