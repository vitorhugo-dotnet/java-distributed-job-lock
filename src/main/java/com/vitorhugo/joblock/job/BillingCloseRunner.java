package com.vitorhugo.joblock.job;

import com.vitorhugo.joblock.billing.BillingCloseService;
import com.vitorhugo.joblock.config.BillingCloseProperties;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Tenta adquirir o lock distribuido, executa o job e registra o resultado -
 * inclusive quando nao executou. E o unico ponto do sistema que conhece o ShedLock.
 */
@Component
public class BillingCloseRunner {

    private static final Logger log = LoggerFactory.getLogger(BillingCloseRunner.class);
    private static final int MAX_ERROR_LENGTH = 500;

    private final LockingTaskExecutor lockingTaskExecutor;
    private final BillingCloseService billingCloseService;
    private final JobExecutionRepository jobExecutionRepository;
    private final BillingCloseProperties properties;
    private final String instanceId;

    BillingCloseRunner(LockingTaskExecutor lockingTaskExecutor,
                       BillingCloseService billingCloseService,
                       JobExecutionRepository jobExecutionRepository,
                       BillingCloseProperties properties,
                       @Value("${app.instance-id}") String instanceId) {
        this.lockingTaskExecutor = lockingTaskExecutor;
        this.billingCloseService = billingCloseService;
        this.jobExecutionRepository = jobExecutionRepository;
        this.properties = properties;
        this.instanceId = instanceId;
    }

    public JobExecution run(TriggerSource source) {
        Instant startedAt = Instant.now();
        LockConfiguration lock = new LockConfiguration(
                startedAt, properties.name(), properties.lockAtMostFor(), properties.lockAtLeastFor());

        JobExecution execution;
        try {
            LockingTaskExecutor.TaskResult<Integer> result = lockingTaskExecutor.executeWithLock(
                    (LockingTaskExecutor.TaskWithResult<Integer>)
                            () -> billingCloseService.closePendingInvoices(instanceId),
                    lock);

            // wasExecuted() e o sinal que transforma "nao fiz nada" em auditoria.
            execution = result.wasExecuted()
                    ? finished(JobStatus.SUCCESS, result.getResult(), source, startedAt, null)
                    : finished(JobStatus.SKIPPED_LOCK_NOT_ACQUIRED, 0, source, startedAt, null);
        } catch (Throwable failure) {
            log.error("[{}] job {} falhou", instanceId, properties.name(), failure);
            execution = finished(JobStatus.FAILED, 0, source, startedAt, describe(failure));
        }

        jobExecutionRepository.save(execution);
        log.info("[{}] job={} status={} processed={} trigger={}",
                instanceId, properties.name(), execution.status(),
                execution.processedItems(), source);
        return execution;
    }

    private JobExecution finished(JobStatus status, Integer processed, TriggerSource source,
                                  Instant startedAt, String error) {
        return new JobExecution(properties.name(), instanceId, status,
                processed == null ? 0 : processed, startedAt, Instant.now(), source, error);
    }

    private String describe(Throwable failure) {
        String message = failure.getClass().getSimpleName() + ": " + failure.getMessage();
        return message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message;
    }
}
