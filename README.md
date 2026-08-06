# MiniStore API

API REST de **e-commerce simplificado**: catálogo de produtos, **carrinho** (itens) e **pedidos** com baixa automática de estoque. Projeto de portfólio inspirado na vivência com marketplace de grande porte, com foco em back-end limpo e **testes de integração ponta a ponta**.

> Projeto de portfólio de **Aderson Rosa** — Java + Spring Boot, unindo a base de QA (testes automatizados) à construção do back-end.

## 🧰 Tech Stack

- **Java 17**
- **Spring Boot 3** (Web, Data JPA, Validation, AMQP)
- **JPA / Hibernate**
- **RabbitMQ** (Spring AMQP) — publicação e consumo de eventos de pedido (mensageria)
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

## 📨 Mensageria (RabbitMQ)

Ao criar um pedido, a aplicação **publica um evento `order.created`** no RabbitMQ, e um **consumer** processa esse evento de forma desacoplada (simulando notificação/faturamento/separação de estoque).

- **Exchange:** `ministore.exchange` (tipo *topic*) · **Routing key:** `order.created` · **Fila:** `ministore.order-created.queue`
- **Publicação resiliente:** se o broker estiver indisponível, o pedido **não é perdido** — o evento apenas não é enviado e a falha é logada.
- Mensagens trafegam em **JSON** (`Jackson2JsonMessageConverter`).
- Com `docker compose up`, sobe também o **painel de gestão** do RabbitMQ em `http://localhost:15672` (guest/guest), onde dá para ver as mensagens na fila.

Fluxo: `POST /api/orders` → `OrderService` salva o pedido → `OrderEventPublisher` publica `order.created` → `OrderCreatedListener` consome e loga.

## 🧪 Testes

```bash
mvn test
```

- `OrderServiceTest` — regras de estoque, cálculo do total e publicação do evento, isolados com **Mockito**.
- `OrderEventPublisherTest` — publicação no RabbitMQ e resiliência quando o broker está indisponível.
- `MinistoreRestAssuredTest` — sobe a aplicação em porta real e testa o fluxo produto → pedido → baixa de estoque com **REST Assured**.

## 🗂️ Estrutura

```
src/main/java/com/aderson/ministore
├── config       # OpenAPI e RabbitMQ (exchange, fila, binding)
├── controller   # Endpoints REST (produtos, pedidos)
├── domain       # Entidades JPA + repositórios (product, order)
├── dto          # Records de request/response
├── exception    # Tratamento global de erros
├── messaging    # Evento, publisher e listener do RabbitMQ
└── service      # Regras de negócio
```

## 📄 Licença

MIT.
