package com.aderson.ministore;

import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.aderson.ministore.messaging.OrderEventPublisher;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "outbox.publisher.enabled=false"
        })
class MinistoreRestAssuredTest {

    @LocalServerPort
    private int port;

    // Mensageria mockada: o teste de API nao precisa de um broker RabbitMQ real.
    @MockBean
    private OrderEventPublisher orderEventPublisher;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.config = RestAssured.config().jsonConfig(
                JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
    }

    @Test
    void fluxoCompleto_criarProdutoFazerPedidoEBaixarEstoque() {
        // 1) Cria um produto com estoque 5
        int productId = given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Camiseta\",\"description\":\"Algodao\",\"price\":50.00,\"stock\":5}")
                .when()
                .post("/api/products")
                .then()
                .statusCode(201)
                .body("name", equalTo("Camiseta"))
                .body("stock", equalTo(5))
                .extract().path("id");

        // 2) Faz um pedido de 2 unidades
        given()
                .contentType(ContentType.JSON)
                .body("{\"items\":[{\"productId\":" + productId + ",\"quantity\":2}]}")
                .when()
                .post("/api/orders")
                .then()
                .statusCode(201)
                .body("status", equalTo("CREATED"))
                .body("total", comparesEqualTo(new BigDecimal("100.00")))
                .body("items[0].quantity", equalTo(2));

        // 3) Estoque deve ter baixado para 3
        given()
                .when()
                .get("/api/products/" + productId)
                .then()
                .statusCode(200)
                .body("stock", equalTo(3));

        // 4) Pedido acima do estoque -> 422
        given()
                .contentType(ContentType.JSON)
                .body("{\"items\":[{\"productId\":" + productId + ",\"quantity\":100}]}")
                .when()
                .post("/api/orders")
                .then()
                .statusCode(422);
    }
}
