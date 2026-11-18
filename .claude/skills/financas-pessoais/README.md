# Skill `financas-pessoais`

Skill do Claude Code com as regras de dinheiro deste projeto: `BigDecimal` no backend,
arredondamento `HALF_UP` só no fim, parcelas que fecham o total ao centavo e formatação em R$ pt-BR.
Ela também consulta a Selic real na API do Banco Central (`scripts/selic.mjs`). O Claude a usa
sozinho quando a tarefa mexe com valores, saldos, parcelas ou juros, ou você chama com `/financas-pessoais`.

**Usar em outro projeto:** copie esta pasta para `.claude/skills/financas-pessoais/` do seu
projeto (ou para `~/.claude/skills/` para usar em todos) e reinicie o Claude Code. Ajuste os
caminhos de arquivo citados no `SKILL.md` (`backend/`, `frontend/src/datas.js`) para o seu projeto.

| Arquivo | Conteúdo |
|---|---|
| `SKILL.md` | regras essenciais e quando disparar (carregado sempre que a skill é usada) |
| `reference.md` | fórmulas, exemplos em `BigDecimal`, séries da Selic e checklist de testes (lido sob demanda) |
| `scripts/selic.mjs` | busca a Selic atual no Banco Central (Node 18+, sem dependências) |
