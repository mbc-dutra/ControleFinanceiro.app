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

- **Contas com vencimento:** cada saída tem a data até quando precisa ser paga; as atrasadas
  aparecem em vermelho
- **Pago x previsto:** só entra no saldo o que você marcou como pago/recebido. O salário do
  dia 30 fica como "a receber" até você confirmar que caiu. O card mostra também o saldo
  previsto para o fim do mês
- **Previsão dos próximos meses:** lance com "repetir por N meses" (salário, aluguel,
  assinaturas) e navegue até dezembro, janeiro, fevereiro... Cada mês pode ser corrigido
  separadamente
- **Editar** qualquer lançamento (✎) e marcar/desmarcar como pago com um clique no status
- **Me devem:** parte separada, agrupada por pessoa, com quanto cada uma deve e por quê.
  Quando a pessoa paga, clique em "Recebi" e o valor entra no seu mês como reembolso
- Gráfico de gastos por categoria, com percentual, e filtro por entradas/saídas

## Stack

| Parte    | Tecnologia                                    |
|----------|-----------------------------------------------|
| Backend  | Java 17, Spring Boot 3.3, Spring Data JPA     |
| Banco    | PostgreSQL 16 (via Docker)                    |
| Frontend | React 18, Vite 5, axios                       |

## Como rodar

Pré-requisitos: Java 17, Maven, Node 18+ e PostgreSQL 16 (instalado no sistema ou via
`docker compose up -d`) com banco, usuário e senha `financas`.

```bash
cd backend && mvn spring-boot:run           # API → http://localhost:8080/api
cd frontend && npm install && npm run dev   # tela → http://localhost:5173
```

Testes do backend (não precisam de banco): `cd backend && mvn test`

## API

| Método | Rota                                         | O que faz                         |
|--------|----------------------------------------------|-----------------------------------|
| GET    | `/api/lancamentos?tipo=SAIDA&mes=2026-11`    | Lista por vencimento (filtros opcionais) |
| POST   | `/api/lancamentos`                           | Cria (aceita `pago` e `repetirMeses` 1–36) |
| PUT    | `/api/lancamentos/{id}`                      | Edita um lançamento               |
| PATCH  | `/api/lancamentos/{id}/pago`                 | `{"pago": true}` marca como pago/recebido |
| DELETE | `/api/lancamentos/{id}`                      | Exclui                            |
| GET    | `/api/lancamentos/resumo?mes=2026-11`        | Realizado (saldo) + a receber, a pagar e saldo previsto |
| GET    | `/api/lancamentos/categorias?mes=2026-11`    | Gastos por categoria com %        |
| GET    | `/api/dividas`                               | "Me devem", agrupado por pessoa   |
| POST / PUT / DELETE | `/api/dividas[/{id}]`           | Cria, edita, exclui               |
| POST   | `/api/dividas/{id}/quitar`                   | Marca como paga e cria a entrada de reembolso |
| POST   | `/api/dividas/{id}/reabrir`                  | Desfaz a quitação                 |

## Trabalhando com o agente de IA

- O contexto do projeto para o agente está em [`CLAUDE.md`](CLAUDE.md).
- Os prompts usados para gerar código estão em [`docs/prompts.md`](docs/prompts.md).
- **Critério para limpar a sessão:** começo uma sessão nova (em vez de insistir) quando
  (1) o agente repete o mesmo erro depois de 2 correções, (2) ele passa a "lembrar" de
  decisões que já foram descartadas, ou (3) mudei de tarefa. Antes de limpar, levo o que
  foi decidido para o `CLAUDE.md`, para a próxima sessão já começar sabendo.
