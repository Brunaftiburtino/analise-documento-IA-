package br.com.mentoria.validacaodocumentos.documento;

import java.time.LocalDate;

public record DadosExtraidos(
        String nome,
        String cpf,
        String endereco,
        String cidade,
        LocalDate dataEmissao,
        String cep,
        String numeroDocumento,
        LocalDate dataValidade,
        String rendaMensal) {

    public DadosExtraidos(
            String nome,
            String cpf,
            String endereco,
            String cidade,
            LocalDate dataEmissao) {
        this(nome, cpf, endereco, cidade, dataEmissao, null, null, null, null);
    }
}
