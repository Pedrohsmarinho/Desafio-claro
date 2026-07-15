#!/usr/bin/env bash
# Cria o banco/usuario do MariaDB usados ao rodar o backend localmente, sem
# Docker (ver "Como executar (local, sem Docker)" no README). Quando o
# backend roda via `docker compose up`, isso ja e feito automaticamente
# pela imagem oficial do MariaDB (variaveis MARIADB_DATABASE/MARIADB_USER/
# MARIADB_PASSWORD no docker-compose.yml) - este script nao e necessario
# nesse caso.
#
# Uso: ./create-db-user.sh
# Requer um cliente `mysql` (ou `mariadb`) instalado e acesso a um servidor
# MariaDB local com um usuario admin (default: root, sem senha - ajuste via
# variaveis de ambiente abaixo se o seu servidor exigir senha).

set -euo pipefail

DB_ADMIN_USER="${DB_ADMIN_USER:-root}"
DB_ADMIN_PASSWORD="${DB_ADMIN_PASSWORD:-}"
DB_HOST="${DB_HOST:-localhost}"
DB_NAME="${DB_NAME:-pedidos_db}"
DB_USERNAME="${DB_USERNAME:-pedidos_user}"
DB_PASSWORD="${DB_PASSWORD:-pedidos_pass}"

CLIENT="mysql"
if ! command -v mysql >/dev/null 2>&1 && command -v mariadb >/dev/null 2>&1; then
  CLIENT="mariadb"
fi

ARGS=(-h "$DB_HOST" -u "$DB_ADMIN_USER")
if [ -n "$DB_ADMIN_PASSWORD" ]; then
  ARGS+=(-p"$DB_ADMIN_PASSWORD")
fi

"$CLIENT" "${ARGS[@]}" <<SQL
CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USERNAME}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USERNAME}'@'localhost';
FLUSH PRIVILEGES;
SQL

echo "Banco '${DB_NAME}' e usuario '${DB_USERNAME}'@'localhost' prontos."
