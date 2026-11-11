import { useEffect, useState } from 'react'
import {
  listarDividas, criarDivida, atualizarDivida, excluirDivida, quitarDivida, reabrirDivida,
  receberDivida, desfazerRecebimento, pagarDivida, desfazerPagamento
} from '../api/dividas'
import { fmtBRL, formatarData, hojeISO } from '../datas'

const NOME_TIPO = { CREDITO: 'Crédito', DEBITO: 'Débito' }
const OPCOES_PARCELAS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 18, 24, 36, 48]

/** Mensagem da API ({"mensagem": ...}) quando houver; senão, a genérica. */
const mensagemDe = (e, padrao) => e?.response?.data?.mensagem ?? padrao

function FormDivida({ editando, pessoasConhecidas, bancosConhecidos, onSalvar, onCancelar }) {
  const [form, setForm] = useState(() => editando
    ? {
        pessoa: editando.pessoa, tipo: editando.tipo, banco: editando.banco ?? '',
        descricao: editando.descricao, valor: String(editando.valor), data: editando.data
      }
    : { pessoa: '', tipo: 'DEBITO', banco: '', descricao: '', valor: '', data: hojeISO(), parcelas: 1 })
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState(null)

  const alterar = (campo, valor) => setForm(f => ({ ...f, [campo]: valor }))
  const credito = form.tipo === 'CREDITO'

  async function handleSubmit(e) {
    e.preventDefault()
    const valor = parseFloat(form.valor)
    if (!form.pessoa.trim() || !form.descricao.trim() || !valor || valor <= 0) return
    try {
      setEnviando(true)
      setErro(null)
      const { parcelas, ...resto } = form
      await onSalvar({
        ...resto,
        pessoa: form.pessoa.trim(),
        banco: form.banco.trim() || null,
        descricao: form.descricao.trim(),
        valor,
        ...(editando ? {} : { parcelas: Number(parcelas) })
      })
      if (!editando) setForm(f => ({ ...f, descricao: '', valor: '', parcelas: 1 }))
    } catch (err) {
      setErro(mensagemDe(err, 'Não foi possível salvar.'))
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
        <div className="campo">
          <label>Tipo</label>
          <div className="tipo-toggle">
            <button
              type="button"
              className={credito ? 'ativo credito' : ''}
              onClick={() => alterar('tipo', 'CREDITO')}
              title="Cartão, crédito, empréstimo: a pessoa só está pagando essa conta"
            >
              Crédito
            </button>
            <button
              type="button"
              className={!credito ? 'ativo debito' : ''}
              onClick={() => alterar('tipo', 'DEBITO')}
              title="Seu dinheiro: ao receber, entra no seu saldo"
            >
              Débito
            </button>
          </div>
        </div>
        <div className="campo">
          <label htmlFor="d-banco">Instituição</label>
          <input
            id="d-banco"
            list="bancos-conhecidos"
            value={form.banco}
            onChange={e => alterar('banco', e.target.value)}
            placeholder="Ex.: Nubank, Caixa... (opcional)"
            maxLength={40}
          />
          <datalist id="bancos-conhecidos">
            {bancosConhecidos.map(b => <option key={b} value={b} />)}
          </datalist>
        </div>
        <div className="campo campo-grow">
          <label htmlFor="d-desc">O que é</label>
          <input
            id="d-desc"
            value={form.descricao}
            onChange={e => alterar('descricao', e.target.value)}
            placeholder={credito ? 'Ex.: Shopee 3/10, Fatura cartão...' : 'Ex.: Pizza de sexta, Ingresso do show...'}
            required
          />
        </div>
        <div className="campo campo-valor">
          <label htmlFor="d-valor">{Number(form.parcelas) > 1 ? 'Valor da parcela' : 'Valor (R$)'}</label>
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
          <label htmlFor="d-data">{Number(form.parcelas) > 1 ? '1º vencimento' : 'Vencimento'}</label>
          <input id="d-data" type="date" value={form.data} onChange={e => alterar('data', e.target.value)} required />
        </div>
        {!editando && (
          <div className="campo">
            <label htmlFor="d-parcelas">Parcelas</label>
            <select id="d-parcelas" value={form.parcelas} onChange={e => alterar('parcelas', e.target.value)}>
              {OPCOES_PARCELAS.map(n => <option key={n} value={n}>{n === 1 ? 'À vista' : n + 'x'}</option>)}
            </select>
          </div>
        )}
        <button className="botao-primario" type="submit" disabled={enviando}>
          {enviando ? 'Salvando...' : editando ? 'Salvar' : 'Adicionar'}
        </button>
        {editando && <button type="button" className="botao-secundario" onClick={onCancelar}>Cancelar</button>}
      </form>
      {!editando && Number(form.parcelas) > 1 && (
        <p className="dica">
          Serão criadas {form.parcelas} parcelas de {fmtBRL.format(parseFloat(form.valor) || 0)}, uma por mês, cada uma com
          seu vencimento. Cada parcela é marcada (e pode ser corrigida) separadamente.
        </p>
      )}
      <p className="dica">
        {credito
          ? 'Crédito: o dinheiro saiu do seu cartão/crédito. Marque “Recebi” quando a pessoa te pagar e “Paguei” quando você pagar a instituição — nada disso mexe no seu saldo.'
          : 'Débito: era seu dinheiro. Ao clicar em “Recebi”, o valor entra no seu saldo como reembolso.'}
      </p>
      {erro && <p className="erro-form">{erro}</p>}
    </>
  )
}

/**
 * "Me devem": separado dos valores do usuário, agrupado por pessoa → crédito/débito → instituição.
 * Débito: ao quitar, vira uma entrada (Reembolso) no mês atual — por isso avisa o pai via onQuitacao.
 * Crédito: "Recebi" e "Paguei" são marcados à parte e não mexem no saldo.
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
  const hoje = hojeISO()
  const todosBancos = pessoas.flatMap(p => p.tipos.flatMap(t => t.bancos))
  const totalGeral = pessoas.reduce((soma, p) => soma + Number(p.totalAberto), 0)
  const totalAPagar = pessoas.reduce((soma, p) => soma + p.tipos.reduce((s, t) => s + Number(t.totalAPagar), 0), 0)
  const visiveis = pessoas
    .map(p => ({
      ...p,
      tipos: p.tipos
        .map(t => ({
          ...t,
          bancos: t.bancos
            .map(b => ({ ...b, dividas: mostrarQuitadas ? b.dividas : b.dividas.filter(d => !d.quitada) }))
            .filter(b => b.dividas.length > 0)
        }))
        .filter(t => t.bancos.length > 0)
    }))
    .filter(p => p.tipos.length > 0)
  const temQuitadas = todosBancos.some(b => b.dividas.some(d => d.quitada))
  const bancosConhecidos = [...new Map(todosBancos
    .map(b => b.banco)
    .filter(Boolean)
    .map(b => [b.toLowerCase(), b])).values()]

  async function salvar(payload) {
    if (editando) {
      await atualizarDivida(editando.id, payload)
      if (editando.quitada && editando.tipo === 'DEBITO') onQuitacao()
      setEditando(null)
    } else {
      await criarDivida(payload)
    }
    recarregar()
  }

  async function executar(acao, mudaSaldo) {
    try {
      setErro(null)
      await acao()
      recarregar()
      if (mudaSaldo) onQuitacao()
    } catch (e) {
      setErro(mensagemDe(e, 'Não foi possível concluir a ação.'))
    }
  }

  function quitar(d) {
    const ok = window.confirm(
      `${d.pessoa} te pagou ${fmtBRL.format(d.valor)} (${d.descricao})?\n\n` +
      'Isso cria uma entrada "Reembolso" com a data de hoje no seu controle.')
    if (ok) executar(() => quitarDivida(d.id), true)
  }

  function reabrir(d) {
    const ok = window.confirm(`Voltar "${d.descricao}" para pendente? A entrada de reembolso criada será removida do seu saldo.`)
    if (ok) executar(() => reabrirDivida(d.id), true)
  }

  function alternarRecebido(d) {
    if (!d.dataRecebimento) {
      if (window.confirm(`${d.pessoa} te pagou ${fmtBRL.format(d.valor)} (${d.descricao})?`)) {
        executar(() => receberDivida(d.id), false)
      }
    } else if (window.confirm(`Voltar "${d.descricao}" para pendente (não recebido)?`)) {
      executar(() => desfazerRecebimento(d.id), false)
    }
  }

  function alternarPago(d) {
    if (!d.dataPagamento) {
      if (window.confirm(`Você já pagou "${d.descricao}" (${fmtBRL.format(d.valor)}) à instituição?`)) {
        executar(() => pagarDivida(d.id), false)
      }
    } else if (window.confirm(`Voltar "${d.descricao}" para "a pagar"?`)) {
      executar(() => desfazerPagamento(d.id), false)
    }
  }

  function excluir(d) {
    const aviso = d.quitada && d.tipo === 'DEBITO'
      ? 'Excluir este registro? A entrada de reembolso que ele gerou continua no seu controle.'
      : `Excluir "${d.descricao}" de ${d.pessoa}?`
    if (window.confirm(aviso)) executar(() => excluirDivida(d.id), false)
  }

  /** Botão de status: vermelho enquanto pendente, verde depois de confirmado; clicar de novo desfaz. */
  function status(feito, textoPendente, textoFeito, dicaPendente, aoClicar) {
    const dica = feito ? 'Clique para voltar (se marcou por engano)' : dicaPendente
    return (
      <button
        type="button"
        className={feito ? 'l-status ok' : 'l-status atrasado'}
        onClick={aoClicar}
        title={dica}
        aria-label={(feito ? textoFeito : textoPendente) + '. ' + dica}
      >
        {feito ? textoFeito : textoPendente}
      </button>
    )
  }

  function acoes(d) {
    if (d.tipo === 'CREDITO') {
      return (
        <>
          {status(Boolean(d.dataRecebimento), 'Pendente', 'Recebi', 'Clique quando a pessoa te pagar', () => alternarRecebido(d))}
          {status(Boolean(d.dataPagamento), 'A pagar', 'Paguei', 'Clique quando você pagar a instituição', () => alternarPago(d))}
        </>
      )
    }
    return status(d.quitada, 'Pendente', 'Recebi', 'Clique quando a pessoa te pagar',
      () => (d.quitada ? reabrir(d) : quitar(d)))
  }

  function detalhe(d) {
    if (d.tipo === 'CREDITO') {
      const partes = [
        d.dataRecebimento && `recebido em ${formatarData(d.dataRecebimento)}`,
        d.dataPagamento && `pago em ${formatarData(d.dataPagamento)}`
      ].filter(Boolean)
      return partes.length > 0 && <small> · {partes.join(' · ')}</small>
    }
    return d.quitada && <small> · recebido em {formatarData(d.dataQuitacao)}</small>
  }

  return (
    <section className="painel painel-devem">
      <div className="painel-topo">
        <h2>Me devem</h2>
        <span className="total-devem">
          Total em aberto: <strong>{fmtBRL.format(totalGeral)}</strong>
          {totalAPagar > 0 && <> · Crédito a pagar: <strong className="a-pagar">{fmtBRL.format(totalAPagar)}</strong></>}
        </span>
      </div>

      <FormDivida
        key={editando?.id ?? 'nova'}
        editando={editando}
        pessoasConhecidas={pessoas.map(p => p.pessoa)}
        bancosConhecidos={bancosConhecidos}
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
              {p.tipos.map(t => (
                <div className={`pessoa-tipo tipo-${t.tipo.toLowerCase()}`} key={t.tipo}>
                  <div className="tipo-topo">
                    <span className="tipo-nome">{NOME_TIPO[t.tipo]}</span>
                    <span className="tipo-totais">
                      {fmtBRL.format(t.totalAberto)}
                      {t.tipo === 'CREDITO' && Number(t.totalAPagar) > 0 && (
                        <small> · a pagar {fmtBRL.format(t.totalAPagar)}</small>
                      )}
                    </span>
                  </div>
                  {t.bancos.map(b => (
                    <div className="pessoa-banco" key={b.banco ?? ''}>
                      {(b.banco || t.bancos.length > 1) && (
                        <div className="banco-topo">
                          <span className="banco-nome">{b.banco ?? 'Sem instituição'}</span>
                          <span className="banco-total">{fmtBRL.format(b.totalAberto)}</span>
                        </div>
                      )}
                      <ul className="pessoa-dividas">
                        {b.dividas.map(d => (
                          <li key={d.id} className={d.quitada ? 'quitada' : ''}>
                            <span
                              className={!d.quitada && d.data < hoje ? 'l-data vencida' : 'l-data'}
                              title={!d.quitada && d.data < hoje ? 'Vencida' : 'Vencimento'}
                            >
                              {formatarData(d.data)}
                            </span>
                            <span className="l-desc">
                              {d.descricao}
                              {detalhe(d)}
                            </span>
                            <span className="l-valor">{fmtBRL.format(d.valor)}</span>
                            <span className="l-acoes">
                              {acoes(d)}
                              <button className="l-icone" onClick={() => setEditando(d)} aria-label="Editar" title="Editar">✎</button>
                              <button className="l-icone l-del" onClick={() => excluir(d)} aria-label="Excluir" title="Excluir">×</button>
                            </span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  ))}
                </div>
              ))}
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
