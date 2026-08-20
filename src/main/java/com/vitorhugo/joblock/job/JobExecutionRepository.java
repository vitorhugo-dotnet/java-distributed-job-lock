package com.vitorhugo.joblock.job;

import java.util.Optional;

public interface JobExecutionRepository {

    void save(JobExecution execution);

    Optional<JobExecution> findLast(String jobName);
}
