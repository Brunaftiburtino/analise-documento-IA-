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

import java.util.List;

@Service
public class DocumentoValidacaoService {

    private final ExtratorDadosService extratorDadosService;
    private final ValidacaoRegrasService validacaoRegrasService;

    public DocumentoValidacaoService(
            ExtratorDadosService extratorDadosService,
            ValidacaoRegrasService validacaoRegrasService) {
        this.extratorDadosService = extratorDadosService;
        this.validacaoRegrasService = validacaoRegrasService;
    }

    public RespostaValidacao validar(DadosDocumento documento) {
        if (!StringUtils.hasText(documento.conteudoDocumento())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nesta etapa, envie conteudoDocumento; o processamento de imagem será adicionado depois.");
        }

        DadosExtraidos dadosExtraidos = extratorDadosService.extrair(documento.conteudoDocumento());
        ResultadoValidacao resultado = validacaoRegrasService.validar(dadosExtraidos);
        List<EtapaProcessamento> etapas = List.of(
                new EtapaProcessamento(1, "Documento", "Entrada do arquivo para o pipeline",
                        documento.nomeArquivo()),
                new EtapaProcessamento(2, "OCR", "Fallback local: texto enviado diretamente no request",
                        documento.conteudoDocumento().length() + " caracteres recebidos"),
                new EtapaProcessamento(3, "IA", "Fallback local: extração dos campos por padrões de texto",
                        "Extração estruturada concluída"),
                new EtapaProcessamento(4, "Regras de negócio", "Validação dos campos e da data de emissão",
                        resultado.status()));
        return new RespostaValidacao(documento.nomeArquivo(), etapas, dadosExtraidos, resultado);
    }
}
