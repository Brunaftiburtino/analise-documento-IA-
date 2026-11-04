package br.com.mentoria.validacaodocumentos.service;

import br.com.mentoria.validacaodocumentos.documento.DadosDocumento;
import br.com.mentoria.validacaodocumentos.documento.DadosExtraidos;
import br.com.mentoria.validacaodocumentos.documento.EtapaProcessamento;
import br.com.mentoria.validacaodocumentos.documento.RespostaValidacao;
import br.com.mentoria.validacaodocumentos.documento.ResultadoValidacao;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.List;

@Service
public class DocumentoValidacaoService {

    private final ExtratorDadosService extratorDadosService;
    private final ValidacaoRegrasService validacaoRegrasService;
    private final TesseractOcrService tesseractOcrService;

    public DocumentoValidacaoService(
            ExtratorDadosService extratorDadosService,
            ValidacaoRegrasService validacaoRegrasService,
            TesseractOcrService tesseractOcrService) {
        this.extratorDadosService = extratorDadosService;
        this.validacaoRegrasService = validacaoRegrasService;
        this.tesseractOcrService = tesseractOcrService;
    }

    public RespostaValidacao validar(DadosDocumento documento) {
        String textoDocumento = documento.conteudoDocumento();
        boolean ocrExecutado = false;
        if (StringUtils.hasText(documento.imagemBase64()) && tesseractOcrService.isConfigurado()) {
            byte[] imagem;
            try {
                imagem = Base64.getDecoder().decode(documento.imagemBase64());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "imagemBase64 não contém um valor Base64 válido.",
                        e);
            }
            if (imagem.length == 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "imagemBase64 não pode representar uma imagem vazia.");
            }
            textoDocumento = tesseractOcrService.extrairTexto(imagem, documento.mimeType());
            ocrExecutado = true;
        }

        if (!StringUtils.hasText(textoDocumento)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    tesseractOcrService.isConfigurado()
                            ? "Envie conteudoDocumento ou uma imagem em imagemBase64."
                            : "Envie conteudoDocumento ou configure app.ocr.command para processar imagens.");
        }

        DadosExtraidos dadosExtraidos = extratorDadosService.extrair(textoDocumento);
        ResultadoValidacao resultado = validacaoRegrasService.validar(dadosExtraidos);
        List<EtapaProcessamento> etapas = List.of(
                new EtapaProcessamento(1, "Documento", "Entrada do arquivo para o pipeline",
                        documento.nomeArquivo()),
                new EtapaProcessamento(2, "OCR",
                        ocrExecutado ? "Tesseract executado localmente" : "Fallback local: texto enviado diretamente no request",
                        textoDocumento.length() + " caracteres recebidos"),
                new EtapaProcessamento(3, "IA", "Fallback local: extração dos campos por padrões de texto",
                        "Extração estruturada concluída"),
                new EtapaProcessamento(4, "Regras de negócio", "Validação dos campos e da data de emissão",
                        resultado.status()));
        return new RespostaValidacao(documento.nomeArquivo(), etapas, dadosExtraidos, resultado);
    }
}
