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
| 2 | Micrometer + Prometheus (`micrometer-registry-prometheus`, endpoint `/actuator/prometheus`) | ❌ Não feito |
| 3 | Métricas de negócio customizadas (`pedidos_total`, `pedidos_by_status{status}`) | ❌ Não feito |

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
| 4 | Cards de resumo no dashboard (total de pedidos, em processamento, peso total, itens totais) | ✅ Completo |
| 5 | Badge colorido por status (verde/amarelo/vermelho) na listagem | ✅ Completo (cores semânticas próprias, independentes da paleta de marca) |
| 6 | Filtro por status e busca por nome do cliente | ❌ Não feito |
| 7 | Feedback de carregamento (spinner) durante envios | ✅ Completo (login, cadastro, listagem, dashboard) |
| 8 | Atualização automática dos gráficos (polling ou Observable compartilhado) | 🟡 Parcial — usa Observable compartilhado (`PedidoService.pedidos$`), atualiza reativamente a qualquer ação CRUD; **não há polling** de fontes externas |
| 9 | Indicador de saúde da API consumindo `/actuator/health` | 🟡 Parcial — há aviso de "API indisponível" na listagem, mas inferido a partir da falha do `GET /api/pedidos`, não consumindo `/actuator/health` diretamente |
| 10 | Paginação ou ordenação da tabela | ❌ Não feito (limite de 5 pedidos por usuário reduz a necessidade prática) |

## Requisitos técnicos gerais

| Item | Status |
|---|---|
| Angular 17 standalone components (com justificativa documentada) | ✅ Completo |
| Estilização (Angular Material customizado) | ✅ Completo |
| Gráficos via ng2-charts/Chart.js (com justificativa documentada) | ✅ Completo |
| Docker Compose subindo frontend + backend + monitoramento | ❌ Não feito |
| Testes JUnit cobrindo limite de 5 e transições de status | ✅ Completo — 28 testes (`StatusPedidoTest`, `PedidoServiceTest`, `AuthServiceTest`, `JwtServiceTest`, contexto Spring) |
| Testes Jasmine/Karma | ✅ Completo — 30 testes (transições, fallback offline, `authGuard`, `authInterceptor`, `AuthService`) |

## Entregáveis finais

| Item | Status |
|---|---|
| Repositório Git organizado (`backend`, `frontend`, `monitoring`, `docker-compose` na raiz) | 🟡 Estrutura pronta, mas **nenhum commit feito ainda** (`git log` vazio) |
| README com instruções de execução, decisões técnicas e trade-offs | ✅ Completo (exceto a parte de execução via Docker, que depende do `docker-compose.yml` ainda não criado) |
| README com "o que faria diferente com mais tempo" | ✅ Completo |

## Além do escopo original (evoluções pedidas durante a conversa)

- Migração de **H2 → MariaDB** como banco principal (H2 mantido só nos testes)
- **Swagger/OpenAPI** (`springdoc-openapi`) com documentação interativa
- **Postman**: collection com exemplos/mocks, environment e script `curl-examples.sh`
- **Multiusuário completo**: cadastro de usuários, isolamento de pedidos por usuário (limite de 5 por usuário, não global), autorização por JWT
- **Identidade visual da Claro**: paleta vermelho/branco própria, tipografia Manrope, estados vazio/carregando/erro tratados

## Resumo do que falta

1. Métricas customizadas + Micrometer/Prometheus (`/actuator/prometheus`)
2. Filtro por status e busca por nome na listagem
3. Indicador de saúde da API consumindo `/actuator/health` diretamente
4. Paginação/ordenação da tabela
5. `docker-compose.yml` + stack de monitoramento (Prometheus/Grafana)
6. **Commits no Git** — todo o trabalho até agora está sem histórico de versionamento
