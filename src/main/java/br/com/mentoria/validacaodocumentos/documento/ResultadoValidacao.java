package br.com.mentoria.validacaodocumentos.documento;

import java.util.List;

public record ResultadoValidacao(
        boolean aprovado,
        String status,
        List<String> regrasAtendidas,
        List<String> pendencias) {
}
