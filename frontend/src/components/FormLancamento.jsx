import { useState } from 'react'
import { CATEGORIAS_ENTRADA, CATEGORIAS_SAIDA } from '../categorias'
import { hojeISO } from '../datas'

const OPCOES_REPETIR = [1, 2, 3, 4, 5, 6, 9, 12, 18, 24]

function estadoInicial(lancamento, mes) {
  if (lancamento) {
    return {
      tipo: lancamento.tipo,
      descricao: lancamento.descricao,
      categoria: lancamento.categoria,
      instituicao: lancamento.instituicao ?? '',
      valor: String(lancamento.valor),
      data: lancamento.data,
      pago: lancamento.pago,
      repetirMeses: 1
    }
  }
  // Ao planejar um mês futuro, a data já começa naquele mês.
  const hoje = hojeISO()
  const data = hoje.startsWith(mes) ? hoje : `${mes}-01`
  return {
    tipo: 'SAIDA',
    descricao: '',
    categoria: CATEGORIAS_SAIDA[0],
    instituicao: '',
    valor: '',
    data,
    pago: data <= hoje,
    repetirMeses: 1
  }
}

/**
 * Cria (com opção de repetir por N meses) ou edita um lançamento.
 * Se `editando` mudar, o componente pai deve trocar a `key` para reiniciar o formulário.
 */
export default function FormLancamento({ editando, mes, instituicoesConhecidas = [], onSalvar, onCancelar }) {
  const [form, setForm] = useState(() => estadoInicial(editando, mes))
  // Enquanto o usuário não mexer no "já paguei", ele acompanha a data automaticamente.
  const [pagoTocado, setPagoTocado] = useState(Boolean(editando))
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState(null)

  const saida = form.tipo === 'SAIDA'
  const listaBase = saida ? CATEGORIAS_SAIDA : CATEGORIAS_ENTRADA
  const categorias = listaBase.includes(form.categoria) ? listaBase : [...listaBase, form.categoria]

  function alterar(campo, valor) {
    setForm(f => ({ ...f, [campo]: valor }))
  }

  function trocarTipo(tipo) {
    setForm(f => ({ ...f, tipo, categoria: tipo === 'SAIDA' ? CATEGORIAS_SAIDA[0] : CATEGORIAS_ENTRADA[0] }))
  }

  function trocarData(data) {
    setForm(f => ({ ...f, data, pago: pagoTocado ? f.pago : data <= hojeISO() }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    const valor = parseFloat(form.valor)
    if (!form.descricao.trim() || !valor || valor <= 0 || !form.data) return

    const payload = {
      descricao: form.descricao.trim(),
      tipo: form.tipo,
      categoria: form.categoria,
      instituicao: form.instituicao.trim() || null,
      valor,
      data: form.data,
      pago: form.pago,
      ...(editando ? {} : { repetirMeses: Number(form.repetirMeses) })
    }

    try {
      setEnviando(true)
      setErro(null)
      await onSalvar(payload)
      if (!editando) {
        setForm(f => ({ ...f, descricao: '', valor: '', repetirMeses: 1 }))
      }
    } catch {
      setErro('Não foi possível salvar o lançamento.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className={editando ? 'painel painel-editando' : 'painel'}>
      <div className="painel-topo">
        <h2>{editando ? 'Editar lançamento' : 'Novo lançamento'}</h2>
        {editando && (
          <button type="button" className="botao-secundario" onClick={onCancelar}>Cancelar</button>
        )}
      </div>

      <form className="form-lancamento" onSubmit={handleSubmit}>
        <div className="campo">
          <label>Tipo</label>
          <div className="tipo-toggle">
            <button type="button" className={saida ? 'ativo saida' : ''} onClick={() => trocarTipo('SAIDA')}>
              Saída
            </button>
            <button type="button" className={!saida ? 'ativo entrada' : ''} onClick={() => trocarTipo('ENTRADA')}>
              Entrada
            </button>
          </div>
        </div>

        <div className="campo campo-grow">
          <label htmlFor="descricao">Descrição</label>
          <input
            id="descricao"
            type="text"
            value={form.descricao}
            onChange={e => alterar('descricao', e.target.value)}
            placeholder={saida ? 'Ex.: Aluguel, Conta de luz...' : 'Ex.: Salário, Freela...'}
            required
          />
        </div>

        <div className="campo">
          <label htmlFor="categoria">Categoria</label>
          <select id="categoria" value={form.categoria} onChange={e => alterar('categoria', e.target.value)}>
            {categorias.map(c => <option key={c} value={c}>{c}</option>)}
          </select>
        </div>

        <div className="campo">
          <label htmlFor="instituicao">Instituição</label>
          <input
            id="instituicao"
            list="instituicoes-conhecidas"
            value={form.instituicao}
            onChange={e => alterar('instituicao', e.target.value)}
            placeholder={saida ? 'Ex.: Nubank, cartão Inter...' : 'Ex.: Nubank, Caixa...'}
            maxLength={40}
          />
          <datalist id="instituicoes-conhecidas">
            {instituicoesConhecidas.map(i => <option key={i} value={i} />)}
          </datalist>
        </div>

        <div className="campo campo-valor">
          <label htmlFor="valor">Valor (R$)</label>
          <input
            id="valor"
            type="number"
            min="0.01"
            step="0.01"
            value={form.valor}
            onChange={e => alterar('valor', e.target.value)}
            placeholder="0,00"
            required
          />
        </div>

        <div className="campo campo-data">
          <label htmlFor="data">{saida ? 'Vencimento' : 'Data prevista'}</label>
          <input id="data" type="date" value={form.data} onChange={e => trocarData(e.target.value)} required />
        </div>

        {!editando && (
          <div className="campo">
            <label htmlFor="repetir">Repetir</label>
            <select id="repetir" value={form.repetirMeses} onChange={e => alterar('repetirMeses', e.target.value)}>
              {OPCOES_REPETIR.map(n => (
                <option key={n} value={n}>{n === 1 ? 'Só este mês' : `Por ${n} meses`}</option>
              ))}
            </select>
          </div>
        )}

        <label className="campo-check">
          <input
            type="checkbox"
            checked={form.pago}
            onChange={e => { setPagoTocado(true); alterar('pago', e.target.checked) }}
          />
          {saida ? 'Já paguei' : 'Já recebi'}
        </label>

        <button className="botao-primario" type="submit" disabled={enviando}>
          {enviando ? 'Salvando...' : editando ? 'Salvar' : 'Lançar'}
        </button>
      </form>

      {!editando && Number(form.repetirMeses) > 1 && (
        <p className="dica">
          Serão criados {form.repetirMeses} lançamentos, um por mês. Cada um pode ser corrigido depois separadamente.
        </p>
      )}
      {erro && <p className="erro-form">{erro}</p>}
    </section>
  )
}
