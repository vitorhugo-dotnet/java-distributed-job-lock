package com.vitorhugo.joblock.billing;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class JdbcInvoiceRepository implements InvoiceRepository {

    private final JdbcClient jdbcClient;

    JdbcInvoiceRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<Invoice> findPending(int limit) {
        return jdbcClient.sql("""
                        SELECT id, customer, amount, status
                        FROM invoices
                        WHERE status = 'PENDING'
                        ORDER BY id
                        LIMIT :limit
                        """)
                .param("limit", limit)
                .query(Invoice.class)
                .list();
    }

    @Override
    public int close(long invoiceId, String closedBy) {
        return jdbcClient.sql("""
                        UPDATE invoices
                        SET status = 'CLOSED', closed_at = now(), closed_by = :closedBy
                        WHERE id = :id AND status = 'PENDING'
                        """)
                .param("closedBy", closedBy)
                .param("id", invoiceId)
                .update();
    }
}
