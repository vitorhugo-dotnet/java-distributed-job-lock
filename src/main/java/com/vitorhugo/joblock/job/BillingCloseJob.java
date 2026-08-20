package com.vitorhugo.joblock.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Toda instancia agenda o job; o lock distribuido decide quem realmente executa.
 */
@Component
public class BillingCloseJob {

    private final BillingCloseRunner runner;

    BillingCloseJob(BillingCloseRunner runner) {
        this.runner = runner;
    }

    @Scheduled(cron = "${job.billing-close.cron}")
    public void executar() {
        runner.run(TriggerSource.SCHEDULED);
    }
}
