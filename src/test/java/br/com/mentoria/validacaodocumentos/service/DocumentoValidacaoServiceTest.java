package br.com.mentoria.validacaodocumentos.service;

import br.com.mentoria.validacaodocumentos.documento.DadosDocumento;
import br.com.mentoria.validacaodocumentos.documento.RespostaValidacao;
import br.com.mentoria.validacaodocumentos.documento.TipoDocumento;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentoValidacaoServiceTest {

    private DocumentoValidacaoService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC);
        service = new DocumentoValidacaoService(
                new ExtratorDadosService(),
                new ValidacaoRegrasService(clock),
                new TesseractOcrService("", "", "por", 11),
                new OpenCvPreprocessamentoService(""),
                new InterpretadorIaService(
                        org.springframework.web.client.RestClient.builder(),
                        new ObjectMapper(),
                        "",
                        "gpt-4o-mini",
                        "https://api.openai.com/v1"));
    }

    @Test
    void extraiCamposEaprovaDocumentoDentroDoPrazo() {
        RespostaValidacao resposta = service.validar(documentoCom(
                "Nome: Joao da Silva\n"
                        + "CPF: 529.982.247-25\n"
                        + "Endereco: Rua das Flores, 150\n"
                        + "CEP: 46430-000\n"
                        + "Cidade: Guanambi - BA\n"
                        + "Data de emissao: 10/08/2026"));

        assertEquals("Joao da Silva", resposta.dadosExtraidos().nome());
        assertEquals("529.982.247-25", resposta.dadosExtraidos().cpf());
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
    void rejeitaDocumentoComDataDeEmissaoFutura() {
        RespostaValidacao resposta = service.validar(documentoCom(
                "Nome: Joao da Silva\n"
                        + "CPF: 529.982.247-25\n"
                        + "Endereco: Rua das Flores, 150\n"
                        + "CEP: 46430-000\n"
                        + "Cidade: Guanambi - BA\n"
                        + "Data de emissao: 05/11/2026"));

        assertFalse(resposta.resultado().aprovado());
        assertTrue(resposta.resultado().pendencias().contains("Data de emissao futura"));
    }

    @Test
    void aceitaDocumentoEmitidoExatamenteHaNoventaDias() {
        RespostaValidacao resposta = service.validar(documentoCom(
                "Nome: Joao da Silva\n"
                        + "CPF: 529.982.247-25\n"
                        + "Endereco: Rua das Flores, 150\n"
                        + "CEP: 46430-000\n"
                        + "Cidade: Guanambi - BA\n"
                        + "Data de emissao: 06/08/2026"));

        assertTrue(resposta.resultado().aprovado());
        assertTrue(resposta.resultado().regrasAtendidas().contains("Emissao dentro de 90 dias"));
    }

    @Test
    void validaCepObrigatorioNoComprovanteDeResidencia() {
        RespostaValidacao resposta = service.validar(documentoCom(
                "Nome: Joao da Silva\n"
                        + "CPF: 529.982.247-25\n"
                        + "Endereco: Rua das Flores, 150\n"
                        + "Data de emissao: 10/08/2026"));

        assertFalse(resposta.resultado().aprovado());
        assertTrue(resposta.resultado().pendencias().contains("CEP ausente ou invalido"));
    }

    @Test
    void aprovaDocumentoDeIdentificacaoComDadosMinimos() {
        RespostaValidacao resposta = service.validar(new DadosDocumento(
                "cnh.png",
                "image/png",
                "Nome: Joao da Silva\nCPF: 529.982.247-25\nCNH: 123456789",
                null,
                TipoDocumento.DOCUMENTO_IDENTIFICACAO));

        assertTrue(resposta.resultado().aprovado());
        assertTrue(resposta.resultado().regrasAtendidas().contains("Numero do documento identificado"));
    }

    @Test
    void rejeitaDocumentoDeIdentificacaoVencido() {
        RespostaValidacao resposta = service.validar(new DadosDocumento(
                "cnh.png",
                "image/png",
                "Nome: Joao da Silva\nCPF: 529.982.247-25\nCNH: 123456789\nValidade: 01/01/2026",
                null,
                TipoDocumento.DOCUMENTO_IDENTIFICACAO));

        assertFalse(resposta.resultado().aprovado());
        assertTrue(resposta.resultado().pendencias().contains("Documento de identificacao vencido"));
    }

    @Test
    void validaRendaPositivaEDataRecenteNoComprovanteDeRenda() {
        RespostaValidacao resposta = service.validar(new DadosDocumento(
                "holerite.png",
                "image/png",
                "Nome: Joao da Silva\nCPF: 529.982.247-25\nRenda mensal: R$ 2.500,00\n"
                        + "Data de emissao: 10/08/2026",
                null,
                TipoDocumento.COMPROVANTE_RENDA));

        assertTrue(resposta.resultado().aprovado());
        assertTrue(resposta.resultado().regrasAtendidas().contains("Renda mensal identificada"));
    }

    @Test
    void rejeitaRendaIgualAZero() {
        RespostaValidacao resposta = service.validar(new DadosDocumento(
                "holerite.png",
                "image/png",
                "Nome: Joao da Silva\nCPF: 529.982.247-25\nRenda mensal: 0,00\n"
                        + "Data de emissao: 10/08/2026",
                null,
                TipoDocumento.COMPROVANTE_RENDA));

        assertFalse(resposta.resultado().aprovado());
        assertTrue(resposta.resultado().pendencias().contains("Renda mensal deve ser maior que zero"));
    }

    @Test
    void processaImagemPeloOcrEAplicaRegrasDoTipoSelecionado() {
        TesseractOcrService ocr = mock(TesseractOcrService.class);
        when(ocr.isConfigurado()).thenReturn(true);
        when(ocr.extrairTexto(any(byte[].class), eq("image/png"))).thenReturn(
                "NOME DO CLIENTE Joao da Silva\n"
                        + "CPF/CNPJ 529.982.247-25\n"
                        + "ENDERECO DA UNIDADE CONSUMIDORA Rua das Flores, 150\n"
                        + "CEP 46430-000\n"
                        + "EMISSAO 01/11/2026");
        OpenCvPreprocessamentoService openCv = mock(OpenCvPreprocessamentoService.class);
        InterpretadorIaService ia = mock(InterpretadorIaService.class);
        DocumentoValidacaoService serviceComOcr = new DocumentoValidacaoService(
                new ExtratorDadosService(),
                new ValidacaoRegrasService(Clock.fixed(
                        Instant.parse("2026-11-04T12:00:00Z"),
                        ZoneOffset.UTC)),
                ocr,
                openCv,
                ia);

        RespostaValidacao resposta = serviceComOcr.validarImagem(
                TipoDocumento.COMPROVANTE_RESIDENCIA,
                "comprovante.png",
                "image/png",
                new byte[]{1, 2, 3});

        assertTrue(resposta.resultado().aprovado());
        assertEquals("Joao da Silva", resposta.dadosExtraidos().nome());
        assertEquals("529.982.247-25", resposta.dadosExtraidos().cpf());
        assertEquals("Rua das Flores, 150", resposta.dadosExtraidos().endereco());
        assertEquals("46430-000", resposta.dadosExtraidos().cep());
        assertEquals("2026-11-01", resposta.dadosExtraidos().dataEmissao().toString());
        assertTrue(resposta.etapas().stream().anyMatch(etapa -> etapa.nome().equals("OCR")));
        verify(ocr).extrairTexto(any(byte[].class), eq("image/png"));
    }

    @Test
    void naoReprovaImagemQuandoExtracaoNaoReconheceNenhumCampo() {
        TesseractOcrService ocr = mock(TesseractOcrService.class);
        when(ocr.isConfigurado()).thenReturn(true);
        when(ocr.extrairTexto(any(byte[].class), eq("image/png"))).thenReturn("texto legivel sem campos identificados");
        DocumentoValidacaoService serviceComOcr = new DocumentoValidacaoService(
                new ExtratorDadosService(),
                new ValidacaoRegrasService(Clock.systemUTC()),
                ocr,
                mock(OpenCvPreprocessamentoService.class),
                mock(InterpretadorIaService.class));

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> serviceComOcr.validarImagem(
                        TipoDocumento.COMPROVANTE_RESIDENCIA,
                        "comprovante.png",
                        "image/png",
                        new byte[]{1, 2, 3}));

        assertEquals(422, erro.getStatusCode().value());
        assertTrue(erro.getReason().contains("nenhum campo foi reconhecido"));
    }

    @Test
    void rejeitaCpfComDigitosVerificadoresInvalidos() {
        RespostaValidacao resposta = service.validar(documentoCom(
                "Nome: Joao da Silva\n"
                        + "CPF: 529.982.247-24\n"
                        + "Endereco: Rua das Flores, 150\n"
                        + "CEP: 46430-000\n"
                        + "Cidade: Guanambi - BA\n"
                        + "Data de emissao: 10/08/2026"));

        assertFalse(resposta.resultado().aprovado());
        assertTrue(resposta.resultado().pendencias().contains("CPF invalido"));
    }

    @Test
    void rejeitaCpfComTodosOsDigitosIguais() {
        RespostaValidacao resposta = service.validar(documentoCom(
                "Nome: Joao da Silva\n"
                        + "CPF: 111.111.111-11\n"
                        + "Endereco: Rua das Flores, 150\n"
                        + "CEP: 46430-000\n"
                        + "Cidade: Guanambi - BA\n"
                        + "Data de emissao: 10/08/2026"));

        assertFalse(resposta.resultado().aprovado());
        assertTrue(resposta.resultado().pendencias().contains("CPF invalido"));
    }

    @Test
    void aceitaCpfValidoSemFormatacao() {
        RespostaValidacao resposta = service.validar(documentoCom(
                "Nome: Joao da Silva\n"
                        + "CPF: 52998224725\n"
                        + "Endereco: Rua das Flores, 150\n"
                        + "CEP: 46430-000\n"
                        + "Cidade: Guanambi - BA\n"
                        + "Data de emissao: 10/08/2026"));

        assertTrue(resposta.resultado().aprovado());
        assertTrue(resposta.resultado().regrasAtendidas().contains("CPF valido"));
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
        assertTrue(resposta.etapas().get(2).descricao().contains("Fallback local"));
    }

    @Test
    void rejeitaImagemComBase64InvalidoQuandoTesseractEstaConfigurado() {
        DocumentoValidacaoService serviceComOcr = new DocumentoValidacaoService(
                new ExtratorDadosService(),
                new ValidacaoRegrasService(Clock.systemUTC()),
                new TesseractOcrService("tesseract", "", "por", 11),
                new OpenCvPreprocessamentoService(""),
                new InterpretadorIaService(
                        org.springframework.web.client.RestClient.builder(),
                        new ObjectMapper(),
                        "",
                        "gpt-4o-mini",
                        "https://api.openai.com/v1"));
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
