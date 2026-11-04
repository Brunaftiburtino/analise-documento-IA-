package br.com.mentoria.validacaodocumentos.service;

import br.com.mentoria.validacaodocumentos.documento.DadosExtraidos;
import br.com.mentoria.validacaodocumentos.documento.ResultadoValidacao;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ValidacaoRegrasService {

    private final Clock clock;

    public ValidacaoRegrasService(Clock clock) {
        this.clock = clock;
    }

    public ResultadoValidacao validar(DadosExtraidos dadosDocumento) {
        List<String> regrasAtendidas = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();

        validarCampo(dadosDocumento.nome(), "Nome identificado", "Nome nao encontrado",
                regrasAtendidas, pendencias);
        validarCampo(dadosDocumento.cpf(), "CPF encontrado", "CPF nao encontrado",
                regrasAtendidas, pendencias);
        validarCampo(dadosDocumento.endereco(), "Endereco encontrado", "Endereco nao encontrado",
                regrasAtendidas, pendencias);
        validarCampo(dadosDocumento.cidade(), "Cidade encontrada", "Cidade nao encontrada",
                regrasAtendidas, pendencias);

        if (dadosDocumento.dataEmissao() == null) {
            pendencias.add("Data de emissao nao encontrada");
        } else {
            regrasAtendidas.add("Data de emissao reconhecida");
            LocalDate limite = LocalDate.now(clock).minusDays(90);
            if (dadosDocumento.dataEmissao().isBefore(limite)) {
                pendencias.add("Emissao superior a 90 dias");
            } else {
                regrasAtendidas.add("Emissao dentro de 90 dias");
            }
        }

        boolean aprovado = pendencias.isEmpty();
        return new ResultadoValidacao(
                aprovado,
                aprovado ? "APROVADO" : "REPROVADO",
                List.copyOf(regrasAtendidas),
                List.copyOf(pendencias));
    }

    private static void validarCampo(
            String valor,
            String regraAtendida,
            String pendencia,
            List<String> regrasAtendidas,
            List<String> pendencias) {
        if (valor == null || valor.isBlank()) {
            pendencias.add(pendencia);
        } else {
            regrasAtendidas.add(regraAtendida);
        }
    }
}
