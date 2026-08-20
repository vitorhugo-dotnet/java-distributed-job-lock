-- Carregado apenas no profile "docker" (spring.flyway.locations).
-- Da o que processar na primeira rodada da demonstracao.
INSERT INTO invoices (customer, amount, status)
SELECT 'cliente-' || LPAD(g::text, 3, '0'), (100 + g * 7)::numeric(12, 2), 'PENDING'
FROM generate_series(1, 42) AS g;
