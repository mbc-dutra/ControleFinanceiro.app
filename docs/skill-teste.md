# Prova de disparo da skill `financas-pessoais` (Semana 3)

**Data:** 18/11/2026 · **Claude Code:** 2.1.327 · **Skill:** `.claude/skills/financas-pessoais/`

## Tarefa real
Nova funcionalidade planejada para o "Me devem": digitar o **valor total** de uma compra parcelada
em vez do valor de cada parcela. O pedido **não cita a skill**; o objetivo era ver se ela dispara sozinha.

```bash
claude -p "Na tela 'Me devem', quero poder digitar o VALOR TOTAL de uma compra parcelada \
(ex.: R\$ 100,00 em 3x) em vez do valor da parcela. Como o backend deve dividir esse total \
entre as parcelas? Me mostre o trecho de código Java do DividaService e o teste que você \
escreveria. Não edite arquivos, só responda." \
  --permission-mode plan --output-format stream-json --verbose
```

## Resultado
- No log (stream-json), o agente chamou a skill sozinho:
  `"name":"Skill","input":{"skill":"financas-pessoais"}`
- A resposta seguiu as regras da skill:
  - usou `BigDecimal`;
  - pôs a sobra de centavos na última parcela (100,00 = 33,33 + 33,33 + 33,34);
  - usou testes Mockito com `isEqualByComparingTo` e a conta escrita no comentário;
  - deixou o cálculo no backend e o front só formatando com o `fmtBRL`.
- Ele também **achou uma falha na própria skill**: arredondar cada parcela com `HALF_UP` pode deixar
  a última negativa (R$ 0,72 em 48x → 47 × 0,02 = 0,94 → última = −0,22). O `SKILL.md` foi
  corrigido para truncar (`RoundingMode.DOWN`) nas parcelas.

## Por que existe `reference.md`
O `SKILL.md` fica só com as regras que valem em toda tarefa de dinheiro e com o gatilho na
`description`. Fórmulas, exemplos de código em `BigDecimal`, séries da Selic e o checklist de
testes só fazem sentido em algumas tarefas, por isso ficam em `reference.md`. O agente só lê
esse arquivo quando precisa, e o contexto não é ocupado à toa (progressive disclosure).
