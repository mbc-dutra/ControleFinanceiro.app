import { http } from './http'

/**
 * Lista agrupada por pessoa → tipo → instituição:
 * [{ pessoa, totalAberto, tipos: [{ tipo, totalAberto, totalAPagar, bancos: [{ banco, totalAberto, dividas }] }] }]
 */
export async function listarDividas() {
  const { data } = await http.get('/dividas')
  return data
}

export async function criarDivida(payload) {
  const { data } = await http.post('/dividas', payload)
  return data
}

export async function atualizarDivida(id, payload) {
  const { data } = await http.put(`/dividas/${id}`, payload)
  return data
}

export async function excluirDivida(id) {
  await http.delete(`/dividas/${id}`)
}

/** Marca como paga e cria uma entrada (Reembolso) com a data de hoje. */
export async function quitarDivida(id) {
  const { data } = await http.post(`/dividas/${id}/quitar`)
  return data
}

/** Desfaz a quitação e remove a entrada gerada. */
export async function reabrirDivida(id) {
  const { data } = await http.post(`/dividas/${id}/reabrir`)
  return data
}

/** Crédito: a pessoa pagou (não mexe no saldo). */
export async function receberDivida(id) {
  const { data } = await http.post(`/dividas/${id}/receber`)
  return data
}

export async function desfazerRecebimento(id) {
  const { data } = await http.post(`/dividas/${id}/desfazer-recebimento`)
  return data
}

/** Crédito: o usuário pagou a instituição (não mexe no saldo). */
export async function pagarDivida(id) {
  const { data } = await http.post(`/dividas/${id}/pagar`)
  return data
}

export async function desfazerPagamento(id) {
  const { data } = await http.post(`/dividas/${id}/desfazer-pagamento`)
  return data
}
