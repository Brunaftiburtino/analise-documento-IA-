package br.com.mentoria.validacaodocumentos.service;

import br.com.mentoria.validacaodocumentos.documento.DadosExtraidos;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExtratorDadosService {

    private static final Pattern NOME = campo("Nome");
    private static final Pattern CPF = campo("CPF");
    private static final Pattern ENDERECO = campo("Endere[cç]o");
    private static final Pattern CIDADE = campo("Cidade");
    private static final Pattern DATA_EMISSAO = Pattern.compile(
            "(?im)^\\s*Data\\s+de\\s+emiss[aã]o\\s*:\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4}|\\d{4}-\\d{2}-\\d{2})\\s*$");
    private static final DateTimeFormatter DATA_BARRA = DateTimeFormatter.ofPattern("d/M/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATA_HIFEN = DateTimeFormatter.ofPattern("d-M-uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    public DadosExtraidos extrair(String texto) {
        String data = capturar(DATA_EMISSAO, texto);
        return new DadosExtraidos(
                capturar(NOME, texto),
                capturar(CPF, texto),
                capturar(ENDERECO, texto),
                capturar(CIDADE, texto),
                converterData(data));
    }

    private static Pattern campo(String rotulo) {
        return Pattern.compile("(?im)^\\s*" + rotulo + "\\s*:\\s*(.+?)\\s*$");
    }

    private static String capturar(Pattern pattern, String texto) {
        Matcher matcher = pattern.matcher(texto);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private static LocalDate converterData(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            if (valor.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(valor, DateTimeFormatter.ISO_LOCAL_DATE);
            }
            DateTimeFormatter formato = valor.contains("/") ? DATA_BARRA : DATA_HIFEN;
            return LocalDate.parse(valor, formato);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
