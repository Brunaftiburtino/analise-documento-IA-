package br.com.mentoria.validacaodocumentos.documento;

import jakarta.validation.constraints.NotBlank;

public record DadosDocumento(
        @NotBlank String nomeArquivo,
        @NotBlank String mimeType,
        String conteudoDocumento,
        String imagemBase64,
        TipoDocumento tipoDocumento) {

    public DadosDocumento(
            String nomeArquivo,
            String mimeType,
            String conteudoDocumento,
            String imagemBase64) {
        this(nomeArquivo, mimeType, conteudoDocumento, imagemBase64, TipoDocumento.COMPROVANTE_RESIDENCIA);
    }

    public DadosDocumento {
        if (tipoDocumento == null) {
            tipoDocumento = TipoDocumento.COMPROVANTE_RESIDENCIA;
        }
    }
}
