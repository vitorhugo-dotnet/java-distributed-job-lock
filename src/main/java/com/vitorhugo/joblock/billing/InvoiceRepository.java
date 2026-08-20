package com.vitorhugo.joblock.billing;

import java.util.List;

public interface InvoiceRepository {

    List<Invoice> findPending(int limit);

    /** @return 1 se a fatura ainda estava PENDING e foi fechada agora, 0 caso contrario. */
    int close(long invoiceId, String closedBy);
}
