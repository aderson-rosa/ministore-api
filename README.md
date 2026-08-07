# MiniStore API

API REST de **e-commerce simplificado**: catálogo de produtos, **carrinho** (itens) e **pedidos** com baixa automática de estoque. Projeto de portfólio inspirado na vivência com marketplace de grande porte, com foco em back-end limpo e **testes de integração ponta a ponta**.

> Projeto de portfólio de **Aderson Rosa** — Java + Spring Boot, unindo a base de QA (testes automatizados) à construção do back-end.

## 🧰 Tech Stack

- **Java 17**
- **Spring Boot 3** (Web, Data JPA, Validation, AMQP)
- **JPA / Hibernate**
- **RabbitMQ** (Spring AMQP) — publicação e consumo de eventos de pedido (mensageria)
- **Observabilidade:** Micrometer + Actuator + **Prometheus** (métricas) e correlation id ponta a ponta (MDC)
- **PostgreSQL** (produção) · **H2** (execução local sem instalar nada)
- **JUnit 5 + Mockito** (unitário) e **REST Assured** (integração ponta a ponta em servidor real)
- **springdoc-openapi / Swagger UI**
- **Docker + Docker Compose**

## ▶️ Como rodar

### Opção 1 — Local com H2 (zero configuração)

```bash
mvn spring-boot:run
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Console H2: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:ministore`)

### Opção 2 — Docker Compose com PostgreSQL

```bash
docker compose up --build
```

## 📚 Endpoints principais

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/products` | Lista produtos |
| `GET` | `/api/products/{id}` | Detalha um produto |
| `POST` | `/api/products` | Cadastra produto |
| `PUT` | `/api/products/{id}` | Atualiza produto |
| `DELETE` | `/api/products/{id}` | Remove produto |
| `GET` | `/api/orders` | Lista pedidos |
| `GET` | `/api/orders/{id}` | Detalha um pedido |
| `POST` | `/api/orders` | Cria pedido a partir do carrinho (valida e baixa o estoque) |

### Exemplo — criar produto e pedido

```bash
# Cadastra um produto
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Camiseta","description":"Algodao","price":50.00,"stock":5}'

# Cria um pedido de 2 unidades do produto de id 1
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productId":1,"quantity":2}]}'
```

Regras de negócio:
- Um pedido valida o **estoque** de cada item; se faltar, retorna `422` sem alterar nada.
- Ao confirmar, o **estoque é baixado** e o **total** é calculado a partir do preço atual dos produtos.

## 📨 Mensageria (RabbitMQ) + confiabilidade

Ao criar um pedido, a aplicação emite o evento **`order.created`**, e um **consumer** o processa de forma desacoplada (simulando notificação/faturamento/separação de estoque).

- **Exchange:** `ministore.exchange` (tipo *topic*) · **Routing key:** `order.created` · **Fila:** `ministore.order-created.queue`
- Mensagens em **JSON** (`Jackson2JsonMessageConverter`).
- Com `docker compose up`, sobe também o **painel de gestão** do RabbitMQ em `http://localhost:15672` (guest/guest).

### Garantia de entrega: Transactional Outbox
O evento **não é publicado direto** na criação do pedido. Ele é **gravado numa tabela de outbox (`outbox_events`) na MESMA transação do pedido** (atomicidade). Um relay agendado (`OutboxPublisher`) lê os pendentes e publica no RabbitMQ, marcando como enviados. Se o broker estiver **indisponível**, o evento **não se perde**: permanece `PENDING` e é reprocessado no próximo ciclo (entrega *at-least-once*).

Fluxo: `POST /api/orders` → `OrderService` (transação: baixa estoque + grava outbox) → `OutboxPublisher` (relay) publica `order.created` → `OrderCreatedListener` consome.

### Concorrência: lock pessimista no estoque
A baixa de estoque usa **lock pessimista** (`SELECT ... FOR UPDATE`, via `@Lock(PESSIMISTIC_WRITE)`), serializando o acesso ao produto para **evitar race condition** e venda acima do estoque quando dois pedidos chegam simultaneamente para o mesmo item.

## 🔭 Observabilidade

- **Correlation ID ponta a ponta:** um `X-Correlation-Id` é gerado por requisição (ou reaproveitado do header), colocado no **MDC** (todos os logs do fluxo carregam o id) e devolvido na resposta. Ele é **persistido no outbox** e propagado para o **header da mensagem** ao publicar; o consumer o extrai de volta para o MDC. Assim o rastreamento **sobrevive ao boundary assíncrono e a retries/reprocessamento**.
- **Métricas:** via **Micrometer + Actuator**, com o contador `ministore.orders.placed` (pedidos criados), expostas em `GET /actuator/metrics` e em `GET /actuator/prometheus` (formato de scraping).
- **Stack de monitoramento pronto:** `docker compose up` sobe também **Prometheus** (`:9090`) raspando a API e **Grafana** (`:3000`, admin/admin) com o datasource e o dashboard *"MiniStore Observability"* já provisionados.
- **Health check:** `GET /actuator/health`.

## 🧪 Testes

```bash
mvn test
```

- `OrderServiceTest` — regras de estoque (com lock), cálculo do total e gravação do evento no outbox, isolados com **Mockito**.
- `OrderEventPublisherTest` — publicação no RabbitMQ.
- `OutboxPublisherTest` — relay do outbox: publica pendentes, marca como enviados e mantém pendente quando o broker falha.
- `MinistoreRestAssuredTest` — sobe a aplicação em porta real e testa o fluxo produto → pedido → baixa de estoque com **REST Assured**.

## 🗂️ Estrutura

```
src/main/java/com/aderson/ministore
├── config       # OpenAPI e RabbitMQ (exchange, fila, binding)
├── controller   # Endpoints REST (produtos, pedidos)
├── domain       # Entidades JPA + repositórios (product, order)
├── dto          # Records de request/response
├── exception    # Tratamento global de erros
├── messaging      # Evento, publisher e listener do RabbitMQ
├── observability  # Correlation id (filtro HTTP) e constantes
├── outbox         # Transactional Outbox (entidade, repositório e relay agendado)
└── service        # Regras de negócio
```

## 📄 Licença

MIT.
