# Referência — financas-pessoais

Material detalhado da skill. O `SKILL.md` tem só as regras e aponta para cá quando necessário.

## 1. BigDecimal na prática (backend)

```java
// Construir: sempre a partir de String ou de outro BigDecimal
BigDecimal valor = new BigDecimal("12.34");      // certo
BigDecimal errado = new BigDecimal(12.34);       // errado: 12.339999999999999857891452847979962825775146484375

// Dividir: sempre com escala e arredondamento
BigDecimal parcela = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);

// Comparar: compareTo, não equals (equals considera a escala: 1.0 != 1.00)
if (saldo.compareTo(BigDecimal.ZERO) < 0) { ... }

// Somar uma lista
BigDecimal soma = itens.stream().map(Item::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
```

Percentual (como em `gastosPorCategoria`): multiplique por 100 **antes** de dividir, com
escala 1 e `HALF_UP`. Só converta para `double` no DTO de saída, quando o valor for só para exibir.

## 2. Parcelas que fecham o total

```java
/** R$ 100,00 em 3 → [33.33, 33.33, 33.34] */
static List<BigDecimal> dividirEmParcelas(BigDecimal total, int n) {
    BigDecimal base = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
    BigDecimal ultima = total.subtract(base.multiply(BigDecimal.valueOf(n - 1)));
    List<BigDecimal> parcelas = new ArrayList<>(Collections.nCopies(n - 1, base));
    parcelas.add(ultima);
    return parcelas;
}
```

No projeto, hoje `DividaRequest.parcelas` e `LancamentoRequest.repetirMeses` **repetem** o
mesmo `valor` em cada mês (valor da parcela, não o total). Se surgir a opção "informar o total",
use a função acima e mantenha as parcelas independentes, como já é a regra do `CLAUDE.md`.

## 3. Fórmulas

| Uso | Fórmula |
|---|---|
| Juros compostos | `M = C × (1 + i)^n` |
| Taxa anual → mensal | `i_m = (1 + i_a)^(1/12) − 1` (nunca `i_a / 12`) |
| Taxa anual → diária útil (Selic/CDI) | `i_d = (1 + i_a)^(1/252) − 1` |
| Parcela Price | `PMT = PV × i / (1 − (1 + i)^−n)` |
| SAC | amortização = `PV / n`; juros = saldo devedor × i; parcela = amortização + juros |
| Rendimento real | `(1 + nominal) / (1 + inflação) − 1` |

Expoente fracionário não existe em `BigDecimal`: calcule a **taxa** com `Math.pow` (taxa pode ser
`double`), depois converta com `BigDecimal.valueOf(taxa)` e aplique ao dinheiro em `BigDecimal`,
arredondando só no fim.

## 4. Selic (Banco Central, API SGS, sem chave)

| Série | O que é | Uso |
|---|---|---|
| 432 | Meta Selic do Copom (% a.a.) | exibir "Selic atual" |
| 1178 | Selic efetiva anualizada, base 252 (% a.a.) | **cálculo de rendimento** |
| 11 | Selic diária (% a.d.) | acumular dia a dia |

URL: `https://api.bcb.gov.br/dados/serie/bcdata.sgs.{codigo}/dados/ultimos/1?formato=json`
→ `[{"data":"18/11/2026","valor":"13.65"}]` (valor vem como **texto** com ponto).

Se for implementar a busca dentro do app:
- busque no **backend** (não no React, para não depender de CORS do BCB) e guarde o último valor
  com a data de referência;
- se a API falhar, mostre o último valor salvo **com a data dele**, nunca um número fixo no código;
- sempre exiba a data de referência junto da taxa;
- nos testes, use um valor simulado (mock do cliente HTTP), nunca a API real.

## 5. Formatação (pt-BR)
- Moeda: `fmtBRL.format(v)` de `frontend/src/datas.js` → `R$ 1.234,56`.
- Negativos: `-R$ 50,00`, nunca entre parênteses.
- Percentual: `13,75% a.a.` (sempre diga a base: `a.a.`, `a.m.`, `a.d.`).
- Datas: `dd/mm/aaaa` na tela; trafegam como `AAAA-MM-DD`; fuso local (ver `CLAUDE.md`).

## 6. Testes
- Teste unitário com Mockito, sem banco (padrão de `LancamentoServiceTest`/`DividaServiceTest`).
- Escreva a conta no comentário do teste, conferida à mão:
  ```java
  // 100,00 / 3 = 33,333... → 33,33 + 33,33 + (100,00 − 66,66) = 33,34
  ```
- Casos de borda obrigatórios: valor zero (quando permitido), 1 parcela, taxa 0%, centavos que
  não dividem exato, último dia do mês (31 → 30/28) nas cópias mensais.
- Datas de "hoje" via `Clock.fixed`, nunca `LocalDate.now()` direto.
