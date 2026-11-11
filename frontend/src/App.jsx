import { useEffect, useState } from 'react'
import {
  listarLancamentos,
  criarLancamento,
  atualizarLancamento,
  marcarPago,
  excluirLancamento,
  obterResumo,
  obterCategorias,
  API_URL
} from './api/lancamentos'
import Resumo from './components/Resumo.jsx'
import FormLancamento from './components/FormLancamento.jsx'
import GraficoCategorias from './components/GraficoCategorias.jsx'
import ListaLancamentos from './components/ListaLancamentos.jsx'
import MeDevem from './components/MeDevem.jsx'
import SeletorMes from './components/SeletorMes.jsx'
import { mesAtual } from './datas'

const ERRO_CONEXAO = `Não foi possível conectar à API em ${API_URL}. Verifique se o backend está rodando.`

export default function App() {
  const [resumo, setResumo] = useState({})
  const [categorias, setCategorias] = useState([])
  const [lancamentos, setLancamentos] = useState([])
  const [filtro, setFiltro] = useState('TODOS')
  const [mes, setMes] = useState(mesAtual())
  const [editando, setEditando] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState(null)
  const [versao, setVersao] = useState(0)

  const recarregar = () => setVersao(v => v + 1)

  useEffect(() => {
    let ativo = true
    Promise.all([obterResumo(mes), obterCategorias(mes)])
      .then(([dadosResumo, dadosCategorias]) => {
        if (!ativo) return
        setResumo(dadosResumo)
        setCategorias(dadosCategorias)
        setErro(null)
      })
      .catch(() => { if (ativo) setErro(ERRO_CONEXAO) })
    return () => { ativo = false }
  }, [mes, versao])

  useEffect(() => {
    let ativo = true
    setCarregando(true)
    listarLancamentos(filtro === 'TODOS' ? undefined : filtro, mes)
      .then(dados => { if (ativo) { setLancamentos(dados); setErro(null) } })
      .catch(() => { if (ativo) setErro(ERRO_CONEXAO) })
      .finally(() => { if (ativo) setCarregando(false) })
    return () => { ativo = false }
  }, [filtro, mes, versao])

  async function handleSalvar(payload) {
    if (editando) {
      await atualizarLancamento(editando.id, payload)
      setEditando(null)
    } else {
      await criarLancamento(payload)
    }
    recarregar()
  }

  function handleEditar(item) {
    setEditando(item)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  async function handleAlternarPago(item) {
    try {
      await marcarPago(item.id, !item.pago)
      recarregar()
    } catch {
      setErro('Não foi possível atualizar o status.')
    }
  }

  async function handleExcluir(id) {
    if (!window.confirm('Excluir este lançamento?')) return
    await excluirLancamento(id)
    if (editando?.id === id) setEditando(null)
    recarregar()
  }

  // Sugestões do campo Instituição: as já usadas nos lançamentos do mês (sem repetir por maiúsculas).
  const instituicoesConhecidas = [...new Map(lancamentos
    .map(l => l.instituicao)
    .filter(Boolean)
    .map(i => [i.toLowerCase(), i])).values()]

  return (
    <div className="app">
      <header className="masthead">
        <h1>Controle Financeiro</h1>
        <p className="tagline">Entradas, saídas e saldo sob controle</p>
        <SeletorMes mes={mes} onMudar={m => { setMes(m); setEditando(null) }} />
      </header>

      {erro && <div className="alerta-erro">{erro}</div>}

      <Resumo dados={resumo} />
      <FormLancamento
        key={editando ? `editar-${editando.id}` : `novo-${mes}`}
        editando={editando}
        mes={mes}
        instituicoesConhecidas={instituicoesConhecidas}
        onSalvar={handleSalvar}
        onCancelar={() => setEditando(null)}
      />
      <ListaLancamentos
        itens={lancamentos}
        filtro={filtro}
        onFiltro={setFiltro}
        onExcluir={handleExcluir}
        onEditar={handleEditar}
        onAlternarPago={handleAlternarPago}
        carregando={carregando}
        editandoId={editando?.id}
      />
      <GraficoCategorias dados={categorias} />
      <MeDevem onQuitacao={recarregar} />
    </div>
  )
}
