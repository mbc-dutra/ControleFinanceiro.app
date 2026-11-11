import { COR_CATEGORIA } from '../categorias'
import { fmtBRL, formatarData, hojeISO } from '../datas'

function statusDe(item, hoje) {
  const entrada = item.tipo === 'ENTRADA'
  if (item.pago) return { texto: entrada ? 'Recebido' : 'Pago', classe: 'ok' }
  if (item.data < hoje) return { texto: 'Atrasado', classe: 'atrasado' }
  return { texto: entrada ? 'A receber' : 'A pagar', classe: 'previsto' }
}

export default function ListaLancamentos({
  itens, filtro, onFiltro, onExcluir, onEditar, onAlternarPago, carregando, editandoId
}) {
  const hoje = hojeISO()

  return (
    <section className="painel">
      <h2>Lançamentos</h2>

      <div className="filtros">
        <button className={filtro === 'TODOS' ? 'chip ativo' : 'chip'} onClick={() => onFiltro('TODOS')}>Todos</button>
        <button className={filtro === 'ENTRADA' ? 'chip ativo' : 'chip'} onClick={() => onFiltro('ENTRADA')}>Entradas</button>
        <button className={filtro === 'SAIDA' ? 'chip ativo' : 'chip'} onClick={() => onFiltro('SAIDA')}>Saídas</button>
      </div>

      {carregando ? (
        <p className="vazio">Carregando...</p>
      ) : itens.length === 0 ? (
        <p className="vazio">Nenhum lançamento neste mês.</p>
      ) : (
        <ul className="lista-lancamentos">
          {itens.map(item => {
            const saida = item.tipo === 'SAIDA'
            const cor = saida ? (COR_CATEGORIA[item.categoria] || COR_CATEGORIA.Outros) : null
            const status = statusDe(item, hoje)
            const proximaAcao = item.pago
              ? 'Clique para voltar a previsto'
              : `Clique para marcar como ${saida ? 'pago' : 'recebido'}`
            return (
              <li key={item.id} className={[item.pago ? '' : 'previsto', item.id === editandoId ? 'editando' : ''].join(' ')}>
                <span className="l-data" title={saida ? 'Vencimento' : 'Data prevista'}>{formatarData(item.data)}</span>
                <span className="l-desc">
                  {item.descricao}
                  {item.instituicao && <small className="l-inst" title="Instituição">{item.instituicao}</small>}
                </span>
                <span
                  className="l-cat"
                  style={
                    saida
                      ? { background: `${cor}26`, color: cor, borderColor: `${cor}66` }
                      : { background: 'var(--blue-soft)', color: 'var(--blue-light)' }
                  }
                >
                  {item.categoria}
                </span>
                <button
                  type="button"
                  className={`l-status ${status.classe}`}
                  onClick={() => onAlternarPago(item)}
                  title={proximaAcao}
                  aria-label={`${status.texto}. ${proximaAcao}`}
                >
                  {status.texto}
                </button>
                <span className={`l-valor ${saida ? 'saida' : 'entrada'}`}>
                  {saida ? '−' : '+'} {fmtBRL.format(item.valor)}
                </span>
                <span className="l-acoes">
                  <button className="l-icone" onClick={() => onEditar(item)} aria-label="Editar lançamento" title="Editar">✎</button>
                  <button className="l-icone l-del" onClick={() => onExcluir(item.id)} aria-label="Excluir lançamento" title="Excluir">×</button>
                </span>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
