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
        validarCpf(dadosDocumento.cpf(), regrasAtendidas, pendencias);
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

    private static void validarCpf(
            String valor,
            List<String> regrasAtendidas,
            List<String> pendencias) {
        if (valor == null || valor.isBlank()) {
            pendencias.add("CPF nao encontrado");
        } else if (cpfValido(valor)) {
            regrasAtendidas.add("CPF valido");
        } else {
            pendencias.add("CPF invalido");
        }
    }

    private static boolean cpfValido(String valor) {
        String cpf = valor.trim();
        if (cpf.matches("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}")) {
            cpf = cpf.replace(".", "").replace("-", "");
        } else if (!cpf.matches("\\d{11}")) {
            return false;
        }

        if (cpf.chars().distinct().count() == 1) {
            return false;
        }

        int primeiroDigito = calcularDigito(cpf, 9, 10);
        int segundoDigito = calcularDigito(cpf, 10, 11);
        return primeiroDigito == cpf.charAt(9) - '0'
                && segundoDigito == cpf.charAt(10) - '0';
    }

    private static int calcularDigito(String cpf, int quantidadeDigitos, int pesoInicial) {
        int soma = 0;
        for (int indice = 0; indice < quantidadeDigitos; indice++) {
            soma += (cpf.charAt(indice) - '0') * (pesoInicial - indice);
        }
        int digito = (soma * 10) % 11;
        return digito == 10 ? 0 : digito;
    }
}
