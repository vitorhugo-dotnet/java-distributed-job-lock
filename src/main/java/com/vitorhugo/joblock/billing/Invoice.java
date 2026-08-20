package com.vitorhugo.joblock.billing;

import java.math.BigDecimal;

public record Invoice(long id, String customer, BigDecimal amount, String status) {
}
