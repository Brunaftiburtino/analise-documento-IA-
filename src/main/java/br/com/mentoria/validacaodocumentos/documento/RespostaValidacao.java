package br.com.mentoria.validacaodocumentos.documento;

import java.util.List;

public record RespostaValidacao(
        String nomeArquivo,
        List<EtapaProcessamento> etapas,
        DadosExtraidos dadosExtraidos,
        ResultadoValidacao resultado) {
}
