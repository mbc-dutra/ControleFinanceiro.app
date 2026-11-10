import { COR_CATEGORIA } from '../categorias'

const fmtBRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export default function GraficoCategorias({ dados }) {
  if (!dados || dados.length === 0) {
    return (
      <section className="painel">
        <h2>Gastos por categoria</h2>
        <p className="vazio">Nenhuma saída registrada ainda.</p>
      </section>
    )
  }

  const max = Math.max(...dados.map(d => d.total))

  return (
    <section className="painel">
      <h2>Gastos por categoria</h2>
      <div className="grafico">
        {dados.map(d => {
          const largura = Math.max(4, Math.round((d.total / max) * 100))
          const cor = COR_CATEGORIA[d.categoria] || COR_CATEGORIA.Outros
          return (
            <div className="barra-linha" key={d.categoria}>
              <span className="barra-rotulo">{d.categoria}</span>
              <div className="barra-trilha">
                <div className="barra-preenchida" style={{ width: `${largura}%`, background: cor }} />
              </div>
              <span className="barra-valor">
                {fmtBRL.format(d.total)} · {d.percentual}%
              </span>
            </div>
          )
        })}
      </div>
    </section>
  )
}
