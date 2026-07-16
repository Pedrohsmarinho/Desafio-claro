# Desafio Técnico — Gestão de Pedidos (Claro)

Sistema de gestão de pedidos de e-commerce, composto por API REST em Spring
Boot e SPA em Angular, com observabilidade completa (métricas, logs e
traces — Prometheus, Loki e Tempo, visualizados no Grafana) como
diferencial.

## Estrutura do repositório

```
/backend    -> Spring Boot 3.x (Java 17+), API REST + MariaDB
/frontend   -> Angular 17 (standalone components)
/monitoring -> configs de Prometheus/Loki/Promtail/Tempo + dashboards/provisioning do Grafana
/postman    -> collection + environment do Postman e script de curl
docker-compose.yml
create-db-user.sh -> cria o banco/usuario do MariaDB (fluxo local, sem Docker)
CONTRIBUTING.md -> fluxo de branches (Gitflow) e Conventional Commits
```

> **Nota sobre o banco de dados**: o enunciado original do desafio pede H2 em
> memória ("suficiente para o escopo do desafio"). A pedido do candidato,
> o projeto foi migrado para **MariaDB** como banco principal (mantendo H2
> apenas para os testes JUnit, que seguem rápidos e isolados). Veja a seção
> [Decisões técnicas — Backend](#decisões-técnicas--backend) para detalhes.

## Status do projeto

> Em construção. Esta seção é atualizada a cada etapa concluída.

- [x] Backend obrigatório: modelo de domínio, regras de negócio, endpoints,
      Actuator, logs estruturados, testes manuais (curl) e JUnit.
- [x] Backend migrado de H2 para MariaDB (persistência real).
- [x] Frontend obrigatório: login, dashboard, listagem, cadastro.
- [x] Validação ponta a ponta (via curl simulando o navegador + suites
      automatizadas; veja nota abaixo sobre o teste visual manual).
- [x] Swagger/OpenAPI + collection Postman com mocks.
- [x] Backend multiusuário: cadastro de usuários, JWT completo, pedidos
      isolados por usuário (autorização), limite de 5 por usuário.
- [x] Frontend: tela única de login/cadastro com seletor, `AuthService`,
      route guards e interceptor JWT.
- [x] Frontend: identidade visual própria (paleta, tipografia, componentes),
      cards de resumo, badges de status, estados vazio/carregando/erro.
- [x] Micrometer + Prometheus (`/actuator/prometheus`) e métricas de negócio
      customizadas (`pedidos_total`, `pedidos_by_status`, `pedidos_peso_total_gramas`,
      `pedidos_itens`).
- [x] Frontend: filtro por status + busca por nome, paginação/ordenação da
      tabela, indicador de saúde da API (`/actuator/health`), polling no
      dashboard.
- [x] Docker Compose completo (backend, frontend, **MariaDB**, Prometheus,
      Loki, Tempo, Grafana) — um único `docker compose up --build`, sem
      pré-requisito externo — com tracing distribuído, logs centralizados
      e um único dashboard provisionado automaticamente (negócio, saúde e
      visão técnica juntos).
- [x] Commits no Git organizados por Gitflow (`main`/`develop`,
      Conventional Commits), release `v1.0.0` taggeada, proteção de branch
      na `main` (PR obrigatório, sem force-push, `enforce_admins`), tudo
      publicado no GitHub.

## Como executar (local, sem Docker)

### Backend

Pré-requisito: um MariaDB acessível em `localhost:3306` com o schema e
usuário abaixo (ajuste via variáveis de ambiente `DB_URL`/`DB_USERNAME`/
`DB_PASSWORD` se preferir outros valores). O script `create-db-user.sh`
na raiz do repositório automatiza esse setup (requer um cliente
`mysql`/`mariadb` e acesso admin ao servidor local):

```bash
./create-db-user.sh
```

Ou manualmente:

```sql
CREATE DATABASE IF NOT EXISTS pedidos_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'pedidos_user'@'localhost' IDENTIFIED BY 'pedidos_pass';
GRANT ALL PRIVILEGES ON pedidos_db.* TO 'pedidos_user'@'localhost';
FLUSH PRIVILEGES;
```

Rode com o profile `local` (usa `application-local.yml`: MariaDB em
`localhost:3306` e um segredo de JWT de desenvolvimento, só para essa
situação — nunca usado no Docker Compose):

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

> Rodando via Docker Compose? Esse passo não é necessário — o MariaDB é
> containerizado e criado automaticamente (ver seção seguinte), e o profile
> default (`application.yml`, sem `local`) já é o usado lá dentro.

A API sobe em `http://localhost:8080`. O schema (tabela `pedidos`) é criado/
atualizado automaticamente pelo Hibernate (`ddl-auto: update`) na primeira
subida, e o seed inicial é aplicado pelo `DataSeeder` somente se a tabela
estiver vazia.

Um usuário de demonstração é populado automaticamente pelo `DataSeeder` na
primeira subida (junto com os 3 pedidos do seed) — as credenciais estão no
próprio código-fonte
(`backend/src/main/java/com/claro/desafio/pedidos/config/DataSeeder.java`),
não reproduzidas aqui de propósito (este é um repositório público).

O fluxo normal, porém, é criar sua própria conta: acesse
`http://localhost:4200`, use o seletor **"Criar conta"** na tela de login
(nome, email, senha — mínimo 8 caracteres) e você já é autenticado
automaticamente após o cadastro. Cada usuário só enxerga e manipula os
próprios pedidos. O mesmo endpoint por trás desse fluxo
(`POST /api/auth/registrar`) também pode ser chamado diretamente (ver
[Contrato da API](#contrato-da-api)), se preferir testar via curl/Postman
em vez da tela.

### Frontend

```bash
cd frontend
npm install
npm start
```

A aplicação sobe em `http://localhost:4200` e consome a API em
`http://localhost:8080`.

## Como executar (via Docker Compose)

Sem pré-requisitos externos além de criar o arquivo de segredos locais: um
único comando sobe **tudo**, incluindo o banco de dados — backend, frontend,
MariaDB e a stack completa de observabilidade (**Prometheus + Loki + Tempo +
Grafana**, o "LGTM stack" da Grafana Labs).

Primeiro, copie o template de variáveis de ambiente e gere um segredo de JWT
próprio (o `.env` nunca é commitado — ver `.gitignore`):

```bash
cp .env.example .env
# edite o .env e troque JWT_SECRET por um valor aleatório, por exemplo:
openssl rand -base64 48
```

```bash
docker compose up --build -d
```

Para começar do zero (apaga também os volumes — dados do MariaDB, Grafana,
Loki e Tempo):

```bash
docker compose down -v && docker compose up --build -d
```

O backend só inicia depois que o MariaDB reporta saudável
(`depends_on.condition: service_healthy`), então não há necessidade de
esperar manualmente ou reiniciar o backend na primeira subida.

| Serviço | URL |
|---|---|
| Frontend | http://localhost:4200 |
| Backend | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Prometheus | http://localhost:9090 |
| Loki | http://localhost:3100 (sem UI própria; consultado via Grafana) |
| Tempo | http://localhost:3200 (sem UI própria; consultado via Grafana) |
| Grafana | http://localhost:3000 (usuário `admin`, senha `admin`) |
| MariaDB | `localhost:3307` (usuário `pedidos_user`, senha `pedidos_pass`, banco `pedidos_db`) — porta `3307`, não `3306`, para não conflitar com um MariaDB local já rodando; entre containers o acesso é via o nome de serviço `mariadb:3306` |

Um único dashboard **"Pedidos API - Visão Geral e Saúde"** já vem
provisionado automaticamente no Grafana (login → Dashboards), reunindo
negócio, saúde e métricas técnicas no mesmo lugar em vez de espalhar por
telas diferentes:

- **Cards de resumo**: total de pedidos, em processamento, peso total em
  kg e itens totais (mesmas métricas de negócio dos cards do frontend).
- **Saúde da API**: indicador via a métrica `up` (que o Prometheus gera
  automaticamente a cada scrape — reflete o mesmo `/actuator/health`
  consumido no frontend), com gráfico de disponibilidade na última hora.
- **Métricas técnicas**: requisições/s por endpoint, latência média,
  memória JVM (heap) e taxa de erros 4xx/5xx.
- **Painel de logs em tempo real do backend** (via Loki/Promtail), na
  parte de baixo do dashboard.

Atualiza sozinho a cada 10s (`refresh: 10s` no próprio dashboard).

Cada log do backend carrega `traceId`/`spanId` (Micrometer Tracing), e o
Grafana está configurado para correlacionar automaticamente: no painel de
logs, qualquer linha com `traceId=...` vira um link clicável para o trace
completo no Tempo; e, a partir de um trace no Tempo, "View logs" busca os
logs daquele mesmo `traceId` no Loki. Também dá para ver esses traces
diretamente no Tempo (Explore → Tempo → buscar por `service.name=pedidos-api`).

Para derrubar tudo: `docker compose down` (os dados de Grafana/Loki/Tempo
persistem nos respectivos volumes; para limpar também:
`docker compose down -v`).

### MariaDB containerizado (e por que isso simplificou a rede)

Nas primeiras versões deste Compose, o MariaDB rodava local (fora do
Compose, por decisão de desenvolvimento), escutando só em
`127.0.0.1:3306`. Isso forçava todos os serviços que precisavam alcançá-lo
— `backend`, e por consequência `prometheus`/`loki`/`promtail`/`tempo`/
`grafana`, que precisavam se enxergar entre si e ao backend — a rodar com
`network_mode: host` (Linux-only; bridge + `host.docker.internal` não
alcançaria um serviço bindado apenas em loopback).

Com o MariaDB agora containerizado (serviço `mariadb`, com `healthcheck` e
volume nomeado `mariadb-data` para persistência), essa restrição deixou de
existir: todos os serviços voltaram à rede **bridge padrão do Compose**,
que resolve os nomes de serviço via DNS interno (`backend`, `mariadb`,
`prometheus`, `loki`, `tempo` — ver `monitoring/prometheus.yml`,
`monitoring/promtail-config.yml` e os datasources do Grafana, todos
apontando para nomes de serviço em vez de `localhost`). Isso também torna
a stack portátil para Docker Desktop (Mac/Windows), que não suporta
`network_mode: host` da mesma forma que o Docker nativo do Linux.

O único serviço que continua acessível via `localhost` a partir do host é
o `backend` (porta `8080` publicada) — porque o frontend roda no
**navegador do usuário**, fora da rede do Compose, e precisa de uma porta
publicada de verdade para chamar a API (ver `environment.ts`).

## Modelo de domínio — Pedido

| Campo         | Tipo               | Observação                                   |
|---------------|--------------------|-----------------------------------------------|
| `id`          | Long               | gerado pelo banco                             |
| `displayName` | String             | nome do cliente/pedido                        |
| `itens`       | Integer            | quantidade de itens                           |
| `peso`        | Long               | **armazenado sempre em gramas**               |
| `status`      | enum StatusPedido  | `EM_PROCESSAMENTO`, `PAUSADO`, `CANCELADO`     |

O peso é persistido e trafega pela API sempre em gramas (mesma unidade do
seed). O frontend converte a exibição para quilogramas (`pesoKg`, calculado
no `PedidoResponse`), e é responsável por converter o valor digitado pelo
usuário (em kg, mais natural para um formulário de e-commerce) para gramas
antes do `POST`.

### Máquina de transição de status

Implementada em `StatusPedido.podeTransicionarPara`, mantendo a regra de
negócio junto ao domínio (facilita testes unitários e evita duplicar a
tabela de transições em múltiplos services):

```
EM_PROCESSAMENTO -> PAUSADO
EM_PROCESSAMENTO -> CANCELADO
PAUSADO          -> CANCELADO
PAUSADO          -> EM_PROCESSAMENTO
CANCELADO        -> EM_PROCESSAMENTO
```

Qualquer transição fora dessa tabela (incluindo `CANCELADO -> PAUSADO` e
transições para o mesmo estado) retorna `422 Unprocessable Entity`.

### Limite de negócio — por usuário

Máximo de 5 pedidos cadastrados simultaneamente **por usuário** (não
global). O enunciado original do desafio não especificava o escopo desse
limite — fazia sentido em um sistema single-tenant, onde só existia um
usuário implícito. Ao evoluir para multiusuário, optei por manter o limite
de negócio por usuário (cada conta pode ter até 5 pedidos simultâneos,
independente de quantos outros usuários existam ou quantos pedidos eles
tenham), em vez de um limite global compartilhado por todos — a leitura mais
natural de "sistema de gestão de pedidos" é que o limite é uma regra da
operação de cada cliente, não uma trava artificial da plataforma inteira.
Tentativas de criação acima do limite retornam `422` e são logadas em
`WARN`, incluindo o id do usuário.

## Contrato da API

| Método | Endpoint                    | Descrição                              | Autenticação | Códigos de sucesso | Códigos de erro |
|--------|------------------------------|-----------------------------------------|--------------|---------------------|------------------|
| POST   | `/api/auth/login`            | Autentica o usuário, retorna JWT        | não          | 200                 | 400, 401         |
| POST   | `/api/auth/registrar`        | Cadastra um usuário, já retorna JWT     | não          | 201                 | 400, 409         |
| GET    | `/api/pedidos`                | Lista os pedidos do usuário autenticado | **sim**      | 200                 | 401              |
| POST   | `/api/pedidos`                | Cadastra um novo pedido para o usuário  | **sim**      | 201                 | 400, 401, 422    |
| PATCH  | `/api/pedidos/{id}/status`   | Altera o status de um pedido do usuário | **sim**      | 200                 | 400, 401, 404, 422 |
| DELETE | `/api/pedidos/{id}`          | Exclui um pedido do usuário              | **sim**      | 204                 | 401, 404         |

CORS habilitado apenas para `http://localhost:4200`.

### Autorização por usuário (multi-tenant)

Todas as rotas de `/api/pedidos/**` exigem um JWT válido (header
`Authorization: Bearer <token>`) e operam **exclusivamente** sobre os
pedidos do usuário identificado pelo token — nunca a partir de um
`usuarioId` vindo do corpo, path ou query da requisição. Isso fecha uma
falha de autorização comum (IDOR — *Insecure Direct Object Reference*): sem
essa regra, um usuário autenticado poderia manipular pedidos de outra conta
apenas adivinhando/incrementando o `id` na URL.

Quando um `id` de pedido existe mas pertence a outro usuário, a API retorna
**`404 Not Found`** (não `403 Forbidden`) — a mesma resposta usada para um
`id` que simplesmente não existe. A escolha é deliberada: `403` confirmaria
para quem está tentando o ataque que aquele recurso existe (só que não é
dele), vazando informação; `404` não distingue os dois casos.

### Documentação interativa (Swagger / OpenAPI)

Com o backend rodando, a documentação da API fica disponível em:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Especificação OpenAPI (JSON): `http://localhost:8080/v3/api-docs`

Gerada automaticamente pelo `springdoc-openapi` a partir dos controllers e
DTOs (`@Operation`/`@ApiResponses` nos endpoints), sem necessidade de manter
um arquivo `openapi.yaml` separado do código.

### Observabilidade (Actuator, Prometheus, métricas de negócio)

Com o backend rodando:

- `http://localhost:8080/actuator/health`, `/info`, `/metrics` — obrigatórios
- `http://localhost:8080/actuator/prometheus` — métricas no formato
  Prometheus (`micrometer-registry-prometheus`)

Métricas de negócio customizadas:

- **`pedidos_total`** (Counter): total de pedidos **já criados**
  historicamente, incrementado uma vez a cada `POST /api/pedidos` bem
  sucedido, em `PedidoService.criar`. É cumulativo — **não decresce** com
  exclusões, propositalmente: representa "quantos pedidos esse sistema já
  processou", não "quantos existem agora".
- **`pedidos_by_status{status}`** (Gauge, uma série por valor de
  `StatusPedido`): quantos pedidos existem **agora** em cada status,
  consultado sob demanda a cada scrape do Prometheus (`PedidoRepository
  .countByStatus`). Reflete imediatamente mudanças de status e exclusões,
  sem precisar instrumentar manualmente cada operação do service.
- **`pedidos_peso_total_gramas`** e **`pedidos_itens`** (Gauges): soma do
  peso e da quantidade de itens de todos os pedidos cadastrados agora —
  alimentam diretamente os cards de resumo do dashboard "Pedidos API -
  Visão Geral e Saúde" no Grafana (mesmo dado que os cards de resumo do
  frontend mostram, só que visto pela ótica de infraestrutura/operação).
- Todas essas métricas são **globais** (somam todos os usuários) — fazem
  sentido como indicador de saúde/uso geral da operação, ao contrário do
  limite de 5 pedidos (que é por usuário, uma regra de negócio da API, não
  uma métrica de observabilidade).

Vale notar uma armadilha do Micrometer que apareceu duas vezes ao nomear
essas métricas: nomear um `Gauge` terminado em `_total` (como
`"pedidos_total"` ou a primeira tentativa de `"pedidos_itens_total"`) faz o
`PrometheusNamingConvention` **remover** esse sufixo (reservado, por
convenção Prometheus/OpenMetrics, para métricas cumulativas do tipo
Counter) — `pedidos_total` vazaria como `pedidos`, e `pedidos_itens_total`
como `pedidos_itens`. Por isso `pedidos_total` virou um `Counter` de
verdade (faz sentido ser cumulativo mesmo), e a métrica de itens já foi
batizada direto como `pedidos_itens` (sem lutar contra a convenção para um
Gauge, que satisfaz a mesma).

### Observabilidade completa: Loki + Tempo (stack LGTM)

Além de métricas (Prometheus), o Grafana também está integrado com **logs**
(Loki) e **traces distribuídos** (Tempo), formando a stack "LGTM" (Loki,
Grafana, Tempo, Métricas) da Grafana Labs — não só o Prometheus isolado.

Tracing não estava nos requisitos obrigatórios nem nos diferenciais
listados no enunciado — é o único item deste projeto que vai além do que
foi pedido. A justificativa: métricas (Prometheus) dizem *o quê* está
acontecendo (quantas requisições, quanto tempo, quantos erros) e logs
(Loki) dizem *o quê aconteceu* em cada evento, mas nenhum dos dois
responde sozinho *por quê uma requisição específica* foi lenta ou falhou
— é essa lacuna que o tracing fecha, completando o tripé métricas + logs
+ traces sem, em nenhum momento, atrasar a entrega dos requisitos
obrigatórios (que foram implementados e validados primeiro).

- **Tracing** (`micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp`):
  o Spring instrumenta automaticamente cada requisição HTTP (incluindo os
  filtros de segurança do Spring Security) e exporta os traces via OTLP
  para o Tempo. `management.tracing.sampling.probability: 1.0` amostra
  100% das requisições — razoável para o volume deste desafio; em produção
  com tráfego real, um valor menor (ex: `0.1`) evitaria sobrecarregar o
  coletor.
- **Logs** (Promtail + Loki): o Promtail descobre os containers via o
  socket do Docker (`docker_sd_configs`) e envia os logs de cada um para o
  Loki, sem precisar listar serviço por serviço na configuração.
- **Correlação logs ↔ traces**: o padrão de log
  (`logging.pattern.console`) inclui `traceId`/`spanId` (preenchidos no MDC
  pelo Micrometer Tracing). O datasource do Loki tem um `derivedField` que
  transforma qualquer `traceId=<hex>` num link para o trace correspondente
  no Tempo; o datasource do Tempo tem `tracesToLogsV2` apontando de volta
  pro Loki — a correlação funciona nos dois sentidos.

Duas armadilhas encontradas ao montar isso, registradas para quem for
reproduzir:

1. **Tempo v3 mudou o schema de configuração**: a imagem `grafana/tempo:latest`
   hoje é a v3, que reestruturou o antigo modelo de `ingester`/`compactor`
   (existentes na v2) para uma arquitetura de "live-store" — um
   `tempo.yaml` com as seções antigas falha com
   `field ingester not found in type app.Config`. A configuração mínima
   (`server` + `distributor.receivers.otlp` + `storage.trace`) já é
   suficiente; o resto usa defaults sensatos do modo monolítico.
2. **Volume do Grafana com estado de uma subida anterior**: ao adicionar
   Loki/Tempo depois de já ter rodado o Compose só com Prometheus, o
   Grafana passou a falhar na subida com
   `Datasource provisioning error: data source not found` — não por causa
   da configuração nova, mas porque o volume `grafana-data` (SQLite)
   ainda tinha o estado da execução anterior. Um
   `docker compose down && docker volume rm <projeto>_grafana-data` (ou
   `docker compose down -v`) resolve; quem for rodar isso do zero num
   clone limpo do repositório não deve nem notar o problema.

### Postman (collection, environment, mocks e curl)

Em `/postman`:

- `Pedidos-API.postman_collection.json` — todas as rotas (autenticação,
  registro, listar, criar, alterar status, excluir), organizadas em pastas
  "Autenticação" e "Pedidos". Os requests de login/registro têm um script
  de teste que salva automaticamente o token retornado na variável de
  coleção `{{token}}`; os requests de Pedidos já usam
  `Authorization: Bearer {{token}}`. Cada request tem **exemplos salvos**
  (sucesso e os erros 400/401/404/409/422 relevantes), capturados a partir
  de respostas reais da API. Import: Postman → File → Import → selecione o
  arquivo.
- `Pedidos-API.postman_environment.json` — variáveis `baseUrl`
  (`http://localhost:8080`), `token` e `pedidoId`, para não precisar
  hardcodar a URL em cada request.
- **Mock Server**: como cada request já tem exemplos salvos, é possível
  clicar com o botão direito na collection importada → "Mock collection" e o
  Postman sobe um servidor que responde com esses exemplos — útil para o
  frontend (ou terceiros) trabalharem contra a API sem precisar do backend
  (MariaDB incluso) rodando.
- `curl-examples.sh` — os mesmos requests em `curl` puro (com
  `BASE_URL` configurável via variável de ambiente), para quem preferir
  linha de comando ou quiser colar um bloco no Postman via
  "Import → Raw text" (o Postman reconhece `curl` colado e gera o request
  automaticamente).

## Decisões técnicas — Backend

- **Java 17 / Spring Boot 3.5**: versão estável mais recente compatível com
  o range exigido pelo desafio (>= 3.5.0 no momento da geração do projeto
  via Spring Initializr).
- **MariaDB como banco principal, H2 apenas em testes**: o enunciado original
  aceitava H2 em memória, mas o candidato pediu persistência real em MariaDB.
  A troca ficou restrita à camada de configuração/driver — `pom.xml` passou a
  trazer `mariadb-java-client` no `runtime` (H2 permanece, mas com escopo
  `test`), e `application.yml` aponta para
  `jdbc:mariadb://localhost:3306/pedidos_db` (sobrescrevível por
  `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`, pensando em Docker Compose). Os testes
  que sobem contexto Spring (`PedidosApiApplicationTests`) usam
  `src/test/resources/application.yml`, que o Maven prioriza sobre o arquivo
  de `main` no classpath de teste — nenhuma anotação de profile foi
  necessária. `ddl-auto: update` continua sendo suficiente (sem Flyway/
  Liquibase) dado o escopo do desafio; dados são populados pelo `DataSeeder`
  (`CommandLineRunner`) apenas se a tabela estiver vazia.
- **Regra de transição de status no enum** (`StatusPedido`) em vez de no
  `Service`: mantém a regra de negócio como parte do próprio domínio, mais
  fácil de testar isoladamente e impossível de "esquecer" de validar em um
  novo ponto de entrada.
- **Exceções de negócio dedicadas** (`LimiteExcedidoException`,
  `TransicaoInvalidaException`, `PedidoNaoEncontradoException`,
  `CredenciaisInvalidasException`) + `@RestControllerAdvice` centralizando o
  mapeamento para os códigos HTTP exigidos (400/401/404/422), evitando
  `try/catch` espalhado pelos controllers. Cobre também exceções de
  parsing/binding do próprio Spring que não são de negócio, mas que sem
  handler dedicado cairiam no catch-all genérico e virariam 500 em vez do
  4xx correto: `MethodArgumentTypeMismatchException` (path variable/query
  param com tipo incompatível, ex. `DELETE /api/pedidos/abc` → 400) e
  `HttpRequestMethodNotSupportedException` (verbo HTTP sem handler para
  aquele path, ex. `GET /api/pedidos/abc` → 405, já que só existe
  `DELETE`/`PATCH /api/pedidos/{id}`). **Convenção estabelecida**: qualquer
  ponto que precise montar um corpo de erro usa `ErrorResponse.of(HttpStatus,
  mensagem, path)` (deriva `status`/`error` do próprio `HttpStatus`) — usado
  tanto pelo `GlobalExceptionHandler` quanto pelo `JwtAuthenticationEntryPoint`
  (que roda no filtro de segurança, antes do dispatch do Spring MVC, por isso
  não passa pelo primeiro), evitando duplicar a montagem do corpo de erro em
  mais de um lugar.
- **Evolução para multiusuário + JWT completo**: a versão inicial do
  desafio tinha um único usuário hardcoded (`app.security.admin-email`) e o
  login não emitia token de verdade (`token: null`, documentado como algo a
  implementar depois). Isso foi substituído por um modelo real: entidade
  `Usuario` (nome, email único, senha com hash BCrypt) persistida no banco,
  cadastro via `POST /api/auth/registrar` (ou pela tela de login, seletor
  "Criar conta"), e `POST /api/auth/login` retornando um JWT de verdade. O
  `DataSeeder` continua criando um usuário de demonstração na primeira
  subida (credenciais no próprio `DataSeeder.java`, não repetidas aqui —
  repositório público), para não quebrar os fluxos e a documentação já
  existentes.
- **Regra de senha: mínimo 8 caracteres**: sem exigir a combinação
  "maiúscula+número+símbolo" comum em formulários corporativos, que na
  prática empurra os usuários a padrões previsíveis (`Senha123!`) e não
  melhora muito a segurança real. Um mínimo de 8 caracteres, combinado com
  hashing BCrypt (fator de custo adaptativo, resistente a força bruta),
  cobre razoavelmente bem o risco para o escopo deste desafio. Validado no
  DTO (`RegistroRequest`) com Bean Validation, mesma abordagem usada nos
  outros campos do sistema.
- **Cadastro loga automaticamente (retorna JWT)**: escolhido em vez de
  redirecionar para uma tela de login separada — evita pedir a mesma senha
  duas vezes em sequência (o usuário acabou de digitá-la no cadastro) e
  reduz o número de passos entre "criar conta" e "usar o sistema".
- **JWT via `io.jsonwebtoken` (jjwt), assinatura HS256, expiração de 1
  hora**: o token carrega apenas `sub` (email do usuário) e `exp` — nenhum
  dado mutável (nome, senha) entra no payload, então o token não fica
  desatualizado se esses dados mudarem depois. 1 hora foi escolhido como
  meio-termo: curto o suficiente para limitar o estrago de um token vazado,
  longo o suficiente para não forçar login repetido durante o uso normal do
  sistema num desafio técnico (sem fluxo de refresh token, que ficou fora do
  escopo). O segredo de assinatura vem de `app.security.jwt-secret`, lido da
  variável de ambiente `JWT_SECRET` **sem default** no profile principal
  (`application.yml`) — a aplicação falha na subida se não for definido, em
  vez de usar silenciosamente um valor fraco/conhecido. O valor real é
  injetado via `.env` (gitignored, ver `.env.example`) no Docker Compose;
  rodando fora do Docker, o profile `local` (`application-local.yml`) tem um
  valor de conveniência só para essa situação, isolado do valor usado em
  Docker/produção. Um valor antigo desse segredo chegou a ficar hardcoded
  como default nesses arquivos em commits anteriores do histórico — foi
  removido, mas por estar em histórico público de um repositório Git, deve
  ser tratado como comprometido (nunca reaproveitado como segredo real).
- **`JwtAuthenticationFilter` + `JwtAuthenticationEntryPoint`**: um
  `OncePerRequestFilter` valida o header `Authorization: Bearer <token>`,
  carrega o `Usuario` correspondente e o define como principal no
  `SecurityContext` (usado depois via `@AuthenticationPrincipal Usuario` nos
  controllers). Token ausente, inválido, expirado, ou de um usuário que não
  existe mais: o contexto simplesmente fica vazio, e quem transforma isso em
  `401` é a regra `anyRequest().authenticated()` do `SecurityConfig`, com o
  `JwtAuthenticationEntryPoint` formatando a resposta no mesmo padrão JSON
  do `GlobalExceptionHandler`. `/api/auth/**`, `/actuator/**` e o Swagger
  continuam públicos.
- **Autorização por usuário no `PedidoService`**: todos os métodos passaram
  a receber `usuarioId` explicitamente (nunca lido de um campo do
  request/DTO) e usam `findByUsuarioId`/`findByIdAndUsuarioId`/
  `countByUsuarioId` no lugar das antigas queries globais — ver seção
  [Autorização por usuário (multi-tenant)](#autorização-por-usuário-multi-tenant)
  para o raciocínio do 404 em vez de 403.
- **Peso em gramas na API**: mantém a API consistente com o seed fornecido
  no enunciado (`"peso": 1024`); a conversão para kg é responsabilidade da
  apresentação (`PedidoResponse.pesoKg` no backend e o formulário de
  cadastro no frontend).
- **Logs estruturados via SLF4J/Logback**: `INFO` para criação, mudança de
  status, exclusão e login bem-sucedido; `WARN` para tentativas de login
  inválidas, limite de pedidos excedido e transições de status inválidas;
  `ERROR` reservado para exceções não mapeadas (handler genérico no
  `GlobalExceptionHandler`).
- **Testes**: `StatusPedidoTest` (parametrizado, 9 combinações de transição),
  `PedidoServiceTest` (Mockito; limite de 5 **por usuário**, isolamento
  entre usuários — pedido de um não aparece/edita para o outro — transições
  válidas/inválidas, pedido inexistente), `AuthServiceTest` (registro,
  email duplicado, login com credenciais corretas/incorretas),
  `JwtServiceTest` (geração/validação de token, expiração, segredo
  incorreto), `PedidoControllerSecurityTest` (contexto Spring completo +
  filtro de segurança real, sem mocks: `/api/pedidos` retorna 401 sem
  token/com token inválido/com token expirado, 422 em transição inválida
  e no limite de 5, 404 ao tentar alterar/excluir pedido de outro usuário,
  204 na exclusão bem-sucedida), `PedidoBuscaControllerTest` (filtro por
  status, busca por nome case-insensitive, combinação dos dois, paginação,
  ordenação, isolamento entre usuários no endpoint `/api/pedidos/busca`),
  `DashboardControllerTest` (contagem por status escopada ao usuário
  autenticado em `/api/dashboard/metricas`, isolamento entre usuários),
  `AuthControllerTest` (login/registro: sucesso, 401, 400 de validação,
  409 de email duplicado — via HTTP, não só a nível de serviço),
  `PedidoValidacaoControllerTest` (validação de `POST /api/pedidos`: nome
  vazio/curto, peso/itens ausentes/negativos/não numéricos, e rota
  inexistente retornando 404 no formato padronizado) e
  `PedidoControllerErroInesperadoTest` (uma exceção genérica não mapeada
  cai no handler catch-all e retorna 500 sem vazar stacktrace/detalhe
  interno para o cliente) e `GlobalExceptionHandlerTest` (o handler
  isolado, sem contexto Spring/MockMvc — cada exceção customizada e a
  genérica forçadas manualmente, conferindo status HTTP e corpo JSON
  diretamente). 83 testes no total.

### Cobertura de código (JaCoCo) e testes de cenários de erro

Configurado `jacoco-maven-plugin` no `pom.xml` (`prepare-agent` antes dos
testes, `report` gerado em `target/site/jacoco/index.html` logo depois,
na fase `test`). **Sem `jacoco:check`/threshold mínimo por enquanto** —
decisão deliberada de olhar o número real antes de definir uma meta, em
vez de travar o build num percentual arbitrário. Exclusões do relatório
(não da execução dos testes — todo o código real roda normalmente):
classe `*Application` (main, sem lógica), `SecurityConfig`/`OpenApiConfig`
(configuração pura, sem branches), `dto/**` (records simples) e
`service/exception/**` (exceções triviais, só guardam uma mensagem).

**Antes** (44 testes, cobrindo majoritariamente o caminho feliz +
algumas regras de negócio): **92.8% linhas / 90.6% instruções / 85.0%
branches**. Maior gap: `GlobalExceptionHandler` em 43% — a maioria dos
handlers de exceção nunca era exercitada via HTTP de verdade.

**Depois** (65 testes, com os cenários de erro/exception completos
descritos acima): **99.4% linhas / 99.3% instruções / 85.0% branches**.
`GlobalExceptionHandler` e `PedidoController` foram de 43%/80% para
100%. A cobertura de branches não mudou porque os poucos branches
restantes (`DataSeeder`, um `if` de "já semeado ou não") não fazem parte
do escopo de cenários de erro da API.

**Após a refatoração de camadas (Domain/Entidade/DTO, MapStruct,
`DataSeeder` movido para `PedidoService` e restrito a `@Profile("!test")`
— ver histórico do repositório)**: `GlobalExceptionHandlerTest` foi
adicionado — testes unitários que instanciam o handler diretamente (sem
`@SpringBootTest`/`MockMvc`), forçando cada exceção customizada e a
genérica (`RuntimeException`/`NullPointerException` não mapeada)
manualmente, complementando os testes de integração via controller já
existentes. `GlobalExceptionHandler` permanece em **100% de linhas**. A
cobertura **total do projeto caiu de 99.4% para ~89.4%** nessa mesma
medição — não é uma regressão dos testes, é `DataSeeder` aparecendo com
0% (consequência esperada de ter sido excluído da execução em testes via
`@Profile("!test")`; antes disso ele rodava, ainda que sem asserções
próprias, durante o `@SpringBootTest` de cada teste de controller).

**Dois gaps reais de tratamento de erro foram encontrados e corrigidos
no código (não só ajustados no teste) ao escrever esses testes**:

1. **Rota inexistente retornava 500, não 404**: `GET /api/rota-que-nao-existe`
   caía no handler catch-all genérico em vez de um 404 com o formato
   padronizado. A causa: no Spring Framework 6.1+/Boot 3.2+, uma rota sem
   handler lança `NoResourceFoundException` (não o mais antigo
   `NoHandlerFoundException`, que era o que eu tinha mapeado
   inicialmente) — descoberto empiricamente ao rodar o teste, não
   assumido. Corrigido adicionando um `@ExceptionHandler(NoResourceFoundException.class)`
   dedicado, além de configurar
   `spring.mvc.throw-exception-if-no-handler-found: true` e
   `spring.web.resources.add-mappings: false` (sem isso, a rota
   inexistente nem chegava a lançar uma exceção capturável — caía direto
   no tratamento padrão de recurso estático do Spring, com uma
   Whitelabel Error Page em HTML).
2. **Corpo de requisição malformado (ex: `"peso": "abc"` em vez de um
   número) retornava 500, não 400**: um JSON com tipo errado num campo
   causa `HttpMessageNotReadableException` durante a desserialização,
   **antes** da Bean Validation rodar — uma exceção diferente de
   `MethodArgumentNotValidException`, sem handler dedicado, caindo no
   catch-all genérico (500) em vez de um 400 (erro do cliente, não do
   servidor). Corrigido com um `@ExceptionHandler(HttpMessageNotReadableException.class)`
   dedicado.

Formato de erro já padronizado desde o início do projeto (`ErrorResponse`:
`timestamp`, `status`, `error`, `message`, `path`), usado por **todos**
os handlers de exceção sem exceção — os testes de erro não precisaram
de um formato novo.

### Testes de erro/exception no frontend

Além dos cenários já descritos nas seções de testes por componente
acima, os testes que simulam especificamente respostas de erro do
backend (via `HttpTestingController`, sem precisar do backend rodando):
login com 401 (`AuthService` propaga o erro e não grava token nenhum),
`PedidoService.alterarStatus`/`excluir` com 422/404 vindos da API real
(não do fallback local), e o tratamento desses mesmos erros na tela de
listagem (mensagem de erro via snackbar, tela continua funcional, sem
exceção não tratada). O `authInterceptor` (limpa sessão e redireciona em
qualquer 401) e as mensagens de erro em `LoginComponent`/
`PedidoFormComponent` já tinham cobertura de sessões anteriores.

## Decisões técnicas — Frontend

- **Standalone components (sem NgModules)**: Angular 17 permite dispensar
  `NgModule` em favor de componentes standalone com `imports` próprios. Optei
  por esse modelo (em vez de módulos tradicionais) porque reduz boilerplate,
  deixa explícito no topo de cada componente quais dependências ele usa, e é
  a direção recomendada pelo próprio time do Angular a partir da v17.
- **Angular Material**: escolhido para formulários, tabela, cards, snackbar
  e diálogo de confirmação — cobre a maior parte da UI exigida (login,
  tabela de pedidos, cadastro) sem precisar escrever CSS de componente do
  zero, e tem boa integração com Reactive Forms (`mat-error`, estados de
  validação).
- **ng2-charts + Chart.js** para os gráficos do dashboard (barras de
  status e pizza de limite): é a integração mais madura de Chart.js com
  Angular, com `BaseChartDirective` standalone-friendly.
- **Fallback de cadastro em LocalStorage** (`PedidoService`): ao tentar criar
  um pedido, se a chamada `POST` falhar por indisponibilidade da API (status
  HTTP `0`, isto é, erro de rede/CORS/conexão recusada — não uma resposta de
  negócio como 400/422), o pedido é salvo em `localStorage` com um id
  `local-<timestamp>` e status `EM_PROCESSAMENTO`, e a listagem/dashboard
  passam a exibi-lo mesclado com os pedidos vindos da API. Erros reais da API
  (validação, limite de 5, transição inválida) **não** acionam esse fallback
  — são propagados normalmente para o formulário exibir a mensagem correta.
  Ações subsequentes (mudar status, excluir) sobre um pedido criado
  offline são resolvidas localmente, sem tentar chamar a API para um id que
  ela não conhece. Limitação assumida: se a API cair, o limite de 5 pedidos
  passa a ser validado contra o último total conhecido em memória
  (`pedidos$`), não contra o servidor (que está inacessível).
- **Máquina de transição de status duplicada no frontend**
  (`podeTransicionar` em `pedido.model.ts`): necessária tanto para
  habilitar/desabilitar os botões de ação na listagem quanto para validar
  pedidos criados offline (fallback acima) sem depender de round-trip com o
  backend. Mantida como uma função pura simples, espelhando
  `StatusPedido#podeTransicionarPara` do backend, para reduzir o risco de
  divergência.
- **Peso digitado em kg no formulário, convertido para gramas no envio**: o
  enunciado deixou a unidade do campo do formulário em aberto
  ("salvo em gramas, exibido em kg"); optei por pedir o valor em kg no
  cadastro (mais natural para o usuário final de um sistema de e-commerce) e
  converter para gramas (`Math.round(pesoKg * 1000)`) antes do `POST`,
  mantendo a API sempre em gramas.
- **Login e cadastro na mesma tela, com `mat-button-toggle-group` como
  seletor**: em vez de duas rotas separadas, um único componente
  (`LoginComponent`) alterna entre dois `FormGroup` independentes
  (`formLogin`/`formCadastro`) trocando apenas o template exibido — evita
  navegação/reload ao alternar entre "Entrar" e "Criar conta". Optei por
  `mat-button-toggle-group` (visualmente um segmented control) em vez de
  `mat-radio-group` porque o enunciado pediu explicitamente para fugir da
  aparência de radio buttons padrão.
- **Token em `sessionStorage`, não `localStorage`**: `sessionStorage` expira
  junto com a aba/sessão do navegador, reduzindo a janela de exposição do
  token comparado a `localStorage` (que persiste indefinidamente até ser
  limpo manualmente). A alternativa genuinamente mais segura seria um cookie
  `httpOnly` setado pelo backend no login — inacessível a JavaScript e
  portanto imune a roubo via XSS, ao contrário de qualquer Web Storage, que
  qualquer script rodando na página pode ler. Não adotei essa alternativa
  neste desafio porque exigiria: (a) o backend responder com
  `Set-Cookie` em vez de JSON, (b) `withCredentials`/`allowCredentials` no
  CORS (já parcialmente configurado, mas precisaria de ajuste fino), e
  (c) proteção CSRF (cookies são enviados automaticamente pelo navegador em
  qualquer requisição para o domínio, diferente de um header `Authorization`
  que só vai se o código do frontend explicitamente adicionar) — um escopo
  maior do que o pedido aqui.
- **`AuthService` central + `authGuard` funcional + `authInterceptor`
  funcional**: seguindo o padrão do Angular 17 (`CanActivateFn`/
  `HttpInterceptorFn` em vez das classes `CanActivate`/`HttpInterceptor` do
  Angular clássico), mais simples de testar (funções puras com `inject()`) e
  sem precisar declarar providers adicionais além de
  `withInterceptors([...])`. O guard verifica localmente se o token existe e
  não expirou (decodificando o payload do JWT, sem validar assinatura — isso
  é sempre responsabilidade do backend); o interceptor anexa o header em
  toda chamada para a API e, se o backend responder `401` (token realmente
  inválido/expirado do lado do servidor, ou revogado), limpa a sessão e
  redireciona para `/login` — cobre o caso do relógio do navegador dizer que
  o token ainda vale, mas o backend já não aceitar mais.
- **Identidade visual própria — paleta da Claro (vermelho + branco)**:
  primeira versão usava uma paleta teal+laranja só para fugir do
  indigo/roxo padrão do Material; a pedido do usuário, a paleta final adota
  as cores da marca Claro (vermelho `#e4002b` predominante, branco/cinza
  claro de fundo). Como o Material exige um mapa de 14 tons (50–900 +
  A100–A700 + contraste) para calcular corretamente estados de hover/
  disabled, esses tons foram derivados por interpolação a partir do
  vermelho de marca (`$claro-red` em `styles.scss`) em vez de usar
  `mat.$red-palette` (um vermelho genérico do Material, não o tom exato da
  Claro). O cinza escuro (`mat.$grey-palette`) foi mantido como cor de
  destaque secundária para não transformar toda ação em vermelho — botões
  de ação primária usam o vermelho de marca, o resto do app permanece
  predominantemente branco/neutro. Tipografia trocada de Roboto para
  Manrope, com uma escala clara de título/subtítulo/corpo
  (`.titulo-pagina`/`.subtitulo-pagina` em vez de texto corrido uniforme);
  cantos maiores e sombras mais suaves nos cards/botões (`--raio-card`,
  `--raio-botao`, `--sombra-card` como CSS custom properties, sobrescrevendo
  os estilos padrão do `mat-mdc-card`/`mat-mdc-*-button`); cores de status
  (`--status-em-processamento`, `--status-pausado`, `--status-cancelado`)
  definidas como variáveis CSS próprias, independentes da paleta do
  Material, usadas nos badges coloridos da listagem e nos gráficos do
  dashboard — o mesmo significado de cor em qualquer lugar do app que exiba
  um status.
- **Cards de resumo no dashboard** (total de pedidos, em processamento,
  peso total, itens totais): complementam os gráficos com números diretos,
  sem precisar interpretar um gráfico para responder "quantos pedidos eu
  tenho mesmo".
- **Gráficos do dashboard (barras "por status" e pizza "vs. limite")
  consomem `GET /api/dashboard/metricas`, não recontam a lista de pedidos
  no navegador**: antes, os dois gráficos eram calculados client-side
  (`Array.filter`/`reduce` sobre `pedidos$`) — a mesma lógica de contagem
  duplicada em dois lugares (dashboard e, potencialmente, qualquer outro
  lugar que precisasse do mesmo número). Agora existe uma única query
  autoritativa no backend (`PedidoService.buscarMetricasDashboard`,
  escopada por `usuarioId`), e o frontend só exibe o que ela retorna.
  **Decisão deliberada de não ler direto do `MeterRegistry` do
  Micrometer** (que alimenta `/actuator/prometheus` e o Grafana): as
  métricas de negócio (`pedidos_total`, `pedidos_by_status`) são
  **globais** (somam todos os usuários) e, no caso de `pedidos_total`,
  **cumulativas** (um `Counter` que nunca decresce com exclusões) — nenhuma
  das duas coisas é o que o card/gráfico do usuário logado precisa mostrar
  ("quantos pedidos eu tenho *agora*"). Taguear essas métricas por usuário
  para reaproveitá-las aqui também foi descartado: cardinalidade por
  usuário num Gauge/Counter do Prometheus é um anti-pattern conhecido
  (cresce sem limite conforme a base de usuários cresce). Por isso, o
  Grafana continua respondendo "qual a saúde/uso agregado do sistema" com
  as métricas globais de sempre, e `/api/dashboard/metricas` responde
  "quantos pedidos esse usuário tem agora, por status" com uma consulta
  direta ao banco escopada por usuário — perguntas diferentes, por
  design, cada uma com a fonte de dados certa para o que ela responde.
- **Estados vazio/carregando/API indisponível tratados explicitamente**: a
  listagem e o dashboard mostram um spinner enquanto carregam, uma
  ilustração + CTA ("Cadastrar o primeiro pedido") quando não há nenhum
  pedido, e um aviso (`apiIndisponivel`, alimentado por
  `PedidoService.apiDisponivel$`) quando o último carregamento caiu no
  fallback local — em vez de simplesmente mostrar uma tabela vazia sem
  explicação.
- **Atrito do fluxo de criação**: do dashboard vazio até o primeiro pedido
  cadastrado são 2 cliques ("Cadastrar o primeiro pedido" → preencher e
  salvar); o aviso de limite de 5 atingido (tanto na listagem quanto no
  formulário) explica o "porquê" e oferece uma ação concreta ("Ver meus
  pedidos", para excluir/finalizar algo), em vez de só bloquear o botão sem
  explicação.
- **Indicador de saúde da API consumindo `/actuator/health` de verdade**:
  `HealthService` faz polling a cada 30s e expõe `status$`
  (`up`/`down`/`verificando`); um indicador (bolinha verde/amarela +
  tooltip) fica sempre visível na toolbar, independente da tela. 30s (não
  os 15s do scrape do Prometheus) porque esse indicador só precisa refletir
  um status atual para o usuário olhar de vez em quando — não alimenta
  série histórica nem alimenta alerta, então não ganha nada em checar com
  mais frequência que isso. É deliberadamente separado do aviso
  "API indisponível" da listagem: o indicador da toolbar reflete a saúde
  geral do backend (Actuator), enquanto o aviso da listagem informa algo
  mais específico — que os dados exibidos agora são os do fallback local.
- **Filtro por status + busca por nome, paginação e ordenação resolvidos no
  backend**: `GET /api/pedidos/busca?status=&busca=&page=&size=&sort=`
  (`PedidoRepository.buscar`, uma única query JPQL com `status`/`busca`
  opcionais) devolve um `Page<PedidoResponse>` já filtrado/ordenado/paginado
  — a listagem dispara uma requisição nova a cada mudança de filtro, página
  ou coluna ordenada, em vez de carregar tudo uma vez e filtrar no
  navegador. A busca por nome usa debounce de 300ms (`debounceTime` +
  `distinctUntilChanged`) para não disparar uma requisição por tecla
  digitada; mudar o status ou a página dispara na hora. O total usado para
  habilitar/desabilitar o botão "Adicionar" (limite de 5) continua vindo do
  `pedidos$` compartilhado (não filtrado) — o total da busca filtrada
  (`totalElements`) é só o que aparece na tabela/paginador, para não conectar
  o limite real a um filtro que o usuário pode mudar a qualquer momento.
  Com o limite de 5 pedidos por usuário a paginação tem pouco efeito prático
  hoje, mas fica pronta caso esse limite mude — e o pattern (parâmetros de
  busca resolvidos no banco, não em memória) é o que escalaria se o limite
  crescesse.
- **Polling no dashboard (a cada 20s) complementando o Observable
  compartilhado**: `PedidoService.pedidos$` já atualiza o dashboard
  imediatamente para qualquer ação feita na mesma aba (criar, excluir,
  mudar status). O polling adicionado cobre o caso de uma mudança vinda de
  fora dessa aba/sessão (outra aba, ou o mesmo usuário logado em outro
  dispositivo) — sem isso, os gráficos ficariam desatualizados até a
  próxima navegação para a tela.

### Unidades relativas (rem) vs. px no CSS

Espaçamento (`padding`/`margin`/`gap`), tipografia e "larguras-teto" de
containers (`max-width`/`min-width`/`flex-basis`, inclusive os pontos de
quebra de `grid-template-columns`/ícones dimensionados por `font-size`) usam
`rem` em vez de `px` — escalam de forma proporcional se o usuário aumentar a
fonte padrão do navegador (acessibilidade) ou usar zoom, em vez de manter um
valor fixo em pixels reais de tela. Para os `max-width` de containers
especificamente, optei por `rem` e não por `%`: um "teto máximo" de largura
não tem um `%` de referência útil (o elemento pai já ocupa 100%) — `rem` é o
equivalente real a "não passe de X, mas ainda assim acompanhe o zoom/fonte do
usuário".

`px` foi mantido apenas onde o valor é uma decisão puramente visual, que não
deveria escalar com zoom de fonte: bordas finas (`1px solid`), offsets de
`box-shadow`, o raio de borda dos cards/botões (`--raio-card`,
`--raio-botao`), o `border-radius: 999px` dos badges "pill" (já é um valor
grande o bastante pra forçar o arredondamento total em qualquer tamanho) e
o deslocamento mínimo (`translateY(4px)`) da animação de entrada do
formulário de login.

### Análise e correções de UX/UI

Revisão crítica do fluxo completo (login/cadastro, dashboard, listagem,
cadastro de pedido) em desktop e mobile, focada em consistência antes de
qualquer elemento novo. Dois problemas reais de layout responsivo (não
escolha estética) foram encontrados e corrigidos:

- **Toolbar sobrepondo o nome do app em telas estreitas**: os links
  (Dashboard/Pedidos), o indicador de saúde da API e "Sair" ficavam
  ilegíveis/inclicáveis por não haver `flex-wrap` nem colapso em telas
  pequenas. Corrigido escondendo os rótulos de texto (mantendo ícone +
  `matTooltip`) abaixo de `37.5rem` de largura — sem introduzir um padrão de
  navegação novo (menu hambúrguer/gaveta), só resolvendo a quebra existente.
- **Tabela de pedidos "empurrando" a página inteira no mobile**: a
  `<table>` não tinha um container com rolagem própria, então a página
  toda ficava mais larga que a viewport. Corrigido com um wrapper
  (`.tabela-wrapper`) com `overflow-x: auto` e `min-width` na tabela — agora
  só a tabela rola horizontalmente, o resto da página permanece fixo na
  largura da tela.

Outros pontos observados (estados vazio/carregando, tela de erro de login,
aba de cadastro) já seguiam um padrão consistente entre as telas
(`.estado-vazio`/`.estado-carregando`, `titulo-pagina`/`subtitulo-pagina`) e
não exigiram mudança. Nenhum ponto encontrado envolveu decisão de paleta,
tipografia ou densidade dos componentes.

### Gestão de dependências (package-lock.json)

`package-lock.json` não é versionado no repositório (está no
`.gitignore` do frontend). É uma escolha deliberada, ciente de que o mais
comum é o oposto (versionar o lock file para reprodutibilidade total de
build entre máquinas/CI): sem o lock file, a mitigação escolhida foi fixar
**versões exatas** (sem `^`/`~`) em todas as dependências do
`package.json`, reduzindo (embora não eliminando por completo, já que
sub-dependências transitivas ainda podem variar) o risco de builds
diferentes puxarem versões diferentes em máquinas diferentes.

Antes de fixar as versões, auditei se cada dependência listada era
realmente usada (`grep` por import no código-fonte). `@angular/platform-browser-dynamic`
não aparecia em nenhum import do projeto (o bootstrap é via
`bootstrapApplication`, standalone, não `platformBrowserDynamic().bootstrapModule()`)
e foi removida — mas os testes (`ng test`) quebraram logo em seguida: o
próprio builder de testes do Angular gera um arquivo de bootstrap virtual
que importa `@angular/platform-browser-dynamic/testing` internamente,
invisível a um `grep` do código-fonte. A dependência foi restaurada; fica
registrado como lição: para pacotes do próprio ecossistema Angular/CLI,
"sem import direto" não é garantia de "não utilizado".

### Revisão de qualidade (code review) — convenções estabelecidas

Passada de revisão focada em manutenibilidade (não funcionalidade nova),
seguindo as categorias padrão de code review (duplicação, nomenclatura,
complexidade, tratamento de erro, testes). Corrigido o que era mecânico e
sem risco; itens subjetivos (estilo/densidade) foram confirmados antes de
alterar. Duas convenções novas ficam registradas para uso daqui pra frente:

- **`extrairMensagemErro(err, mensagemPadrao)`** (`core/utils/http-error.util.ts`):
  utilitário único para extrair a mensagem de um erro HTTP (`error.message`
  do backend, com fallback pro `message` técnico do próprio Angular, e por
  fim um texto padrão) — usado por `LoginComponent`, `PedidoFormComponent` e
  `PedidoListComponent`, que antes reimplementavam a mesma lógica cada um a
  seu jeito (pequenas divergências entre si). Qualquer tela nova que precise
  exibir erro de uma chamada HTTP deve usar esse utilitário, não reescrever
  a extração na mão.
- **`PedidoService.limiteMaximo$`**: fonte única do limite máximo de pedidos
  no frontend, populada a partir de `GET /api/dashboard/metricas` (o valor
  real configurado no backend, `app.pedidos.limite-maximo`) assim que a
  primeira resposta chega — `LIMITE_MAXIMO_PEDIDOS` (constante estática) vira
  só o valor inicial seguro antes disso. Antes da correção, três telas
  (dashboard, listagem, cadastro) exibiam o valor estático diretamente; se o
  backend mudasse o limite configurado, o texto ficaria desatualizado
  enquanto o gráfico do dashboard (que já lia da API) mostraria o valor
  certo. Qualquer tela que precise do limite deve se inscrever em
  `pedidoService.limiteMaximo$`, não importar a constante diretamente para
  exibição.

Achado também corrigido nesta revisão (não era conhecido antes): `GET
/api/pedidos/{id-não-numérico}` e variantes (`DELETE`/`PATCH`) retornavam
500 em vez de 400/405 — ver handlers de `MethodArgumentTypeMismatchException`
e `HttpRequestMethodNotSupportedException` na seção de decisões técnicas do
backend.

## Validação realizada

O ambiente de execução usado para construir este projeto não tinha acesso a
um navegador interativo (extensão Chrome não conectada), então a validação
ponta a ponta foi feita por meios automatizados/equivalentes, não
visualmente:

- Backend: bateria de `curl` cobrindo seed, login (sucesso/falha), registro
  (sucesso com login automático, email duplicado, senha curta), listagem,
  criação (com e sem exceder o limite de 5 **por usuário**), transições
  válidas/inválidas, exclusão, acesso sem token (401), token inválido (401),
  acesso cross-user a pedido de outro usuário (404), e um preflight
  `OPTIONS` + `POST` com header `Origin` para confirmar que o CORS funciona
  como o navegador exigiria.
- Backend: 83 testes JUnit (`StatusPedidoTest`, `PedidoServiceTest` —
  incluindo isolamento entre usuários —, `AuthServiceTest`, `JwtServiceTest`,
  `PedidoControllerSecurityTest`, `PedidoBuscaControllerTest`,
  `DashboardControllerTest`, `AuthControllerTest`,
  `PedidoValidacaoControllerTest`, `PedidoControllerErroInesperadoTest` —
  contexto Spring completo, sem mocks, exceto o service mockado no teste
  de 500 — e `GlobalExceptionHandlerTest`, unitário e isolado do contexto
  Spring) rodando contra H2, com cobertura JaCoCo em 100% de linhas no
  `GlobalExceptionHandler` (ver seção dedicada acima para o número total
  do projeto e por que ele varia).
- Frontend: `ng build` (dev e produção) sem erros; 71 testes Jasmine/Karma
  (`ChromeHeadless`) cobrindo a máquina de transição de status, o fallback
  de LocalStorage do `PedidoService`, o `authGuard` (permite/bloqueia +
  redireciona), o `authInterceptor` (anexa token, reage a 401), o
  `AuthService` (sessão em `sessionStorage`, expiração de token), o
  `DashboardService` (consumo de `/api/dashboard/metricas`), o
  `HealthService` (up/down/polling), o filtro/busca/paginação/ordenação da
  listagem via `/api/pedidos/busca` (incluindo o debounce de 300ms na
  busca e a regressão de paginação/tamanho de página corrigida com dados
  mockados), a validação e o fluxo de login/cadastro do `LoginComponent`,
  os cards de resumo e os gráficos do `DashboardComponent` (agora via
  `/api/dashboard/metricas`), e a validação e o limite de 5 pedidos no
  `PedidoFormComponent`.
- Verificado que o `ng serve` já em execução recompilou automaticamente
  (watch mode) e está servindo o bundle atualizado (strings como
  `authInterceptor`/`Criar conta` confirmadas no `main.js` publicado).

**Recomendação**: antes de considerar o fluxo 100% validado, abra
`http://localhost:4200` em um navegador e percorra manualmente
login/cadastro → dashboard → listagem → cadastro de pedido → mudança de
status → exclusão → logout → tentativa de acessar `/dashboard` sem estar
logado (deve redirecionar para `/login`).

## O que eu faria diferente com mais tempo

- **Refresh token**: hoje o JWT expira em 1h e o usuário precisa logar de
  novo — um fluxo de refresh token (com rotação e revogação) evitaria isso
  sem aumentar a janela de exposição de um token de acesso de vida longa.
- **Cookie `httpOnly` em vez de `sessionStorage`**: a proteção real contra
  roubo de token via XSS, discutida na seção de decisões técnicas do
  frontend — ficou fora do escopo por exigir mudanças de CORS/CSRF.
- **Rate limiting em `/api/auth/login` e `/api/auth/registrar`**: hoje nada
  impede tentativas de força bruta contra o login ou criação em massa de
  contas; um limitador (ex: Bucket4j, ou um proxy como Nginx/API Gateway na
  frente) fecharia essa lacuna.
- **Verificação de email no cadastro**: hoje qualquer email "com formato
  válido" é aceito sem confirmar que o dono realmente tem acesso a ele.
- **Migrações versionadas (Flyway/Liquibase)**: `ddl-auto: update` é
  suficiente para o escopo do desafio, mas não seria adequado em produção —
  não há histórico/rollback de mudanças de schema.
- **Testes end-to-end de verdade (Cypress/Playwright)**: o ambiente usado
  para construir este projeto não tinha acesso a um navegador interativo,
  então a validação do frontend ficou nos testes unitários (Jasmine/Karma) +
  validação manual do backend via curl/Postman. Um suite E2E cobrindo os
  fluxos completos (login → cadastro de pedido → mudança de status →
  logout) daria mais confiança do que a combinação atual.
- **CI configurado** (GitHub Actions): rodar os testes (JUnit + Jasmine) e
  o build do Docker automaticamente a cada push/PR, aproveitando a proteção
  de branch já configurada na `main` (que hoje exige PR, mas não exige um
  check de CI passando).
- **Prints/GIF da aplicação e do dashboard Grafana no README**: o ambiente
  usado para construir este projeto não teve, neste momento, acesso a um
  navegador interativo para capturar telas. Toda a validação visual foi
  feita via `curl`, testes automatizados e consultas diretas às APIs do
  Grafana/Prometheus/Loki/Tempo (ver seções de decisões técnicas) — mas
  substituir isso por capturas reais deixaria a documentação mais concreta
  para quem for avaliar sem rodar o projeto localmente.
