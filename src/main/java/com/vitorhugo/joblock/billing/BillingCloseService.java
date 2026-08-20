package com.vitorhugo.joblock.billing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BillingCloseService {

    private static final Logger log = LoggerFactory.getLogger(BillingCloseService.class);
    private static final int BATCH_SIZE = 500;

    private final InvoiceRepository invoiceRepository;

    BillingCloseService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Fecha as faturas pendentes.
     *
     * <p>O UPDATE condicional (WHERE status = 'PENDING') garante idempotencia mesmo
     * que duas execucoes se sobreponham - cenario possivel se o lock expirar no meio
     * de um job lento. O lock coordena; o banco e quem garante a correcao.
     *
     * @return quantidade de faturas efetivamente fechadas por esta execucao.
     */
    public int closePendingInvoices(String owner) {
        var pendentes = invoiceRepository.findPending(BATCH_SIZE);
        int fechadas = 0;
        for (Invoice invoice : pendentes) {
            fechadas += invoiceRepository.close(invoice.id(), owner);
        }
        log.info("billing-close processou {} faturas pendentes (owner={})", fechadas, owner);
        return fechadas;
    }
}
