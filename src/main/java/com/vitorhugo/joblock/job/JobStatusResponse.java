package com.vitorhugo.joblock.job;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JobStatusResponse(
        String job,
        Instant lastRunAt,
        String lastOwner,
        String lastStatus,
        int processedItems) {

    public static JobStatusResponse from(String jobName, Optional<JobExecution> execution) {
        return execution
                .map(e -> new JobStatusResponse(jobName, e.finishedAt(), e.owner(),
                        e.status().name(), e.processedItems()))
                .orElseGet(() -> new JobStatusResponse(jobName, null, null, null, 0));
    }
}
