---
name: financas-pessoais
description: Regras de dinheiro do controle-financeiro (BigDecimal, arredondamento HALF_UP, parcelas que fecham o total, formatação R$ pt-BR) e consulta da Selic real no Banco Central. Use ao criar ou alterar código que calcula ou exibe valores, saldos, parcelas, juros ou rendimentos — lançamentos, resumo/saldo previsto, "Me devem" com parcelas, gráfico por categoria — e sempre que precisar da Selic/CDI atual.
allowed-tools: Read Grep Glob Bash(node .claude/skills/financas-pessoais/scripts/selic.mjs *)
---

# Finanças pessoais: regras de dinheiro do projeto

Valem para `backend/` (Java 17) e `frontend/` (React). Complementam o `CLAUDE.md`, sem repetir.

## Regras que não se negociam
1. **Backend: dinheiro é `BigDecimal`**, coluna `precision 12, scale 2`. Nunca `double`/`float`,
   nem para resultado intermediário. Compare com `compareTo` (testes: `isEqualByComparingTo`).
2. **Arredondamento só no fim** de cada operação: `setScale(2, RoundingMode.HALF_UP)`.
   Divisão sempre com escala e modo explícitos, porque `divide` sem eles lança `ArithmeticException`.
3. **Parcelas e rateios fecham o total ao centavo**: as N−1 primeiras são `total / N` truncado
   (`RoundingMode.DOWN`) e a **última** leva a sobra (R$ 100,00 em 3 = 33,33 + 33,33 + 33,34).
   Aqui não use `HALF_UP`: R$ 0,72 em 48x deixaria a última parcela negativa (−0,22).
4. **Frontend: só exibe e soma para mostrar.** Formatação com o `fmtBRL` de `src/datas.js`
   (`R$ 1.234,56`). Cálculo que importa (saldo, parcelas, juros) é feito no backend.
5. **Taxas nunca dividem por 12**: `i_mensal = (1 + i_anual)^(1/12) − 1`.
6. **Selic nunca vem da memória.** Rode o script abaixo e cite a data de referência.

```bash
node .claude/skills/financas-pessoais/scripts/selic.mjs          # resumo legível
node .claude/skills/financas-pessoais/scripts/selic.mjs --json   # JSON
```

## Quando precisar de mais detalhe
Leia [reference.md](reference.md) **só quando a tarefa pedir**:
- fórmulas (juros compostos, Price, SAC, rendimento real) com exemplo em `BigDecimal`;
- séries da Selic no Banco Central e como buscar a taxa dentro do app;
- formatação de negativos, percentuais e datas;
- checklist de testes (valores conferidos à mão e casos de borda).

## Antes de entregar
- [ ] Nenhum `double`/`float`/`parseFloat` entrou em cálculo de dinheiro no backend.
- [ ] Toda regra nova no service tem teste Mockito com a conta escrita no comentário.
- [ ] `cd backend && mvn test` passa.
