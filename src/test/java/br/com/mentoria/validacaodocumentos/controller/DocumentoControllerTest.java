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

@SpringBootTest(classes = ValidacaoDocumentosApplication.class)
@AutoConfigureMockMvc
class DocumentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
                .andExpect(jsonPath("$.resultado.status").value("APROVADO"));
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
