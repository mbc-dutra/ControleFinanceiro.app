import { useEffect, useState } from 'react'
import {
  listarLancamentos,
  criarLancamento,
  excluirLancamento,
  obterResumo,
  obterCategorias,
  API_URL
} from './api/lancamentos'
import Resumo from './components/Resumo.jsx'
import NovoLancamento from './components/NovoLancamento.jsx'
import GraficoCategorias from './components/GraficoCategorias.jsx'
import ListaLancamentos from './components/ListaLancamentos.jsx'
import SeletorMes from './components/SeletorMes.jsx'
import { mesAtual } from './datas'

export default function App() {
  const [resumo, setResumo] = useState({ entradas: 0, saidas: 0, saldo: 0 })
  const [categorias, setCategorias] = useState([])
  const [lancamentos, setLancamentos] = useState([])
  const [filtro, setFiltro] = useState('TODOS')
  const [mes, setMes] = useState(mesAtual())
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState(null)
  const [versao, setVersao] = useState(0)

  useEffect(() => {
    let ativo = true
    async function carregarResumoECategorias() {
      try {
        const [dadosResumo, dadosCategorias] = await Promise.all([obterResumo(mes), obterCategorias(mes)])
        if (!ativo) return
        setResumo(dadosResumo)
        setCategorias(dadosCategorias)
        setErro(null)
      } catch {
        if (ativo) setErro(`Não foi possível conectar à API em ${API_URL}. Verifique se o backend está rodando.`)
      }
    }
    carregarResumoECategorias()
    return () => { ativo = false }
  }, [mes, versao])

  useEffect(() => {
    let ativo = true
    async function carregarLista() {
      try {
        setCarregando(true)
        const tipo = filtro === 'TODOS' ? undefined : filtro
        const dados = await listarLancamentos(tipo, mes)
        if (ativo) {
          setLancamentos(dados)
          setErro(null)
        }
      } catch {
        if (ativo) setErro(`Não foi possível conectar à API em ${API_URL}. Verifique se o backend está rodando.`)
      } finally {
        if (ativo) setCarregando(false)
      }
    }
    carregarLista()
    return () => { ativo = false }
  }, [filtro, mes, versao])

  async function handleCriar(payload) {
    await criarLancamento(payload)
    setVersao(v => v + 1)
  }

  async function handleExcluir(id) {
    await excluirLancamento(id)
    setVersao(v => v + 1)
  }

  return (
    <div className="app">
      <header className="masthead">
        <h1>Controle Financeiro</h1>
        <p className="tagline">Entradas, saídas e saldo sob controle</p>
        <SeletorMes mes={mes} onMudar={setMes} />
      </header>

      {erro && <div className="alerta-erro">{erro}</div>}

      <Resumo dados={resumo} />
      <NovoLancamento onCriar={handleCriar} />
      <GraficoCategorias dados={categorias} />
      <ListaLancamentos
        itens={lancamentos}
        filtro={filtro}
        onFiltro={setFiltro}
        onExcluir={handleExcluir}
        carregando={carregando}
      />
    </div>
  )
}
