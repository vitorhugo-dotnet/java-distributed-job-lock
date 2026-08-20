package com.vitorhugo.joblock.job;

import com.vitorhugo.joblock.support.PostgresIntegrationTest;
import com.vitorhugo.joblock.support.ShedLockFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.instance-id=instance-2",
        "job.billing-close.lock-at-least-for=PT0S",
        "job.billing-close.cron=-"
})
class JobControllerTest extends PostgresIntegrationTest {

    @Autowired
    MockMvc mockMvc;

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
                SELECT 'cliente-' || g, 10.00, 'PENDING' FROM generate_series(1, 3) AS g
                """).update();
    }

    @Test
    void statusSemExecucaoDeveResponderCamposNulos() throws Exception {
        mockMvc.perform(get("/jobs/billing-close/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job").value("billing-close"))
                .andExpect(jsonPath("$.lastRunAt").doesNotExist())
                .andExpect(jsonPath("$.lastOwner").doesNotExist())
                .andExpect(jsonPath("$.lastStatus").doesNotExist())
                .andExpect(jsonPath("$.processedItems").value(0));
    }

    @Test
    void runNowDeveExecutarEDevolverOResultado() throws Exception {
        mockMvc.perform(post("/jobs/billing-close/run-now"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job").value("billing-close"))
                .andExpect(jsonPath("$.lastOwner").value("instance-2"))
                .andExpect(jsonPath("$.lastStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.processedItems").value(3));
    }

    @Test
    void runNowDeveResponderSkipQuandoOutraInstanciaSeguraOLock() throws Exception {
        shedLock.segurarPorOutraInstancia("billing-close", "instance-1");

        mockMvc.perform(post("/jobs/billing-close/run-now"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastStatus").value("SKIPPED_LOCK_NOT_ACQUIRED"))
                .andExpect(jsonPath("$.processedItems").value(0));
    }

    @Test
    void statusDeveRefletirAUltimaExecucao() throws Exception {
        mockMvc.perform(post("/jobs/billing-close/run-now")).andExpect(status().isOk());

        mockMvc.perform(get("/jobs/billing-close/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastOwner").value("instance-2"))
                .andExpect(jsonPath("$.lastStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.lastRunAt").isNotEmpty())
                .andExpect(jsonPath("$.processedItems").value(3));
    }
}
