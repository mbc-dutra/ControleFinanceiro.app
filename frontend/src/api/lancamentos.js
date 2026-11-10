import axios from 'axios'

export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

const http = axios.create({ baseURL: API_URL })

// `mes` no formato AAAA-MM. Omitido = todos os meses.

export async function listarLancamentos(tipo, mes) {
  const params = {}
  if (tipo) params.tipo = tipo
  if (mes) params.mes = mes
  const { data } = await http.get('/lancamentos', { params })
  return data
}

export async function criarLancamento(payload) {
  const { data } = await http.post('/lancamentos', payload)
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
