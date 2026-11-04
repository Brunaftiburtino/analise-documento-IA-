package br.com.mentoria.validacaodocumentos.documento;

import jakarta.validation.constraints.NotBlank;

public record DadosDocumento(
        @NotBlank String nomeArquivo,
        @NotBlank String mimeType,
        String conteudoDocumento,
        String imagemBase64) {
}
