# Controle Financeiro

## Para quem não programa: que problema isso resolve?

Muita gente chega no fim do mês sem saber para onde foi o dinheiro: os gastos ficam
espalhados entre o cartão, o Pix e o dinheiro vivo, e a anotação em caderno ou planilha
acaba abandonada. Este aplicativo é um lugar único e simples para registrar o que entrou
e o que saiu, e ele mostra na hora quanto sobrou no mês e em quais categorias (moradia,
alimentação, transporte...) o dinheiro está indo embora. Isso importa porque quem enxerga
os próprios gastos consegue cortar o que pesa, se planejar para as contas do mês seguinte
e sair do aperto antes de cair no cheque especial ou no rotativo do cartão.

## O que já funciona

- Registrar entradas e saídas com descrição, categoria, valor e data
- Navegar mês a mês (‹ ›) e ver o saldo, total de entradas e de saídas do mês escolhido
- Gráfico de gastos por categoria, com percentual
- Filtrar a lista por entradas/saídas e excluir lançamentos

## Stack

| Parte    | Tecnologia                                    |
|----------|-----------------------------------------------|
| Backend  | Java 17, Spring Boot 3.3, Spring Data JPA     |
| Banco    | PostgreSQL 16 (via Docker)                    |
| Frontend | React 18, Vite 5, axios                       |

## Como rodar

Pré-requisitos: Java 17, Maven, Node 18+ e Docker.

```bash
docker compose up -d                        # banco
cd backend && mvn spring-boot:run           # API → http://localhost:8080/api
cd frontend && npm install && npm run dev   # tela → http://localhost:5173
```

Testes do backend (não precisam de banco): `cd backend && mvn test`

## API

| Método | Rota                                         | O que faz                         |
|--------|----------------------------------------------|-----------------------------------|
| GET    | `/api/lancamentos?tipo=SAIDA&mes=2026-11`    | Lista (filtros opcionais)         |
| POST   | `/api/lancamentos`                           | Cria um lançamento                |
| DELETE | `/api/lancamentos/{id}`                      | Exclui                            |
| GET    | `/api/lancamentos/resumo?mes=2026-11`        | Entradas, saídas e saldo          |
| GET    | `/api/lancamentos/categorias?mes=2026-11`    | Gastos por categoria com %        |

## Trabalhando com o agente de IA

- O contexto do projeto para o agente está em [`CLAUDE.md`](CLAUDE.md).
- Os prompts usados para gerar código estão em [`docs/prompts.md`](docs/prompts.md).
- **Critério para limpar a sessão:** começo uma sessão nova (em vez de insistir) quando
  (1) o agente repete o mesmo erro depois de 2 correções, (2) ele passa a "lembrar" de
  decisões que já foram descartadas, ou (3) mudei de tarefa. Antes de limpar, levo o que
  foi decidido para o `CLAUDE.md`, para a próxima sessão já começar sabendo.
