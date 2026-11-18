package br.com.mentoria.validacaodocumentos.documento;

import java.util.List;
import java.time.LocalDateTime;

public record RespostaValidacao(
        String id,
        TipoDocumento tipoDocumento,
        String nomeArquivo,
        LocalDateTime criadaEm,
        List<EtapaProcessamento> etapas,
        DadosExtraidos dadosExtraidos,
        ResultadoValidacao resultado) {

    public RespostaValidacao(
            String nomeArquivo,
            List<EtapaProcessamento> etapas,
            DadosExtraidos dadosExtraidos,
            ResultadoValidacao resultado) {
        this(null, TipoDocumento.COMPROVANTE_RESIDENCIA, nomeArquivo, null, etapas, dadosExtraidos, resultado);
    }
}
