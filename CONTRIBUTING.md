# Guia de Contribuição

Este projeto segue o modelo **Gitflow** para organização de branches, e
**Conventional Commits** para mensagens de commit.

## Branches principais

- **`main`** — sempre reflete o que está em produção/estável. Protegida:
  nenhum push direto, apenas merge via `release/*` ou `hotfix/*`.
- **`develop`** — branch de integração contínua, ponto de partida de toda
  `feature/*`. Também protegida contra push direto.

## Branches de trabalho

| Tipo | Nasce de | Volta para | Quando usar |
|---|---|---|---|
| `feature/<nome-curto>` | `develop` | `develop` | Uma funcionalidade nova ou alteração de escopo (ex: `feature/filtro-status`) |
| `release/<versao>` | `develop` | `main` **e** `develop` (com tag) | Preparar uma versão para lançamento (ex: `release/1.2.0`) |
| `hotfix/<nome-curto>` | `main` | `main` **e** `develop` (com tag) | Correção urgente direto em produção (ex: `hotfix/token-expirado`) |

Regras:

- **Nunca commitar direto em `main` ou `develop`** — sempre via branch
  dedicada + merge (ou Pull Request, quando o remoto estiver configurado).
- Nomes de branch em minúsculas, com hífen separando palavras, sem
  acentos (`feature/filtro-por-status`, não `feature/Filtro_por_Status`).
- Ao finalizar uma `release/*` ou `hotfix/*`, criar uma tag de versão
  (`vX.Y.Z`) no merge para `main`.

## Conventional Commits

Toda mensagem de commit segue o formato:

```
<tipo>: <descrição curta no imperativo>

[corpo opcional explicando o "porquê", não o "o quê"]
```

Tipos usados neste projeto:

| Tipo | Quando usar |
|---|---|
| `feat` | Nova funcionalidade |
| `fix` | Correção de bug |
| `docs` | Alteração apenas de documentação (README, este arquivo, etc.) |
| `chore` | Manutenção que não altera comportamento (deps, config, build) |
| `refactor` | Reorganização de código sem mudar comportamento observável |
| `test` | Adição ou ajuste de testes, sem alterar código de produção |

Exemplos:

```
feat: adiciona filtro por status na listagem de pedidos
fix: corrige transicao invalida nao retornando 422
docs: atualiza instrucoes de execucao via Docker Compose
test: cobre limite de 5 pedidos por usuario
```

Manter esse padrão facilita gerar changelog automaticamente a partir do
histórico de commits mais adiante.

## Fluxo resumido para uma nova funcionalidade

```bash
git checkout develop
git pull
git checkout -b feature/nome-da-funcionalidade
# ... commits seguindo Conventional Commits ...
git checkout develop
git merge --no-ff feature/nome-da-funcionalidade
git branch -d feature/nome-da-funcionalidade
git push origin develop
```

## Fluxo resumido para uma release

```bash
git checkout develop
git checkout -b release/1.1.0
# ajustes finais, bump de versao, etc.
git checkout main
git merge --no-ff release/1.1.0
git tag -a v1.1.0 -m "v1.1.0"
git checkout develop
git merge --no-ff release/1.1.0
git branch -d release/1.1.0
git push origin main develop --tags
```

## Fluxo resumido para um hotfix

```bash
git checkout main
git checkout -b hotfix/nome-curto
# correcao
git checkout main
git merge --no-ff hotfix/nome-curto
git tag -a v1.1.1 -m "v1.1.1"
git checkout develop
git merge --no-ff hotfix/nome-curto
git branch -d hotfix/nome-curto
git push origin main develop --tags
```
