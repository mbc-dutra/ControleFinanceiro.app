const fmtBRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export default function Resumo({ dados }) {
  return (
    <section className="tiles">
      <div className="tile tile-saldo">
        <span className="tile-label">Saldo atual</span>
        <span className="tile-valor">{fmtBRL.format(dados.saldo || 0)}</span>
      </div>
      <div className="tile tile-entradas">
        <span className="tile-label">Entradas</span>
        <span className="tile-valor">{fmtBRL.format(dados.entradas || 0)}</span>
      </div>
      <div className="tile tile-saidas">
        <span className="tile-label">Saídas</span>
        <span className="tile-valor">{fmtBRL.format(dados.saidas || 0)}</span>
      </div>
    </section>
  )
}
