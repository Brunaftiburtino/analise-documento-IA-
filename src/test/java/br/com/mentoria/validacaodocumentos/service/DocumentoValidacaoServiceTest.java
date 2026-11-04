package br.com.mentoria.validacaodocumentos.service;

import br.com.mentoria.validacaodocumentos.documento.DadosDocumento;
import br.com.mentoria.validacaodocumentos.documento.RespostaValidacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentoValidacaoServiceTest {

    private DocumentoValidacaoService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC);
        service = new DocumentoValidacaoService(
                new ExtratorDadosService(),
                new ValidacaoRegrasService(clock),
                new TesseractOcrService("", "", "por"));
    }

    @Test
    void extraiCamposEaprovaDocumentoDentroDoPrazo() {
        RespostaValidacao resposta = service.validar(documentoCom(
                "Nome: Joao da Silva\n"
                        + "CPF: 123.456.789-00\n"
                        + "Endereco: Rua das Flores, 150\n"
                        + "Cidade: Guanambi - BA\n"
                        + "Data de emissao: 10/08/2026"));

        assertEquals("Joao da Silva", resposta.dadosExtraidos().nome());
        assertEquals("123.456.789-00", resposta.dadosExtraidos().cpf());
        assertEquals("2026-08-10", resposta.dadosExtraidos().dataEmissao().toString());
        assertTrue(resposta.resultado().aprovado());
        assertEquals("APROVADO", resposta.resultado().status());
        assertEquals(4, resposta.etapas().size());
    }

    @Test
    void apontaPendenciasParaCamposAusentesEDocumentoAntigo() {
        RespostaValidacao resposta = service.validar(documentoCom(
                "Nome: Joao da Silva\nData de emissao: 01/01/2026"));

        assertFalse(resposta.resultado().aprovado());
        assertTrue(resposta.resultado().pendencias().contains("CPF nao encontrado"));
        assertTrue(resposta.resultado().pendencias().contains("Emissao superior a 90 dias"));
    }

    @Test
    void exigeTextoAteQueOcrSejaImplementado() {
        DadosDocumento documento = new DadosDocumento(
                "imagem.png",
                "image/png",
                null,
                "aW1hZ2Vt");

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> service.validar(documento));

        assertEquals(400, erro.getStatusCode().value());
    }

    @Test
    void usaTextoComoFallbackQuandoImagemETextoSaoEnviadosSemTesseract() {
        RespostaValidacao resposta = service.validar(new DadosDocumento(
                "comprovante.png",
                "image/png",
                "Nome: Joao da Silva\nData de emissao: 10/08/2026",
                "aW1hZ2Vt"));

        assertEquals("Joao da Silva", resposta.dadosExtraidos().nome());
        assertTrue(resposta.etapas().get(1).descricao().contains("Fallback local"));
    }

    @Test
    void rejeitaImagemComBase64InvalidoQuandoTesseractEstaConfigurado() {
        DocumentoValidacaoService serviceComOcr = new DocumentoValidacaoService(
                new ExtratorDadosService(),
                new ValidacaoRegrasService(Clock.systemUTC()),
                new TesseractOcrService("tesseract", "", "por"));
        DadosDocumento documento = new DadosDocumento(
                "imagem.png",
                "image/png",
                "Nome: Joao",
                "não-é-base64");

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> serviceComOcr.validar(documento));

        assertEquals(400, erro.getStatusCode().value());
    }

    private static DadosDocumento documentoCom(String conteudo) {
        return new DadosDocumento("comprovante.pdf", "application/pdf", conteudo, null);
    }
}
