package br.com.mentoria.validacaodocumentos.controller;

import br.com.mentoria.validacaodocumentos.documento.DadosDocumento;
import br.com.mentoria.validacaodocumentos.documento.RespostaValidacao;
import br.com.mentoria.validacaodocumentos.documento.TipoDocumento;
import br.com.mentoria.validacaodocumentos.service.DocumentoValidacaoService;
import br.com.mentoria.validacaodocumentos.service.HistoricoValidacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private static final long LIMITE_UPLOAD_BYTES = 10L * 1024 * 1024;
    private static final Set<String> TIPOS_IMAGEM_SUPORTADOS = Set.of(
            "image/png", "image/jpeg", "image/tiff", "image/bmp");

    private final DocumentoValidacaoService documentoValidacaoService;
    private final HistoricoValidacaoService historicoValidacaoService;

    public DocumentoController(
            DocumentoValidacaoService documentoValidacaoService,
            HistoricoValidacaoService historicoValidacaoService) {
        this.documentoValidacaoService = documentoValidacaoService;
        this.historicoValidacaoService = historicoValidacaoService;
    }

    @GetMapping("/exemplo")
    public DadosDocumento exemplo() {
        String dataEmissao = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/uuuu"));
        String conteudo = """
                COMPROVANTE DE RESIDENCIA
                Nome: Joao da Silva
                CPF: 529.982.247-25
                Endereco: Rua das Flores, 150
                CEP: 46430-000
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
        return historicoValidacaoService.salvar(documentoValidacaoService.validar(documento));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public RespostaValidacao upload(
            @RequestPart("arquivo") MultipartFile arquivo,
            @RequestParam TipoDocumento tipoDocumento) {
        if (arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo enviado está vazio.");
        }
        if (arquivo.getSize() > LIMITE_UPLOAD_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "O arquivo excede o limite de 10 MB.");
        }

        String mimeType = arquivo.getContentType();
        if (mimeType == null
                || !TIPOS_IMAGEM_SUPORTADOS.contains(mimeType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "O upload aceita imagens PNG, JPEG, TIFF ou BMP.");
        }

        try {
            String nomeArquivo = nomeSeguro(arquivo.getOriginalFilename());
            RespostaValidacao resposta = documentoValidacaoService.validarImagem(
                    tipoDocumento,
                    nomeArquivo,
                    mimeType,
                    arquivo.getBytes());
            return historicoValidacaoService.salvar(resposta);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível ler o arquivo enviado.",
                    e);
        }
    }

    @GetMapping("/historico")
    public java.util.List<RespostaValidacao> listarHistorico() {
        return historicoValidacaoService.listarRecentes();
    }

    @GetMapping("/{id}")
    public RespostaValidacao consultar(@org.springframework.web.bind.annotation.PathVariable String id) {
        return historicoValidacaoService.consultar(id);
    }

    private static String nomeSeguro(String nomeOriginal) {
        String nome = StringUtils.hasText(nomeOriginal) ? nomeOriginal.replace('\\', '/') : "documento";
        String nomeArquivo = StringUtils.getFilename(StringUtils.cleanPath(nome));
        return StringUtils.hasText(nomeArquivo) ? nomeArquivo : "documento";
    }
}
