import { deslocarMes, nomeDoMes } from '../datas'

export default function SeletorMes({ mes, onMudar }) {
  return (
    <div className="seletor-mes">
      <button type="button" onClick={() => onMudar(deslocarMes(mes, -1))} aria-label="Mês anterior">‹</button>
      <span className="seletor-mes-nome">{nomeDoMes(mes)}</span>
      <button type="button" onClick={() => onMudar(deslocarMes(mes, 1))} aria-label="Próximo mês">›</button>
    </div>
  )
}
