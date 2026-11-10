import { fmtBRL } from '../datas'

// Saldo/entradas/saídas = só o que já foi pago ou recebido.
// "A receber"/"a pagar" = previstos do mês, que entram no saldo previsto.
export default function Resumo({ dados }) {
  const {
    entradas = 0, saidas = 0, saldo = 0,
    aReceber = 0, aPagar = 0, saldoPrevisto = 0
  } = dados

  return (
    <section className="tiles">
      <div className="tile tile-saldo">
        <span className="tile-label">Saldo do mês</span>
        <span className="tile-valor">{fmtBRL.format(saldo)}</span>
        <span className="tile-sub">Previsto no fim do mês: <strong>{fmtBRL.format(saldoPrevisto)}</strong></span>
      </div>
      <div className="tile tile-entradas">
        <span className="tile-label">Recebido</span>
        <span className="tile-valor">{fmtBRL.format(entradas)}</span>
        <span className="tile-sub">A receber: <strong>{fmtBRL.format(aReceber)}</strong></span>
      </div>
      <div className="tile tile-saidas">
        <span className="tile-label">Pago</span>
        <span className="tile-valor">{fmtBRL.format(saidas)}</span>
        <span className="tile-sub">A pagar: <strong>{fmtBRL.format(aPagar)}</strong></span>
      </div>
    </section>
  )
}
