# Checklist do Desafio Técnico — Gestão de Pedidos (Claro)

> Última atualização: ver histórico do repositório. Este documento é um
> retrato do que foi implementado e validado até o momento, organizado pelos
> requisitos e diferenciais do enunciado original.

## Backend — Obrigatórios

| Item | Status |
|---|---|
| Login com autenticação (email/senha) | ✅ Completo (evoluiu para JWT completo) |
| Endpoints (login, `GET`/`POST` pedidos, `PATCH` status, `DELETE`) com regras de negócio | ✅ Completo |
| Spring Boot Actuator (`/actuator/health`, `/metrics`, `/info`) | ✅ Completo |
| Logs estruturados (criação, mudança de status, exclusão, tentativas de login) com níveis INFO/WARN/ERROR coerentes | ✅ Completo |
| Limite de 5 pedidos, transições de status válidas, códigos HTTP 200/201/400/401/404/422 | ✅ Completo |
| CORS habilitado para `http://localhost:4200` | ✅ Completo |

## Backend — Diferenciais (ordem de prioridade do enunciado)

| # | Item | Status |
|---|---|---|
| 1 | Autenticação JWT retornada no login | ✅ Completo — e ampliado além do pedido original: cadastro de usuários (`POST /api/auth/registrar`), modelo multi-tenant (pedidos isolados por usuário), autorização (404 em vez de 403 para recurso de outro usuário) |
| 2 | Micrometer + Prometheus (`micrometer-registry-prometheus`, endpoint `/actuator/prometheus`) | ✅ Completo |
| 3 | Métricas de negócio customizadas (`pedidos_total`, `pedidos_by_status{status}`) | ✅ Completo — e ampliado: `pedidos_peso_total_gramas` e `pedidos_itens` também, alimentando o dashboard único do Grafana |

## Frontend — Telas obrigatórias

| Tela | Requisitos | Status |
|---|---|---|
| Login | Reactive Forms, validação de email, botão desabilitado se inválido, sucesso → dashboard, falha → mensagem | ✅ Completo (evoluiu para tela única de login **+ cadastro**, com seletor tipo segmented control) |
| Dashboard | Gráfico de barras por status, gráfico de pizza vs. limite máximo, mensagem de saudação | ✅ Completo |
| Listagem de pedidos | Colunas (Cliente, Itens, Peso em kg, Status, Ações), botão "Adicionar" desabilitado no limite, ações inválidas desabilitadas/ocultas, exclusão com confirmação, toast em toda ação | ✅ Completo |
| Cadastro de pedido | Botão "Voltar", validações (Cliente ≥5 caracteres, Peso numérico, Itens inteiro), `POST` com fallback em LocalStorage, sempre inicia `EM_PROCESSAMENTO`, mensagem clara no limite de 5 | ✅ Completo |

## Frontend — Diferenciais (ordem de prioridade do enunciado)

| # | Item | Status |
|---|---|---|
| 1 | Route Guard (`CanActivate`) protegendo rotas internas | ✅ Completo (`authGuard`, funcional, padrão Angular 17) |
| 2 | HTTP Interceptor anexando o token JWT automaticamente | ✅ Completo (`authInterceptor`; também trata 401 limpando sessão e redirecionando) |
| 3 | Mensagens de validação campo a campo no cadastro | ✅ Completo (login, cadastro de usuário e cadastro de pedido) |
| 4 | Cards de resumo no dashboard (total de pedidos, em processamento, peso total, itens totais) | ✅ Completo — no frontend **e** espelhado no dashboard único do Grafana |
| 5 | Badge colorido por status (verde/amarelo/vermelho) na listagem | ✅ Completo (cores semânticas próprias, independentes da paleta de marca) |
| 6 | Filtro por status e busca por nome do cliente | ✅ Completo (`MatTableDataSource`, filtro combinado status + busca) |
| 7 | Feedback de carregamento (spinner) durante envios | ✅ Completo (login, cadastro, listagem, dashboard) |
| 8 | Atualização automática dos gráficos (polling ou Observable compartilhado) | ✅ Completo — Observable compartilhado (`PedidoService.pedidos$`) + polling a cada 20s no frontend; dashboard Grafana também com `refresh: 10s` |
| 9 | Indicador de saúde da API consumindo `/actuator/health` | ✅ Completo — no frontend (`HealthService`, polling a cada 15s, indicador na toolbar) **e** no Grafana (painel com a métrica `up{job="pedidos-api"}`, que o Prometheus deriva do scrape do mesmo endpoint) |
| 10 | Paginação ou ordenação da tabela | ✅ Completo (`MatSort` + `MatPaginator` na listagem) |

## Requisitos técnicos gerais

| Item | Status |
|---|---|
| Angular 17 standalone components (com justificativa documentada) | ✅ Completo |
| Estilização (Angular Material customizado) | ✅ Completo |
| Gráficos via ng2-charts/Chart.js (com justificativa documentada) | ✅ Completo |
| Docker Compose subindo frontend + backend + MariaDB + monitoramento | ✅ Completo — um único `docker compose up --build`, sem pré-requisito externo; validado do zero (`down -v && up --build`) |
| Testes JUnit cobrindo limite de 5 e transições de status | ✅ Completo — 48 testes (`StatusPedidoTest`, `PedidoServiceTest`, `AuthServiceTest`, `JwtServiceTest`, `PedidoControllerSecurityTest`, `PedidoBuscaControllerTest` e `DashboardControllerTest` com contexto Spring completo e filtro de segurança real) |
| Testes Jasmine/Karma | ✅ Completo — 64 testes (transições, fallback offline, `authGuard`, `authInterceptor`, `AuthService`, `DashboardService`, `HealthService`, filtro/busca/paginação/ordenação da listagem via API (com debounce, incluindo regressão de paginação/tamanho de página), validação e fluxo de `LoginComponent`, cards e gráficos do `DashboardComponent` (via `GET /api/dashboard/metricas`), validação e limite de 5 no `PedidoFormComponent`) |

## Entregáveis finais

| Item | Status |
|---|---|
| Repositório Git organizado (`backend`, `frontend`, `monitoring`, `docker-compose` na raiz) | ✅ Completo — Gitflow (`main`/`develop`), Conventional Commits, release `v1.0.0` taggeada, proteção de branch na `main` (PR obrigatório, sem force-push, `enforce_admins`), tudo publicado no GitHub |
| README com instruções de execução, decisões técnicas e trade-offs | ✅ Completo (local **e** via Docker Compose) |
| README com "o que faria diferente com mais tempo" | ✅ Completo |
| `CONTRIBUTING.md` com fluxo Gitflow e Conventional Commits | ✅ Completo |

## Além do escopo original (evoluções pedidas durante a conversa)

- Migração de **H2 → MariaDB** como banco principal (H2 mantido só nos testes), depois **containerizado no Docker Compose** (`create-db-user.sh` cobre o setup manual do fluxo local sem Docker)
- **Swagger/OpenAPI** (`springdoc-openapi`) com documentação interativa
- **Postman**: collection com exemplos/mocks, environment e script `curl-examples.sh`
- **Multiusuário completo**: cadastro de usuários, isolamento de pedidos por usuário (limite de 5 por usuário, não global), autorização por JWT
- **Identidade visual da Claro**: paleta vermelho/branco própria, tipografia Manrope, estados vazio/carregando/erro tratados
- **Gitflow**: `main`/`develop`, convenção `feature/`/`release/`/`hotfix/`, Conventional Commits, proteção de branch, release `v1.0.0`
- **Observabilidade completa (stack LGTM)**: além do Prometheus (métricas), o Grafana também tem **Loki** (logs centralizados de todos os containers via Promtail) e **Tempo** (tracing distribuído via Micrometer Tracing + OTLP, com spans automáticos do Spring Security/MVC) — logs e traces correlacionados bidirecionalmente (`traceId` no log linka pro trace no Tempo, e vice-versa)
- **Um único dashboard Grafana** "Pedidos API - Visão Geral e Saúde": cards de resumo, indicador de saúde, disponibilidade e logs em tempo real, junto com as métricas técnicas (req/s por endpoint, latência, memória JVM, erros 4xx/5xx) — tudo num só lugar, sem precisar trocar de dashboard

## Resumo do que falta

Nada do escopo pedido ou dos diferenciais listados no enunciado ficou de
fora. Itens que ficaram para uma eventual continuação (não bloqueantes,
documentados em ["O que eu faria diferente com mais tempo"](README.md#o-que-eu-faria-diferente-com-mais-tempo)):

- CI (GitHub Actions) rodando testes/build a cada PR
- Testes E2E de verdade (Cypress/Playwright)
- Refresh token e cookie `httpOnly` para o JWT do frontend
