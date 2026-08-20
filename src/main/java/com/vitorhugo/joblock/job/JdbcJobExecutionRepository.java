package com.vitorhugo.joblock.job;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Optional;

@Repository
class JdbcJobExecutionRepository implements JobExecutionRepository {

    private final JdbcClient jdbcClient;

    JdbcJobExecutionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void save(JobExecution execution) {
        jdbcClient.sql("""
                        INSERT INTO job_execution
                            (job_name, owner, status, processed_items, started_at,
                             finished_at, trigger_source, error_message)
                        VALUES (:jobName, :owner, :status, :processedItems, :startedAt,
                                :finishedAt, :triggerSource, :errorMessage)
                        """)
                .param("jobName", execution.jobName())
                .param("owner", execution.owner())
                .param("status", execution.status().name())
                .param("processedItems", execution.processedItems())
                .param("startedAt", Timestamp.from(execution.startedAt()))
                .param("finishedAt", Timestamp.from(execution.finishedAt()))
                .param("triggerSource", execution.triggerSource().name())
                .param("errorMessage", execution.errorMessage())
                .update();
    }

    @Override
    public Optional<JobExecution> findLast(String jobName) {
        return jdbcClient.sql("""
                        SELECT job_name, owner, status, processed_items, started_at,
                               finished_at, trigger_source, error_message
                        FROM job_execution
                        WHERE job_name = :jobName
                        ORDER BY started_at DESC, id DESC
                        LIMIT 1
                        """)
                .param("jobName", jobName)
                .query((rs, rowNum) -> new JobExecution(
                        rs.getString("job_name"),
                        rs.getString("owner"),
                        JobStatus.valueOf(rs.getString("status")),
                        rs.getInt("processed_items"),
                        rs.getTimestamp("started_at").toInstant(),
                        rs.getTimestamp("finished_at").toInstant(),
                        TriggerSource.valueOf(rs.getString("trigger_source")),
                        rs.getString("error_message")))
                .optional();
    }
}
