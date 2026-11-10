// Datas sempre no fuso local do usuário (toISOString() usa UTC e vira o dia às 21h no Brasil).
// Datas trafegam como texto AAAA-MM-DD, então dá para comparar com < e >.

const pad = n => String(n).padStart(2, '0')

export function hojeISO() {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export function mesAtual() {
  return hojeISO().slice(0, 7)
}

export function deslocarMes(mes, delta) {
  const [ano, m] = mes.split('-').map(Number)
  const d = new Date(ano, m - 1 + delta, 1)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`
}

const fmtMes = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' })

export function nomeDoMes(mes) {
  const [ano, m] = mes.split('-').map(Number)
  return fmtMes.format(new Date(ano, m - 1, 1))
}

/** "2026-11-30" → "30/11/26" */
export function formatarData(iso) {
  const [ano, m, d] = iso.split('-')
  return `${d}/${m}/${ano.slice(2)}`
}

export const fmtBRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
