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
