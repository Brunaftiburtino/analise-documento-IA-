package br.com.mentoria.validacaodocumentos.documento;

import java.time.LocalDate;

public record DadosExtraidos(
        String nome,
        String cpf,
        String endereco,
        String cidade,
        LocalDate dataEmissao) {
}
