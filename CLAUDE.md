# CLAUDE.md — controle-financeiro

## Contexto
App de controle financeiro pessoal que substitui a planilha do usuário: ele lança as contas do
mês com data de vencimento, planeja os meses seguintes (previsão), marca o que já foi pago ou
recebido e controla quem deve dinheiro a ele ("Me devem"). Usuário único, sem login (por
enquanto). Moeda única: BRL.

- `backend/` — API REST, Spring Boot 3.3 + Java 17 + Spring Data JPA + PostgreSQL 16.
  Camadas: `controller` → `service` → `repository`, DTOs como `record` em `dto/`.
- `frontend/` — React 18 + Vite 5 + axios. Chamadas HTTP só em `src/api/`.
- `docker-compose.yml` — sobe apenas o Postgres (banco/usuário/senha: `financas`).
- `docs/prompts.md` — prompts usados para gerar partes do projeto.

## Convenções
- **Dinheiro é `BigDecimal`** no backend (coluna `precision 12, scale 2`). Nunca `double`/`float`
  para valores; compare com `compareTo`/`isEqualByComparingTo`, não `equals`.
- **Domínio em português** (`Lancamento`, `descricao`, `valor`, `TipoLancamento.ENTRADA/SAIDA`);
  siga o mesmo idioma em classes, campos, endpoints e componentes novos.
- **Mês = `YearMonth` no formato `AAAA-MM`** (query param `mes`). Sem `mes` = todos os meses.
- **Realizado x previsto (`Lancamento.pago`)**: `saldo` só soma o que tem `pago = true`;
  pendentes vão para `aReceber`/`aPagar` e entram só no `saldoPrevisto`. O usuário marca
  manualmente — nunca virar `pago` automaticamente pela data. Na criação sem `pago`, a regra
  é: data futura = previsto. `data` é o vencimento (saída) ou a data prevista (entrada).
- **Repetir**: `repetirMeses` cria cópias independentes (sem vínculo entre elas); editar uma
  não altera as outras — é assim que o usuário corrige a previsão mês a mês.
- **"Me devem" (`Divida`) fica fora do saldo.** Agrupado por pessoa → `TipoDivida` → instituição
  (`banco`, texto livre; na tela aparece como "Instituição"). Dois tipos (`tipo` nulo = `DEBITO`):
  - `DEBITO` (dinheiro do usuário): ao quitar vira uma `ENTRADA` categoria `Reembolso` com data
    de hoje (`lancamentoId` guarda o vínculo; reabrir apaga essa entrada).
  - `CREDITO` (cartão, crédito, empréstimo): "recebi" (`dataRecebimento`) e "paguei a instituição"
    (`dataPagamento`) são marcados à parte e **nunca** criam lançamento; com os dois, fica quitada.
  - `data` da dívida é o **vencimento**. `parcelas` (só na criação) cria uma dívida por mês,
    "descrição (i/N)", independentes entre si (mesma ideia do `repetirMeses`).
  - Dívida em aberto aparece sempre, em qualquer mês, até ser marcada (não filtra por `mes`).
- "Hoje" no backend vem do bean `Clock` (testes usam `Clock.fixed`) — não use `LocalDate.now()` sem ele.
- **Datas no front sempre em fuso local** — use `src/datas.js`; não use `toISOString()` para "hoje".
- Erros da API saem como JSON: validação → `{campo: mensagem}`; demais → `{"mensagem": "..."}`
  (ver `ApiExceptionHandler`). Não lance exceção genérica sem passar por ele.
- Categorias ficam em `frontend/src/categorias.js` (a API aceita qualquer texto).
- Toda regra nova no `LancamentoService` ganha teste unitário com Mockito (sem subir banco).

## Comandos
```bash
# Testes do backend (não precisam de banco)
cd backend && mvn test

# Rodar tudo localmente
# Postgres 16 está instalado como serviço do Windows (sobe sozinho; banco/usuário/senha: financas).
# Alternativa em máquinas com Docker: docker compose up -d
cd backend && mvn spring-boot:run    # API em http://localhost:8080/api
cd frontend && npm install && npm run dev   # tela em http://localhost:5173

# Conferir se o front compila
cd frontend && npm run build
```

## Não faça
- Não commitar `.env`, `target/`, `node_modules/`, `dist/`.
- Não usar `curl` do Git Bash com acentos no JSON para testar a API: ele envia em cp1252 e a API
  responde 400 ("Invalid UTF-8"). Use um arquivo UTF-8 (`--data-binary @arquivo.json`) ou Node.
- O banco local tem dados reais do usuário: em testes manuais, marque registros com "TESTE" e apague-os.
- Não trocar `ddl-auto=update` por migrações sem combinar antes (Flyway é o próximo passo planejado).
- Não adicionar autenticação, multiusuário ou Open Finance sem pedido explícito.
