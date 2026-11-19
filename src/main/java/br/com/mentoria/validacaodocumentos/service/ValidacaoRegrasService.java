package br.com.mentoria.validacaodocumentos.service;

import br.com.mentoria.validacaodocumentos.documento.DadosExtraidos;
import br.com.mentoria.validacaodocumentos.documento.ResultadoValidacao;
import br.com.mentoria.validacaodocumentos.documento.TipoDocumento;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ValidacaoRegrasService {

    private final Clock clock;

    public ValidacaoRegrasService(Clock clock) {
        this.clock = clock;
    }

    public ResultadoValidacao validar(DadosExtraidos dadosDocumento) {
        return validar(TipoDocumento.COMPROVANTE_RESIDENCIA, dadosDocumento);
    }

    public ResultadoValidacao validar(TipoDocumento tipoDocumento, DadosExtraidos dadosDocumento) {
        List<String> regrasAtendidas = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();

        validarCampo(dadosDocumento.nome(), "Nome identificado", "Nome nao encontrado",
                regrasAtendidas, pendencias);
        validarCpf(dadosDocumento.cpf(), regrasAtendidas, pendencias);

        switch (tipoDocumento) {
            case COMPROVANTE_RESIDENCIA -> validarComprovanteResidencia(dadosDocumento, regrasAtendidas, pendencias);
            case DOCUMENTO_IDENTIFICACAO -> validarDocumentoIdentificacao(
                    dadosDocumento, regrasAtendidas, pendencias);
            case COMPROVANTE_RENDA -> validarComprovanteRenda(dadosDocumento, regrasAtendidas, pendencias);
        }

        boolean aprovado = pendencias.isEmpty();
        return new ResultadoValidacao(
                aprovado,
                aprovado ? "APROVADO" : "REPROVADO",
                List.copyOf(regrasAtendidas),
                List.copyOf(pendencias));
    }

    private void validarComprovanteResidencia(
            DadosExtraidos dados,
            List<String> regrasAtendidas,
            List<String> pendencias) {
        validarCampo(dados.endereco(), "Endereco encontrado", "Endereco nao encontrado",
                regrasAtendidas, pendencias);
        validarCep(dados.cep(), regrasAtendidas, pendencias);
        validarDataEmissao(dados.dataEmissao(), regrasAtendidas, pendencias);
    }

    private void validarDocumentoIdentificacao(
            DadosExtraidos dados,
            List<String> regrasAtendidas,
            List<String> pendencias) {
        LocalDate hoje = LocalDate.now(clock);
        if (dados.dataValidade() != null) {
            regrasAtendidas.add("Data de validade impressa reconhecida");
        }
        if (dados.dataEmissao() == null) {
            regrasAtendidas.add("Validade nao avaliada por ausencia de data de emissao");
            return;
        }
        if (dados.dataEmissao().isAfter(hoje)) {
            pendencias.add("Data de emissao futura");
            return;
        }

        LocalDate validade = dados.dataEmissao().plusYears(10);
        regrasAtendidas.add("Validade de 10 anos calculada desde a emissao");
        if (validade.isBefore(hoje)) {
            pendencias.add("Documento de identificacao vencido");
        } else {
            regrasAtendidas.add("Documento de identificacao dentro da validade");
        }
    }

    private void validarComprovanteRenda(
            DadosExtraidos dados,
            List<String> regrasAtendidas,
            List<String> pendencias) {
        validarRenda(dados.rendaMensal(), regrasAtendidas, pendencias);
        validarDataEmissao(dados.dataEmissao(), regrasAtendidas, pendencias);
    }

    private void validarDataEmissao(
            LocalDate dataEmissao,
            List<String> regrasAtendidas,
            List<String> pendencias) {
        if (dataEmissao == null) {
            pendencias.add("Data de emissao nao encontrada");
            return;
        }

        regrasAtendidas.add("Data de emissao reconhecida");
        LocalDate hoje = LocalDate.now(clock);
        if (dataEmissao.isAfter(hoje)) {
            pendencias.add("Data de emissao futura");
        } else if (dataEmissao.isBefore(hoje.minusDays(90))) {
            pendencias.add("Emissao superior a 90 dias");
        } else {
            regrasAtendidas.add("Emissao dentro de 90 dias");
        }
    }

    private static void validarCep(
            String cep,
            List<String> regrasAtendidas,
            List<String> pendencias) {
        if (cep == null || !cep.matches("\\d{5}-?\\d{3}")) {
            pendencias.add("CEP ausente ou invalido");
        } else {
            regrasAtendidas.add("CEP identificado");
        }
    }

    private static void validarRenda(
            String renda,
            List<String> regrasAtendidas,
            List<String> pendencias) {
        if (renda == null || !renda.matches("(?:\\d+|\\d{1,3}(?:\\.\\d{3})+)(?:,\\d{1,2}|\\.\\d{1,2})?")) {
            pendencias.add("Renda mensal ausente ou invalida");
            return;
        }

        try {
            String valor = renda.contains(",")
                    ? renda.replace(".", "").replace(',', '.')
                    : renda;
            if (new BigDecimal(valor).signum() <= 0) {
                pendencias.add("Renda mensal deve ser maior que zero");
            } else {
                regrasAtendidas.add("Renda mensal identificada");
            }
        } catch (NumberFormatException e) {
            pendencias.add("Renda mensal ausente ou invalida");
        }
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
