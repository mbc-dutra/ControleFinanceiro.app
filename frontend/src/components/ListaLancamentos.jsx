import { COR_CATEGORIA } from '../categorias'

const fmtBRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const fmtData = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: '2-digit' })

export default function ListaLancamentos({ itens, filtro, onFiltro, onExcluir, carregando }) {
  return (
    <section className="painel">
      <h2>Lançamentos</h2>

      <div className="filtros">
        <button className={filtro === 'TODOS' ? 'chip ativo' : 'chip'} onClick={() => onFiltro('TODOS')}>
          Todos
        </button>
        <button className={filtro === 'ENTRADA' ? 'chip ativo' : 'chip'} onClick={() => onFiltro('ENTRADA')}>
          Entradas
        </button>
        <button className={filtro === 'SAIDA' ? 'chip ativo' : 'chip'} onClick={() => onFiltro('SAIDA')}>
          Saídas
        </button>
      </div>

      {carregando ? (
        <p className="vazio">Carregando...</p>
      ) : itens.length === 0 ? (
        <p className="vazio">Nenhum lançamento para mostrar.</p>
      ) : (
        <ul className="lista-lancamentos">
          {itens.map(item => {
            const cor = item.tipo === 'SAIDA' ? (COR_CATEGORIA[item.categoria] || COR_CATEGORIA.Outros) : null
            const sinal = item.tipo === 'ENTRADA' ? '+' : '−'
            const data = new Date(`${item.data}T00:00:00`)
            return (
              <li key={item.id}>
                <span className="l-data">{fmtData.format(data)}</span>
                <span className="l-desc">{item.descricao}</span>
                <span
                  className="l-cat"
                  style={
                    item.tipo === 'SAIDA'
                      ? { background: `${cor}26`, color: cor, borderColor: `${cor}66` }
                      : { background: 'var(--wine-soft)', color: 'var(--wine-light)' }
                  }
                >
                  {item.categoria}
                </span>
                <span className={`l-valor ${item.tipo === 'ENTRADA' ? 'entrada' : 'saida'}`}>
                  {sinal} {fmtBRL.format(item.valor)}
                </span>
                <button className="l-del" onClick={() => onExcluir(item.id)} aria-label="Excluir lançamento">
                  ×
                </button>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
