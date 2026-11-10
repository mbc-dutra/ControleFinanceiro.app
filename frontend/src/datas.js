// Datas sempre no fuso local do usuário (toISOString() usa UTC e vira o dia às 21h no Brasil).

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
