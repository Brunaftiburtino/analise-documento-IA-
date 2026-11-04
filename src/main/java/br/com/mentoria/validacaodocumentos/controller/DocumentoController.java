package br.com.mentoria.validacaodocumentos.controller;

import br.com.mentoria.validacaodocumentos.documento.DadosDocumento;
import br.com.mentoria.validacaodocumentos.documento.RespostaValidacao;
import br.com.mentoria.validacaodocumentos.service.DocumentoValidacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoValidacaoService documentoValidacaoService;

    public DocumentoController(DocumentoValidacaoService documentoValidacaoService) {
        this.documentoValidacaoService = documentoValidacaoService;
    }

    @GetMapping("/exemplo")
    public DadosDocumento exemplo() {
        String dataEmissao = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/uuuu"));
        String conteudo = """
                COMPROVANTE DE RESIDENCIA
                Nome: Joao da Silva
                CPF: 123.456.789-00
                Endereco: Rua das Flores, 150
                Cidade: Guanambi - BA
                Data de emissao: %s
                """.formatted(dataEmissao);
        return new DadosDocumento(
                "comprovante-residencia.pdf",
                "application/pdf",
                conteudo,
                null);
    }

    @PostMapping("/validar")
    @ResponseStatus(HttpStatus.OK)
    public RespostaValidacao validar(@Valid @RequestBody DadosDocumento documento) {
        return documentoValidacaoService.validar(documento);
    }
}
