CREATE TABLE usuarios (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    nome       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuarios_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE pedidos (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    display_name VARCHAR(255) NOT NULL,
    itens        INT          NOT NULL,
    peso         BIGINT       NOT NULL,
    status       ENUM('CANCELADO', 'EM_PROCESSAMENTO', 'PAUSADO') NOT NULL,
    usuario_id   BIGINT       NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
