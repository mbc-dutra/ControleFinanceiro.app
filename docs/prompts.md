# Prompts usados com o agente

Registro dos prompts que geraram partes do projeto. Cada prompt segue a estrutura:
**contexto → tarefa → restrições → exemplo → formato de saída**.

---

## Prompt 01 — Filtro por mês (Semana 2)

**Pedido vago original:** "faz o app mostrar as coisas por mês".

**Prompt específico enviado ao agente:**

```text
Contexto: projeto controle-financeiro (leia o CLAUDE.md). Backend Spring Boot 3 / Java 17
em backend/, frontend React + Vite em frontend/. Hoje os endpoints /api/lancamentos,
/api/lancamentos/resumo e /api/lancamentos/categorias consideram TODOS os lançamentos
de todas as datas, então o "saldo" mistura meses diferentes.

Tarefa: adicionar um filtro opcional por mês nos três endpoints e um seletor de mês na tela.

Restrições:
- Parâmetro de query `mes` no formato AAAA-MM (java.time.YearMonth). Sem `mes` = comportamento atual.
- Filtrar no banco (métodos derivados do Spring Data com DataBetween), não em memória.
- Mês inválido (ex.: "2026-13") responde 400 com {"mensagem": "..."} — mesmo formato do ApiExceptionHandler.
- Não mudar o contrato dos DTOs existentes.
- Escrever teste unitário do LancamentoService com Mockito (sem subir banco).
- Frontend: seletor de mês com botões ‹ › no cabeçalho, mês atual como padrão.

Exemplo de entrada e saída esperada:
  Lançamentos: 05/10/2026 ENTRADA Renda 3000,00 | 10/11/2026 ENTRADA Renda 3000,00
               | 12/11/2026 SAIDA Moradia 1200,00
  GET /api/lancamentos/resumo?mes=2026-11
  → {"entradas": 3000.00, "saidas": 1200.00, "saldo": 1800.00}
  GET /api/lancamentos/resumo  (sem mes)
  → {"entradas": 6000.00, "saidas": 1200.00, "saldo": 4800.00}
  GET /api/lancamentos/resumo?mes=2026-13
  → 400 {"mensagem": "Parâmetro 'mes' inválido. Use o formato AAAA-MM, ex.: 2026-11"}

Formato de saída:
1. Lista dos arquivos alterados/criados com 1 linha explicando cada um.
2. O código completo de cada arquivo alterado.
3. O comando para rodar os testes e o resultado.
4. O que ficou de fora / próximos passos (máx. 3 itens).
```

**Resultado (resumo do que o agente entregou):**

| Arquivo | O que mudou |
|---|---|
| `backend/.../repository/LancamentoRepository.java` | 4 consultas novas com `DataBetween` |
| `backend/.../service/LancamentoService.java` | `listar`, `resumo` e `gastosPorCategoria` recebem `YearMonth mes` |
| `backend/.../controller/LancamentoController.java` | `@RequestParam(required = false) YearMonth mes` nos 3 GETs |
| `backend/.../exception/ApiExceptionHandler.java` | 400 com mensagem amigável para `mes` inválido |
| `backend/src/test/.../LancamentoServiceTest.java` | 4 testes (exemplo do prompt, fevereiro com 28 dias, percentual) |
| `backend/src/test/.../LancamentoControllerTest.java` | 2 testes (`mes=2026-11` → 200, `mes=2026-13` → 400) |
| `frontend/src/datas.js` | utilitários de mês/data em fuso local |
| `frontend/src/components/SeletorMes.jsx` | seletor ‹ mês › no cabeçalho |
| `frontend/src/api/lancamentos.js`, `App.jsx`, `App.css` | envio do `mes` e recarga ao trocar de mês |

`cd backend && mvn test` → **6 testes, 0 falhas**. `cd frontend && npm run build` → ok.

Bônus encontrado no caminho: o formulário usava `toISOString()` para a data de hoje, que
está em UTC — depois das 21h (horário de Brasília) o lançamento caía no dia seguinte.
Corrigido com `hojeISO()` em `datas.js`.

Próximos passos: migrações com Flyway; editar lançamento (PUT); metas de gasto por categoria.

---

## Prompt 02 — Funcionar como a minha planilha (vencimento, previsão, "me devem")

**Pedido original (do jeito que falei):** "uso uma planilha de Excel e quero as mesmas análises
no app: gastos do mês com data de vencimento, previsão de dezembro, janeiro, fevereiro...
corrijo quando muda; quando outras pessoas me devem, anoto o valor no nome delas, separado
dos meus valores; e se o salário cai dia 30, não faz sentido já somar hoje — deixa como provisão."

**Antes de gerar código, o agente devolveu 3 perguntas de decisão** (cada uma muda o modelo de dados):

| Pergunta | Decisão |
|---|---|
| O que tira um lançamento do "previsto"? | Eu marco manualmente (pago/recebido) |
| Como prever meses futuros? | Opção "repetir por N meses", cada cópia editável |
| Quando alguém me paga, mexe no meu saldo? | Sim: vira uma entrada "Reembolso" |

**Prompt específico resultante:**

```text
Contexto: controle-financeiro (leia CLAUDE.md). Hoje todo lançamento conta no saldo,
mesmo com data futura; não dá para editar; não há controle de dívidas de terceiros.

Tarefa:
1. Lancamento ganha `pago` (boolean). Resumo do mês passa a ter: entradas, saidas, saldo
   (só pagos) + aReceber, aPagar, saldoPrevisto. Sem `pago` na criação: data futura = previsto.
2. POST aceita `repetirMeses` (1–36): cria uma cópia por mês, mesmo dia (31 → último dia).
3. PUT /api/lancamentos/{id} (editar) e PATCH /api/lancamentos/{id}/pago.
4. Nova entidade Divida (pessoa, descricao, valor, data, quitada, dataQuitacao, lancamentoId)
   em /api/dividas, listada agrupada por pessoa (sem diferenciar maiúsculas) com total em aberto.
   POST /{id}/quitar cria ENTRADA "Reembolso" hoje; POST /{id}/reabrir desfaz e apaga a entrada.
5. Front: campo "Vencimento" + "Já paguei"; "Repetir por N meses"; status clicável na lista
   (Pago / A pagar / Atrasado); botão editar; seção "Me devem" separada.

Restrições: "hoje" via bean Clock (testável); dinheiro em BigDecimal; não quebrar o filtro `mes`;
testes unitários com Mockito para cada regra nova.

Exemplo (hoje = 10/11/2026):
  POST {Salário, ENTRADA, 3000, data 2026-11-30, repetirMeses 3}
  → 3 lançamentos (30/11, 30/12, 30/01), todos pago=false
  GET /resumo?mes=2026-11 → saldo não muda; aReceber = 3000; saldoPrevisto = saldo + 3000
  Dívida "João — Pizza 50" → quitar → aparece ENTRADA "João — Pizza" 50,00 em 10/11, pago=true

Formato de saída: lista de arquivos (1 linha cada), código, resultado dos testes,
teste manual ponta a ponta contra o banco, e o que ficou de fora (máx. 3 itens).
```

**Resultado:** `mvn test` → **17 testes, 0 falhas**; teste ponta a ponta contra o Postgres
local com registros "TESTE", todos apagados no final.

Ficou de fora: pagamento parcial de dívida; editar todas as cópias repetidas de uma vez;
alerta de contas vencendo nos próximos dias.
