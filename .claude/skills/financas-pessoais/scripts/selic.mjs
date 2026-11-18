#!/usr/bin/env node
// Busca a taxa Selic atual na API pública do Banco Central (SGS), sem chave de acesso.
// Uso: node selic.mjs            -> resumo em texto
//      node selic.mjs --json     -> saída JSON (para usar em código)
//
// Séries usadas:
//   432  = Meta Selic definida pelo Copom (% ao ano)
//   11   = Selic diária efetiva (% ao dia)
//   1178 = Selic efetiva anualizada, base 252 dias úteis (% ao ano)

const SERIES = { meta: 432, diaria: 11, efetivaAnual: 1178 };
const url = (cod) =>
  `https://api.bcb.gov.br/dados/serie/bcdata.sgs.${cod}/dados/ultimos/1?formato=json`;

async function buscar(cod) {
  const res = await fetch(url(cod), { signal: AbortSignal.timeout(15000) });
  if (!res.ok) throw new Error(`API do BCB respondeu ${res.status} para a série ${cod}`);
  const [ultimo] = await res.json();
  return { data: ultimo.data, valor: Number(ultimo.valor) };
}

try {
  const [meta, diaria, efetivaAnual] = await Promise.all(
    Object.values(SERIES).map(buscar)
  );
  const resultado = {
    metaSelicAA: meta.valor,
    selicEfetivaAA: efetivaAnual.valor,
    selicDiariaAD: diaria.valor,
    selicMensalEquivalente: Number(((Math.pow(1 + efetivaAnual.valor / 100, 1 / 12) - 1) * 100).toFixed(4)),
    dataReferencia: efetivaAnual.data,
    fonte: "Banco Central do Brasil - SGS (séries 432, 11 e 1178)",
  };

  if (process.argv.includes("--json")) {
    console.log(JSON.stringify(resultado, null, 2));
  } else {
    const pct = (v, casas = 2) => v.toLocaleString("pt-BR", { minimumFractionDigits: casas, maximumFractionDigits: casas }) + "%";
    console.log(`Meta Selic (Copom):        ${pct(resultado.metaSelicAA)} a.a.`);
    console.log(`Selic efetiva anualizada:  ${pct(resultado.selicEfetivaAA)} a.a. (base 252)`);
    console.log(`Selic diária:              ${pct(resultado.selicDiariaAD, 6)} a.d.`);
    console.log(`Equivalente mensal:        ${pct(resultado.selicMensalEquivalente, 4)} a.m.`);
    console.log(`Referência: ${resultado.dataReferencia} | Fonte: ${resultado.fonte}`);
  }
} catch (err) {
  console.error(`Não foi possível obter a Selic: ${err.message}`);
  process.exit(1);
}
