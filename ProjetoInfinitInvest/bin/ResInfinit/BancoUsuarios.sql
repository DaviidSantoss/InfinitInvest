

-- Tabela usuários
CREATE TABLE IF NOT EXISTS usuarios (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    senha_hash TEXT NOT NULL,
    fotoPerfil BLOB
);

-- Tabela sessão
CREATE TABLE IF NOT EXISTS sessao (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS ativos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id INTEGER NOT NULL,
    categoria TEXT NOT NULL,
    ativo TEXT NOT NULL,
    quantidade REAL NOT NULL,
    preco_medio REAL NOT NULL,
    preco_atual REAL NOT NULL,
    variacao REAL NOT NULL,
    saldo REAL NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

