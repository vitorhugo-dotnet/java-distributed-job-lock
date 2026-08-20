package com.vitorhugo.joblock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param lockAtMostFor  teto de seguranca: se a instancia morrer sem liberar o lock,
 *                       ele expira depois desse tempo. Precisa ser maior que a pior
 *                       duracao esperada do job.
 * @param lockAtLeastFor piso: segura o lock por esse tempo mesmo se o job terminar
 *                       instantaneamente, evitando que relogios levemente
 *                       dessincronizados deixem duas instancias rodarem em sequencia.
 */
@ConfigurationProperties(prefix = "job.billing-close")
public record BillingCloseProperties(
        String name,
        String cron,
        Duration lockAtMostFor,
        Duration lockAtLeastFor) {
}
