-- Schema exigido pelo ShedLock. Uma linha por job: enquanto lock_until estiver
-- no futuro, nenhuma outra instancia consegue executar a tarefa.
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
