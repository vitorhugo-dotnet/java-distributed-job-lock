package com.vitorhugo.joblock.job;

import com.vitorhugo.joblock.config.BillingCloseProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/jobs/billing-close")
class JobController {

    private final BillingCloseRunner runner;
    private final JobExecutionRepository jobExecutionRepository;
    private final BillingCloseProperties properties;

    JobController(BillingCloseRunner runner,
                  JobExecutionRepository jobExecutionRepository,
                  BillingCloseProperties properties) {
        this.runner = runner;
        this.jobExecutionRepository = jobExecutionRepository;
        this.properties = properties;
    }

    @GetMapping("/status")
    JobStatusResponse status() {
        return JobStatusResponse.from(properties.name(),
                jobExecutionRepository.findLast(properties.name()));
    }

    /**
     * Dispara o job manualmente pelo mesmo caminho do agendamento - e portanto tambem
     * respeita o lock. Se outra instancia estiver executando, a resposta e 200 com
     * SKIPPED_LOCK_NOT_ACQUIRED: isso e informacao, nao erro.
     */
    @PostMapping("/run-now")
    JobStatusResponse runNow() {
        JobExecution execution = runner.run(TriggerSource.MANUAL);
        return JobStatusResponse.from(properties.name(), Optional.of(execution));
    }
}
