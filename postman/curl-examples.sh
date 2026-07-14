#!/usr/bin/env bash
# Exemplos de curl para todas as rotas da Pedidos API (multi-tenant + JWT).
# Espelha os requests/exemplos da collection Pedidos-API.postman_collection.json
#
# Uso: BASE_URL=http://localhost:8080 ./curl-examples.sh

set -e
BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "== Autenticacao =="

echo "-- Login - sucesso (usuario de demonstracao do seed) --"
LOGIN_RESP=$(curl -sS -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@pedidos.com","senha":"admin123"}')
echo "$LOGIN_RESP"
TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import json,sys; print(json.load(sys.stdin)['token'])")

echo "-- Login - senha invalida (401) --"
curl -sS -w '\nHTTP:%{http_code}\n' -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@pedidos.com","senha":"errada"}'

echo "-- Registrar - sucesso, ja retorna token (201) --"
curl -sS -w '\nHTTP:%{http_code}\n' -X POST "$BASE_URL/api/auth/registrar" \
  -H "Content-Type: application/json" \
  -d '{"nome":"Novo Usuario","email":"novo-usuario@teste.com","senha":"senha1234"}'

echo "-- Registrar - email ja cadastrado (409) --"
curl -sS -w '\nHTTP:%{http_code}\n' -X POST "$BASE_URL/api/auth/registrar" \
  -H "Content-Type: application/json" \
  -d '{"nome":"Novo Usuario","email":"novo-usuario@teste.com","senha":"senha1234"}'

echo "-- Registrar - senha curta (400) --"
curl -sS -w '\nHTTP:%{http_code}\n' -X POST "$BASE_URL/api/auth/registrar" \
  -H "Content-Type: application/json" \
  -d '{"nome":"Outro","email":"outro@teste.com","senha":"123"}'

echo
echo "== Pedidos (todas exigem Authorization: Bearer <token>) =="

echo "-- Sem token (401) --"
curl -sS -w '\nHTTP:%{http_code}\n' "$BASE_URL/api/pedidos"

echo "-- Listar pedidos do usuario autenticado --"
curl -sS -w '\nHTTP:%{http_code}\n' "$BASE_URL/api/pedidos" \
  -H "Authorization: Bearer $TOKEN"

echo "-- Criar pedido - sucesso (201) --"
curl -sS -w '\nHTTP:%{http_code}\n' -X POST "$BASE_URL/api/pedidos" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"displayName":"Pedido #4 - Ana Paula","itens":3,"peso":700}'

echo "-- Criar pedido - nome invalido (400) --"
curl -sS -w '\nHTTP:%{http_code}\n' -X POST "$BASE_URL/api/pedidos" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"displayName":"Ana","itens":1,"peso":300}'

echo "-- Criar pedido - limite excedido (422, se o usuario ja tiver 5 cadastrados) --"
curl -sS -w '\nHTTP:%{http_code}\n' -X POST "$BASE_URL/api/pedidos" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"displayName":"Pedido #6 - Deve Falhar","itens":1,"peso":300}'

echo "-- Alterar status - sucesso (pedido 1: EM_PROCESSAMENTO -> PAUSADO) --"
curl -sS -w '\nHTTP:%{http_code}\n' -X PATCH "$BASE_URL/api/pedidos/1/status" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"status":"PAUSADO"}'

echo "-- Alterar status - transicao invalida (422, pedido 3: CANCELADO -> PAUSADO) --"
curl -sS -w '\nHTTP:%{http_code}\n' -X PATCH "$BASE_URL/api/pedidos/3/status" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"status":"PAUSADO"}'

echo "-- Alterar status - pedido nao encontrado OU de outro usuario (404) --"
curl -sS -w '\nHTTP:%{http_code}\n' -X PATCH "$BASE_URL/api/pedidos/999/status" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"status":"PAUSADO"}'

echo "-- Excluir pedido - sucesso (204) --"
curl -sS -w '\nHTTP:%{http_code}\n' -X DELETE "$BASE_URL/api/pedidos/5" \
  -H "Authorization: Bearer $TOKEN"

echo "-- Excluir pedido - nao encontrado OU de outro usuario (404) --"
curl -sS -w '\nHTTP:%{http_code}\n' -X DELETE "$BASE_URL/api/pedidos/999" \
  -H "Authorization: Bearer $TOKEN"
