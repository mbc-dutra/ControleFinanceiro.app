import { useState } from 'react'
import { CATEGORIAS_ENTRADA, CATEGORIAS_SAIDA } from '../categorias'
import { hojeISO } from '../datas'


export default function NovoLancamento({ onCriar }) {
  const [tipo, setTipo] = useState('SAIDA')
  const [descricao, setDescricao] = useState('')
  const [categoria, setCategoria] = useState(CATEGORIAS_SAIDA[0])
  const [valor, setValor] = useState('')
  const [data, setData] = useState(hojeISO)
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState(null)

  const categorias = tipo === 'SAIDA' ? CATEGORIAS_SAIDA : CATEGORIAS_ENTRADA

  function trocarTipo(novoTipo) {
    setTipo(novoTipo)
    setCategoria(novoTipo === 'SAIDA' ? CATEGORIAS_SAIDA[0] : CATEGORIAS_ENTRADA[0])
  }

  async function handleSubmit(e) {
    e.preventDefault()
    const valorNumerico = parseFloat(valor)
    if (!descricao.trim() || !valorNumerico || valorNumerico <= 0 || !data) return

    try {
      setEnviando(true)
      setErro(null)
      await onCriar({
        descricao: descricao.trim(),
        tipo,
        categoria,
        valor: valorNumerico,
        data
      })
      setDescricao('')
      setValor('')
    } catch {
      setErro('Não foi possível salvar o lançamento.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="painel">
      <h2>Novo lançamento</h2>
      <form className="form-lancamento" onSubmit={handleSubmit}>
        <div className="campo">
          <label>Tipo</label>
          <div className="tipo-toggle">
            <button
              type="button"
              className={tipo === 'SAIDA' ? 'ativo saida' : ''}
              onClick={() => trocarTipo('SAIDA')}
            >
              Saída
            </button>
            <button
              type="button"
              className={tipo === 'ENTRADA' ? 'ativo entrada' : ''}
              onClick={() => trocarTipo('ENTRADA')}
            >
              Entrada
            </button>
          </div>
        </div>

        <div className="campo campo-grow">
          <label htmlFor="descricao">Descrição</label>
          <input
            id="descricao"
            type="text"
            value={descricao}
            onChange={e => setDescricao(e.target.value)}
            placeholder="Ex.: Supermercado, Salário..."
            required
          />
        </div>

        <div className="campo">
          <label htmlFor="categoria">Categoria</label>
          <select id="categoria" value={categoria} onChange={e => setCategoria(e.target.value)}>
            {categorias.map(c => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>

        <div className="campo campo-valor">
          <label htmlFor="valor">Valor (R$)</label>
          <input
            id="valor"
            type="number"
            min="0.01"
            step="0.01"
            value={valor}
            onChange={e => setValor(e.target.value)}
            placeholder="0,00"
            required
          />
        </div>

        <div className="campo campo-data">
          <label htmlFor="data">Data</label>
          <input id="data" type="date" value={data} onChange={e => setData(e.target.value)} required />
        </div>

        <button className="botao-primario" type="submit" disabled={enviando}>
          {enviando ? 'Salvando...' : 'Lançar'}
        </button>
      </form>
      {erro && <p className="erro-form">{erro}</p>}
    </section>
  )
}
