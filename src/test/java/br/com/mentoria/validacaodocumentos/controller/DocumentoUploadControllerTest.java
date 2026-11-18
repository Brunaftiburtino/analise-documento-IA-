package br.com.mentoria.validacaodocumentos.controller;

import br.com.mentoria.validacaodocumentos.documento.DadosExtraidos;
import br.com.mentoria.validacaodocumentos.documento.EtapaProcessamento;
import br.com.mentoria.validacaodocumentos.documento.RespostaValidacao;
import br.com.mentoria.validacaodocumentos.documento.ResultadoValidacao;
import br.com.mentoria.validacaodocumentos.documento.TipoDocumento;
import br.com.mentoria.validacaodocumentos.ValidacaoDocumentosApplication;
import br.com.mentoria.validacaodocumentos.service.DocumentoValidacaoService;
import br.com.mentoria.validacaodocumentos.service.HistoricoValidacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ValidacaoDocumentosApplication.class)
@AutoConfigureMockMvc
class DocumentoUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentoValidacaoService documentoValidacaoService;

    @MockitoBean
    private HistoricoValidacaoService historicoValidacaoService;

    @Test
    void recebeImagemExecutaValidacaoEArmazenaResultado() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "C:\\documentos\\comprovante.png",
                "image/png",
                new byte[]{1, 2, 3});
        RespostaValidacao resultadoPipeline = new RespostaValidacao(
                null,
                TipoDocumento.COMPROVANTE_RESIDENCIA,
                "comprovante.png",
                null,
                List.of(new EtapaProcessamento(1, "OCR", "Executado", "Concluído")),
                new DadosExtraidos(null, null, null, null, null),
                new ResultadoValidacao(false, "REPROVADO", List.of(), List.of("Dados ausentes")));
        RespostaValidacao resultadoPersistido = new RespostaValidacao(
                "id-teste",
                TipoDocumento.COMPROVANTE_RESIDENCIA,
                "comprovante.png",
                null,
                resultadoPipeline.etapas(),
                resultadoPipeline.dadosExtraidos(),
                resultadoPipeline.resultado());
        when(documentoValidacaoService.validarImagem(
                eq(TipoDocumento.COMPROVANTE_RESIDENCIA),
                eq("comprovante.png"),
                eq("image/png"),
                any())).thenReturn(resultadoPipeline);
        when(historicoValidacaoService.salvar(resultadoPipeline)).thenReturn(resultadoPersistido);

        mockMvc.perform(multipart("/api/documentos/upload")
                        .file(arquivo)
                        .param("tipoDocumento", "COMPROVANTE_RESIDENCIA"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("id-teste"))
                .andExpect(jsonPath("$.tipoDocumento").value("COMPROVANTE_RESIDENCIA"));

        verify(documentoValidacaoService).validarImagem(
                eq(TipoDocumento.COMPROVANTE_RESIDENCIA),
                eq("comprovante.png"),
                eq("image/png"),
                any());
        verify(historicoValidacaoService).salvar(resultadoPipeline);
    }

    @Test
    void rejeitaUploadDePdfQuandoEndpointAceitaSomenteImagens() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "comprovante.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/documentos/upload")
                        .file(arquivo)
                        .param("tipoDocumento", "COMPROVANTE_RESIDENCIA"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
