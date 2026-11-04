package br.com.mentoria.validacaodocumentos.documento;

public record EtapaProcessamento(
        int ordem,
        String nome,
        String descricao,
        String resultado) {
}
