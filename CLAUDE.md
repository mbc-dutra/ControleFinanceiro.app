# CLAUDE.md — controle-financeiro

## Contexto
App de controle financeiro pessoal: o usuário registra entradas e saídas, vê o saldo do mês
e quanto gastou por categoria. Usuário único, sem login (por enquanto). Público: pessoas que
hoje anotam gastos em caderno ou planilha. Moeda única: BRL.

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
docker compose up -d                 # Postgres na porta 5432
cd backend && mvn spring-boot:run    # API em http://localhost:8080/api
cd frontend && npm install && npm run dev   # tela em http://localhost:5173

# Conferir se o front compila
cd frontend && npm run build
```

## Não faça
- Não commitar `.env`, `target/`, `node_modules/`, `dist/`.
- Não trocar `ddl-auto=update` por migrações sem combinar antes (Flyway é o próximo passo planejado).
- Não adicionar autenticação, multiusuário ou Open Finance sem pedido explícito.
