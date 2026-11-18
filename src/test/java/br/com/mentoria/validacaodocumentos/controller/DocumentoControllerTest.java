package br.com.mentoria.validacaodocumentos.controller;

import br.com.mentoria.validacaodocumentos.ValidacaoDocumentosApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest(classes = ValidacaoDocumentosApplication.class)
@AutoConfigureMockMvc
class DocumentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void disponibilizaInterfaceDaDemonstracaoNaPaginaInicial() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ValidaDoc")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"history-title\"")));
    }

    @Test
    void endpointDeExemploRetornaPayloadQuePodeSerValidado() throws Exception {
        String payload = mockMvc.perform(get("/api/documentos/exemplo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeArquivo").value("comprovante-residencia.pdf"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        mockMvc.perform(post("/api/documentos/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeArquivo").value("comprovante-residencia.pdf"))
                .andExpect(jsonPath("$.etapas.length()").value(4))
                .andExpect(jsonPath("$.dadosExtraidos.nome").value("Joao da Silva"))
                .andExpect(jsonPath("$.resultado.aprovado").value(true))
                .andExpect(jsonPath("$.resultado.status").value("APROVADO"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.tipoDocumento").value("COMPROVANTE_RESIDENCIA"))
                .andExpect(jsonPath("$.dadosExtraidos.cep").value("46430-000"));
    }

    @Test
    void persisteResultadoParaConsultaEIncluiNoHistorico() throws Exception {
        String payload = mockMvc.perform(get("/api/documentos/exemplo"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String resposta = mockMvc.perform(post("/api/documentos/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .build()
                .readTree(resposta)
                .get("id")
                .asText();

        mockMvc.perform(get("/api/documentos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.resultado.status").value("APROVADO"));

        mockMvc.perform(get("/api/documentos/historico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    void consultaInexistenteRetornaNaoEncontrado() throws Exception {
        mockMvc.perform(get("/api/documentos/{id}", "id-inexistente"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejeitaRequestSemCamposObrigatorios() throws Exception {
        mockMvc.perform(post("/api/documentos/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nomeArquivo": "",
                                  "mimeType": "",
                                  "conteudoDocumento": "texto"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
