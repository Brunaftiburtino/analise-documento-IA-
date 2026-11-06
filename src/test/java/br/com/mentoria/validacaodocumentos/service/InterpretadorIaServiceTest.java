package br.com.mentoria.validacaodocumentos.service;

import br.com.mentoria.validacaodocumentos.documento.DadosExtraidos;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterpretadorIaServiceTest {

    private HttpServer server;
    private AtomicReference<String> responseBody;
    private AtomicReference<String> requestBody;
    private AtomicReference<String> authorization;

    @BeforeEach
    void setUp() throws IOException {
        responseBody = new AtomicReference<>();
        requestBody = new AtomicReference<>();
        authorization = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void interpretaJsonEstruturadoEEnviaAutorizacaoConfigurada() {
        responseBody.set("""
                {
                  "choices": [{
                    "message": {
                      "content": "{\\"nome\\":\\"Joao da Silva\\",\\"cpf\\":\\"123.456.789-00\\",\\"endereco\\":\\"Rua das Flores, 150\\",\\"cidade\\":\\"Guanambi - BA\\",\\"dataEmissao\\":\\"2026-08-10\\"}"
                    }
                  }]
                }
                """);

        DadosExtraidos dados = service("chave-de-teste").interpretar("texto do documento");

        assertEquals("Joao da Silva", dados.nome());
        assertEquals("123.456.789-00", dados.cpf());
        assertEquals("Rua das Flores, 150", dados.endereco());
        assertEquals("Guanambi - BA", dados.cidade());
        assertEquals(LocalDate.of(2026, 8, 10), dados.dataEmissao());
        assertEquals("Bearer chave-de-teste", authorization.get());
        assertTrue(requestBody.get().contains("\"type\":\"json_object\""));
        assertTrue(requestBody.get().contains("texto do documento"));
    }

    @Test
    void aceitaCamposAusentesOuNulosSemInventarValores() {
        responseBody.set("""
                {"choices":[{"message":{"content":"{\\"nome\\":null}"}}]}
                """);

        DadosExtraidos dados = service("chave-de-teste").interpretar("texto parcial");

        assertNull(dados.nome());
        assertNull(dados.cpf());
        assertNull(dados.dataEmissao());
    }

    @Test
    void informaErroDeGatewayQuandoApiRecusaARequisicao() throws IOException {
        server.removeContext("/v1/chat/completions");
        server.createContext("/v1/chat/completions", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> service("chave-de-teste").interpretar("texto"));

        assertEquals(HttpStatus.BAD_GATEWAY, erro.getStatusCode());
    }

    @Test
    void indicaSeAChaveNaoEstaConfigurada() {
        assertFalse(service(" ").isConfigurado());
    }

    private InterpretadorIaService service(String apiKey) {
        return new InterpretadorIaService(
                RestClient.builder(),
                new ObjectMapper(),
                apiKey,
                "modelo-teste",
                "http://localhost:" + server.getAddress().getPort() + "/v1");
    }
}
