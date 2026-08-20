package com.vitorhugo.joblock.support;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Manipula a tabela {@code shedlock} do jeito que a realidade permite.
 *
 * <p>Dois detalhes que os testes precisam respeitar:
 *
 * <ul>
 *   <li>Com {@code usingDbTime()}, o ShedLock grava e compara os timestamps em UTC
 *       ({@code timezone('utc', CURRENT_TIMESTAMP)}). Usar {@code now()} puro numa
 *       sessao em UTC-3 produziria um "futuro" tres horas no passado.
 *   <li>O ShedLock guarda em memoria (LockRecordRegistry) quais locks ja teve linha
 *       inserida e, a partir dai, so faz UPDATE. Apagar a linha por fora deixa o
 *       provider num estado impossivel em producao, onde a linha nunca e removida -
 *       um lock liberado e apenas uma linha com {@code lock_until} no passado.
 * </ul>
 */
@Component
public class ShedLockFixture {

    private final JdbcClient jdbcClient;

    ShedLockFixture(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /** Deixa o lock livre para a proxima aquisicao, como apos um job terminar. */
    public void liberar(String lockName) {
        jdbcClient.sql("""
                        UPDATE shedlock
                        SET lock_until = timezone('utc', now()) - interval '1 second'
                        WHERE name = :name
                        """)
                .param("name", lockName)
                .update();
    }

    /** Simula outra instancia viva segurando o lock agora. */
    public void segurarPorOutraInstancia(String lockName, String owner) {
        jdbcClient.sql("""
                        INSERT INTO shedlock (name, lock_until, locked_at, locked_by)
                        VALUES (:name,
                                timezone('utc', now()) + interval '5 minutes',
                                timezone('utc', now()),
                                :owner)
                        ON CONFLICT (name) DO UPDATE
                            SET lock_until = EXCLUDED.lock_until,
                                locked_at  = EXCLUDED.locked_at,
                                locked_by  = EXCLUDED.locked_by
                        """)
                .param("name", lockName)
                .param("owner", owner)
                .update();
    }

    /** Simula uma instancia que morreu segurando o lock e cujo prazo ja venceu. */
    public void expirarLockDeInstanciaMorta(String lockName, String owner) {
        jdbcClient.sql("""
                        UPDATE shedlock
                        SET locked_by  = :owner,
                            locked_at  = timezone('utc', now()) - interval '10 minutes',
                            lock_until = timezone('utc', now()) - interval '5 minutes'
                        WHERE name = :name
                        """)
                .param("name", lockName)
                .param("owner", owner)
                .update();
    }

    public String donoAtualDoLock(String lockName) {
        return jdbcClient.sql("SELECT locked_by FROM shedlock WHERE name = :name")
                .param("name", lockName)
                .query(String.class)
                .single();
    }
}
