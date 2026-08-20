package com.vitorhugo.joblock.job;

import java.time.Instant;

public record JobExecution(
        String jobName,
        String owner,
        JobStatus status,
        int processedItems,
        Instant startedAt,
        Instant finishedAt,
        TriggerSource triggerSource,
        String errorMessage) {
}
