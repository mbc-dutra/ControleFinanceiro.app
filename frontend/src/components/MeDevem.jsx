import { useEffect, useState } from 'react'
import {
  listarDividas, criarDivida, atualizarDivida, excluirDivida, quitarDivida, reabrirDivida
} from '../api/dividas'
import { fmtBRL, formatarData, hojeISO } from '../datas'

function FormDivida({ editando, pessoasConhecidas, onSalvar, onCancelar }) {
  const [form, setForm] = useState(() => editando
    ? { pessoa: editando.pessoa, descricao: editando.descricao, valor: String(editando.valor), data: editando.data }
    : { pessoa: '', descricao: '', valor: '', data: hojeISO() })
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState(null)

  const alterar = (campo, valor) => setForm(f => ({ ...f, [campo]: valor }))

  async function handleSubmit(e) {
    e.preventDefault()
    const valor = parseFloat(form.valor)
    if (!form.pessoa.trim() || !form.descricao.trim() || !valor || valor <= 0) return
    try {
      setEnviando(true)
      setErro(null)
      await onSalvar({ ...form, pessoa: form.pessoa.trim(), descricao: form.descricao.trim(), valor })
      if (!editando) setForm(f => ({ ...f, descricao: '', valor: '' }))
    } catch {
      setErro('Não foi possível salvar.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <>
      <form className="form-lancamento" onSubmit={handleSubmit}>
        <div className="campo">
          <label htmlFor="d-pessoa">Quem deve</label>
          <input
            id="d-pessoa"
            list="pessoas-conhecidas"
            value={form.pessoa}
            onChange={e => alterar('pessoa', e.target.value)}
            placeholder="Nome"
            maxLength={80}
            required
          />
          <datalist id="pessoas-conhecidas">
            {pessoasConhecidas.map(p => <option key={p} value={p} />)}
          </datalist>
        </div>
        <div className="campo campo-grow">
          <label htmlFor="d-desc">O que é</label>
          <input
            id="d-desc"
            value={form.descricao}
            onChange={e => alterar('descricao', e.target.value)}
            placeholder="Ex.: Pizza de sexta, Ingresso do show..."
            required
          />
        </div>
        <div className="campo campo-valor">
          <label htmlFor="d-valor">Valor (R$)</label>
          <input
            id="d-valor"
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
          <label htmlFor="d-data">Desde</label>
          <input id="d-data" type="date" value={form.data} onChange={e => alterar('data', e.target.value)} required />
        </div>
        <button className="botao-primario" type="submit" disabled={enviando}>
          {enviando ? 'Salvando...' : editando ? 'Salvar' : 'Adicionar'}
        </button>
        {editando && <button type="button" className="botao-secundario" onClick={onCancelar}>Cancelar</button>}
      </form>
      {erro && <p className="erro-form">{erro}</p>}
    </>
  )
}

/**
 * "Me devem": separado dos valores do usuário. Não entra no saldo até ser quitado;
 * ao quitar, vira uma entrada (Reembolso) no mês atual — por isso avisa o pai via onQuitacao.
 */
export default function MeDevem({ onQuitacao }) {
  const [pessoas, setPessoas] = useState([])
  const [mostrarQuitadas, setMostrarQuitadas] = useState(false)
  const [editando, setEditando] = useState(null)
  const [erro, setErro] = useState(null)
  const [versao, setVersao] = useState(0)

  useEffect(() => {
    let ativo = true
    listarDividas()
      .then(dados => { if (ativo) { setPessoas(dados); setErro(null) } })
      .catch(() => { if (ativo) setErro('Não foi possível carregar quem te deve.') })
    return () => { ativo = false }
  }, [versao])

  const recarregar = () => setVersao(v => v + 1)
  const totalGeral = pessoas.reduce((soma, p) => soma + Number(p.totalAberto), 0)
  const visiveis = pessoas
    .map(p => ({ ...p, dividas: mostrarQuitadas ? p.dividas : p.dividas.filter(d => !d.quitada) }))
    .filter(p => p.dividas.length > 0)
  const temQuitadas = pessoas.some(p => p.dividas.some(d => d.quitada))

  async function salvar(payload) {
    if (editando) {
      await atualizarDivida(editando.id, payload)
      if (editando.quitada) onQuitacao()
      setEditando(null)
    } else {
      await criarDivida(payload)
    }
    recarregar()
  }

  async function executar(acao, mudaSaldo) {
    try {
      await acao()
      recarregar()
      if (mudaSaldo) onQuitacao()
    } catch {
      setErro('Não foi possível concluir a ação.')
    }
  }

  function quitar(d) {
    const ok = window.confirm(
      `${d.pessoa} te pagou ${fmtBRL.format(d.valor)} (${d.descricao})?\n\n` +
      'Isso cria uma entrada "Reembolso" com a data de hoje no seu controle.')
    if (ok) executar(() => quitarDivida(d.id), true)
  }

  function reabrir(d) {
    const ok = window.confirm('Desfazer a quitação? A entrada de reembolso criada será removida.')
    if (ok) executar(() => reabrirDivida(d.id), true)
  }

  function excluir(d) {
    const aviso = d.quitada
      ? 'Excluir este registro? A entrada de reembolso que ele gerou continua no seu controle.'
      : `Excluir "${d.descricao}" de ${d.pessoa}?`
    if (window.confirm(aviso)) executar(() => excluirDivida(d.id), false)
  }

  return (
    <section className="painel painel-devem">
      <div className="painel-topo">
        <h2>Me devem</h2>
        <span className="total-devem">Total em aberto: <strong>{fmtBRL.format(totalGeral)}</strong></span>
      </div>
      <p className="dica">Fica separado do seu saldo. Quando a pessoa pagar, clique em “Recebi” e o valor entra como reembolso no mês.</p>

      <FormDivida
        key={editando?.id ?? 'nova'}
        editando={editando}
        pessoasConhecidas={pessoas.map(p => p.pessoa)}
        onSalvar={salvar}
        onCancelar={() => setEditando(null)}
      />

      {erro && <p className="erro-form">{erro}</p>}

      {visiveis.length === 0 ? (
        <p className="vazio">Ninguém te deve nada no momento.</p>
      ) : (
        <div className="pessoas">
          {visiveis.map(p => (
            <article className="pessoa" key={p.pessoa}>
              <header className="pessoa-topo">
                <span className="pessoa-nome">{p.pessoa}</span>
                <span className="pessoa-total">{fmtBRL.format(p.totalAberto)}</span>
              </header>
              <ul className="pessoa-dividas">
                {p.dividas.map(d => (
                  <li key={d.id} className={d.quitada ? 'quitada' : ''}>
                    <span className="l-data">{formatarData(d.data)}</span>
                    <span className="l-desc">
                      {d.descricao}
                      {d.quitada && <small> · pago em {formatarData(d.dataQuitacao)}</small>}
                    </span>
                    <span className="l-valor">{fmtBRL.format(d.valor)}</span>
                    <span className="l-acoes">
                      {d.quitada
                        ? <button className="botao-mini" onClick={() => reabrir(d)}>Desfazer</button>
                        : <button className="botao-mini ok" onClick={() => quitar(d)}>Recebi</button>}
                      <button className="l-icone" onClick={() => setEditando(d)} aria-label="Editar" title="Editar">✎</button>
                      <button className="l-icone l-del" onClick={() => excluir(d)} aria-label="Excluir" title="Excluir">×</button>
                    </span>
                  </li>
                ))}
              </ul>
            </article>
          ))}
        </div>
      )}

      {temQuitadas && (
        <button type="button" className="link" onClick={() => setMostrarQuitadas(m => !m)}>
          {mostrarQuitadas ? 'Esconder quitadas' : 'Mostrar quitadas'}
        </button>
      )}
    </section>
  )
}
