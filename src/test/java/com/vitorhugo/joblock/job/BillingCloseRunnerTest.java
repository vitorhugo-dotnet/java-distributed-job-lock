package com.vitorhugo.joblock.job;

import com.vitorhugo.joblock.support.PostgresIntegrationTest;
import com.vitorhugo.joblock.support.ShedLockFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "app.instance-id=instance-9",
        "job.billing-close.lock-at-least-for=PT0S",
        "job.billing-close.lock-at-most-for=PT30S",
        "job.billing-close.cron=-"
})
class BillingCloseRunnerTest extends PostgresIntegrationTest {

    private static final String LOCK = "billing-close";

    @Autowired
    BillingCloseRunner runner;

    @Autowired
    JobExecutionRepository jobExecutionRepository;

    @Autowired
    ShedLockFixture shedLock;

    @Autowired
    JdbcClient jdbcClient;

    @BeforeEach
    void limpar() {
        jdbcClient.sql("DELETE FROM job_execution").update();
        jdbcClient.sql("DELETE FROM invoices").update();
        shedLock.liberar(LOCK);
    }

    @Test
    void deveCriarRegistroDeLockNaTabelaShedlock() {
        runner.run(TriggerSource.SCHEDULED);

        assertThat(shedLock.donoAtualDoLock(LOCK)).isEqualTo("instance-9");
    }

    @Test
    void deveRegistrarOwnerDaExecucao() {
        runner.run(TriggerSource.MANUAL);

        var ultima = jobExecutionRepository.findLast(LOCK).orElseThrow();

        assertThat(ultima.owner()).isEqualTo("instance-9");
        assertThat(ultima.status()).isEqualTo(JobStatus.SUCCESS);
        assertThat(ultima.triggerSource()).isEqualTo(TriggerSource.MANUAL);
    }

    @Test
    void deveAdquirirLockNovamenteQuandoOAnteriorExpirou() {
        runner.run(TriggerSource.SCHEDULED);
        shedLock.expirarLockDeInstanciaMorta(LOCK, "instance-morta");

        JobExecution execucao = runner.run(TriggerSource.SCHEDULED);

        assertThat(execucao.status()).isEqualTo(JobStatus.SUCCESS);
        assertThat(shedLock.donoAtualDoLock(LOCK)).isEqualTo("instance-9");
    }

    @Test
    void naoDeveExecutarQuandoOutraInstanciaSeguraOLock() {
        shedLock.segurarPorOutraInstancia(LOCK, "instance-viva");

        JobExecution execucao = runner.run(TriggerSource.SCHEDULED);

        assertThat(execucao.status()).isEqualTo(JobStatus.SKIPPED_LOCK_NOT_ACQUIRED);
        assertThat(execucao.processedItems()).isZero();
        assertThat(shedLock.donoAtualDoLock(LOCK)).isEqualTo("instance-viva");
    }
}
