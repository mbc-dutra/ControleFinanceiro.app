import { http } from './http'

export { API_URL } from './http'

// `mes` no formato AAAA-MM. Omitido = todos os meses.

export async function listarLancamentos(tipo, mes) {
  const params = {}
  if (tipo) params.tipo = tipo
  if (mes) params.mes = mes
  const { data } = await http.get('/lancamentos', { params })
  return data
}

/** payload pode ter `repetirMeses` (1–36). Retorna a lista de lançamentos criados. */
export async function criarLancamento(payload) {
  const { data } = await http.post('/lancamentos', payload)
  return data
}

export async function atualizarLancamento(id, payload) {
  const { data } = await http.put(`/lancamentos/${id}`, payload)
  return data
}

export async function marcarPago(id, pago) {
  const { data } = await http.patch(`/lancamentos/${id}/pago`, { pago })
  return data
}

export async function excluirLancamento(id) {
  await http.delete(`/lancamentos/${id}`)
}

export async function obterResumo(mes) {
  const { data } = await http.get('/lancamentos/resumo', { params: mes ? { mes } : {} })
  return data
}

export async function obterCategorias(mes) {
  const { data } = await http.get('/lancamentos/categorias', { params: mes ? { mes } : {} })
  return data
}
