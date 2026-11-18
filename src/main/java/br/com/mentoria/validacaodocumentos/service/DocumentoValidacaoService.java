package br.com.mentoria.validacaodocumentos.service;

import br.com.mentoria.validacaodocumentos.documento.DadosDocumento;
import br.com.mentoria.validacaodocumentos.documento.DadosExtraidos;
import br.com.mentoria.validacaodocumentos.documento.EtapaProcessamento;
import br.com.mentoria.validacaodocumentos.documento.RespostaValidacao;
import br.com.mentoria.validacaodocumentos.documento.ResultadoValidacao;
import br.com.mentoria.validacaodocumentos.documento.TipoDocumento;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.List;

@Service
public class DocumentoValidacaoService {

    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    private final ExtratorDadosService extratorDadosService;
    private final ValidacaoRegrasService validacaoRegrasService;
    private final TesseractOcrService tesseractOcrService;
    private final OpenCvPreprocessamentoService openCvPreprocessamentoService;
    private final InterpretadorIaService interpretadorIaService;

    public DocumentoValidacaoService(
            ExtratorDadosService extratorDadosService,
            ValidacaoRegrasService validacaoRegrasService,
            TesseractOcrService tesseractOcrService,
            OpenCvPreprocessamentoService openCvPreprocessamentoService,
            InterpretadorIaService interpretadorIaService) {
        this.extratorDadosService = extratorDadosService;
        this.validacaoRegrasService = validacaoRegrasService;
        this.tesseractOcrService = tesseractOcrService;
        this.openCvPreprocessamentoService = openCvPreprocessamentoService;
        this.interpretadorIaService = interpretadorIaService;
    }

    public RespostaValidacao validar(DadosDocumento documento) {
        byte[] imagem = null;
        if (StringUtils.hasText(documento.imagemBase64()) && tesseractOcrService.isConfigurado()) {
            try {
                imagem = Base64.getDecoder().decode(documento.imagemBase64());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "imagemBase64 não contém um valor Base64 válido.",
                        e);
            }
        }
        return validar(documento, imagem, false);
    }

    public RespostaValidacao validarImagem(
            TipoDocumento tipoDocumento,
            String nomeArquivo,
            String mimeType,
            byte[] imagem) {
        if (imagem == null || imagem.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo enviado está vazio.");
        }
        if (imagem.length > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "O arquivo excede o limite de 10 MB.");
        }
        if (!tesseractOcrService.isConfigurado()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Configure app.ocr.command para processar o upload de imagens.");
        }
        DadosDocumento documento = new DadosDocumento(nomeArquivo, mimeType, null, null, tipoDocumento);
        return validar(documento, imagem, true);
    }

    private RespostaValidacao validar(DadosDocumento documento, byte[] imagem, boolean upload) {
        String textoDocumento = documento.conteudoDocumento();
        boolean ocrExecutado = false;
        boolean openCvExecutado = false;
        if (imagem != null) {
            if (imagem.length == 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "imagemBase64 não pode representar uma imagem vazia.");
            }
            if (openCvPreprocessamentoService.isConfigurado()) {
                imagem = openCvPreprocessamentoService.preprocessar(imagem, documento.mimeType());
                openCvExecutado = true;
            }
            textoDocumento = tesseractOcrService.extrairTexto(imagem, documento.mimeType());
            ocrExecutado = true;
        }

        if (!StringUtils.hasText(textoDocumento)) {
            throw new ResponseStatusException(
                    upload ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_REQUEST,
                    tesseractOcrService.isConfigurado()
                            ? "Envie conteudoDocumento ou uma imagem em imagemBase64."
                            : "Envie conteudoDocumento ou configure app.ocr.command para processar imagens.");
        }

        boolean iaOpenAiExecutada = interpretadorIaService.isConfigurado();
        DadosExtraidos dadosExtraidos = iaOpenAiExecutada
                ? interpretadorIaService.interpretar(textoDocumento, documento.tipoDocumento())
                : extratorDadosService.extrair(textoDocumento);
        ResultadoValidacao resultado = validacaoRegrasService.validar(documento.tipoDocumento(), dadosExtraidos);
        List<EtapaProcessamento> etapas = new java.util.ArrayList<>();
        etapas.add(new EtapaProcessamento(1, "Documento", "Entrada do arquivo para o pipeline",
                documento.nomeArquivo()));
        int ordem = 2;
        if (ocrExecutado) {
            etapas.add(new EtapaProcessamento(ordem++, "OpenCV",
                    openCvExecutado
                            ? "Pré-processamento executado localmente"
                            : "OpenCV não configurado; imagem original preservada",
                    openCvExecutado ? "Imagem pré-processada" : "Imagem original encaminhada ao OCR"));
        }
        etapas.add(new EtapaProcessamento(ordem++, "OCR",
                ocrExecutado ? "Tesseract executado localmente" : "Fallback local: texto enviado diretamente no request",
                textoDocumento.length() + " caracteres recebidos"));
        etapas.add(new EtapaProcessamento(ordem++, "IA",
                iaOpenAiExecutada
                        ? "Interpretação estruturada pela API OpenAI"
                        : "Fallback local: extração dos campos por padrões de texto",
                iaOpenAiExecutada ? "Resposta estruturada recebida" : "Extração estruturada concluída"));
        etapas.add(new EtapaProcessamento(ordem, "Regras de negócio",
                "Aplicação das regras específicas do tipo de documento", resultado.status()));
        return new RespostaValidacao(
                null,
                documento.tipoDocumento(),
                documento.nomeArquivo(),
                null,
                List.copyOf(etapas),
                dadosExtraidos,
                resultado);
    }
}
