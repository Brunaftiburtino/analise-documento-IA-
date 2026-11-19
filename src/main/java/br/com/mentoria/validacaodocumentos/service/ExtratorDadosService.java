package br.com.mentoria.validacaodocumentos.service;

import br.com.mentoria.validacaodocumentos.documento.DadosExtraidos;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExtratorDadosService {

    private static final String NOME_ROTULOS =
            "Nome(?:\\s+(?:(?:do|da)\\s+)?(?:cliente|titular|consumidor|portador|"
                    + "benefici[aá]rio|funcion[aá]rio|colaborador|completo|social))?"
                    + "|Name(?:\\s+(?:Social|Complete))?|Titular|Cliente|Consumidor|Benefici[aá]rio";
    private static final String CPF_ROTULOS =
            "CPF(?:\\s*/\\s*CNPJ)?(?:\\s+(?:(?:do|da)\\s+)?(?:cliente|titular|portador))?"
                    + "|CNPJ|Personal\\s+Number";
    private static final String ENDERECO_ROTULOS =
            "Endere[cç]o(?:\\s+(?:(?:da|do|de)\\s+)?(?:unidade(?:\\s+consumidora)?|"
                    + "cliente|titular|portador|im[oó]vel|instala[cç][aã]o))?|Logradouro|Address";
    private static final String CIDADE_ROTULOS = "Cidade|Munic[ií]pio|City|Municipality";
    private static final String CEP_ROTULOS = "CEP|ZIP\\s+Code|Postal\\s+Code";
    private static final String EMISSAO_ROTULOS =
            "Data\\s+de\\s+(?:emiss[aã]o|apresenta[cç][aã]o)"
                    + "|Emiss[aã]o|Apresenta[cç][aã]o|Expedi[cç][aã]o|Emitido\\s+em"
                    + "|Date\\s+of\\s+(?:Issue|Presentation)|Issue\\s+Date";
    private static final String VALIDADE_ROTULOS =
            "Data\\s+de\\s+validade|Validade|V[aá]lido\\s+at[eé]"
                    + "|Date\\s+of\\s+Expiry|Expiry\\s+Date|Expiration\\s+Date|Valid\\s+Until";
    private static final String NUMERO_DOCUMENTO_ROTULOS =
            "N[uú]mero\\s+do\\s+(?:documento|registro)|Registro(?:\\s+Geral)?|RG|CNH"
                    + "|Document\\s+Number|Registration\\s+Number";
    private static final String RENDA_ROTULOS =
            "Renda\\s+mensal|Sal[aá]rio(?:\\s+(?:bruto|l[ií]quido|base))?|"
                    + "Total\\s+de\\s+(?:proventos|vencimentos)|Valor\\s+l[ií]quido"
                    + "|Monthly\\s+Income|Salary";
    private static final String ROTULOS_CONTEXTO =
            "Data\\s+de\\s+nascimento|Nascimento|Date\\s+of\\s+Birth|Birth|Sexo|Sex|"
                    + "G[eê]nero|Gender|Nacionalidade|Nationality|Naturalidade|"
                    + "Place\\s+of\\s+Birth|Assinatura|Signature";
    private static final String TODOS_ROTULOS = String.join("|",
            NOME_ROTULOS,
            CPF_ROTULOS,
            ENDERECO_ROTULOS,
            CIDADE_ROTULOS,
            CEP_ROTULOS,
            EMISSAO_ROTULOS,
            VALIDADE_ROTULOS,
            NUMERO_DOCUMENTO_ROTULOS,
            RENDA_ROTULOS,
            ROTULOS_CONTEXTO);

    private static final Pattern CAMPO_NOME = campo(NOME_ROTULOS);
    private static final Pattern CAMPO_CPF = campo(CPF_ROTULOS);
    private static final Pattern CAMPO_ENDERECO = campo(ENDERECO_ROTULOS);
    private static final Pattern CAMPO_CIDADE = campo(CIDADE_ROTULOS);
    private static final Pattern CAMPO_CEP = campo(CEP_ROTULOS);
    private static final Pattern CAMPO_EMISSAO = campo(EMISSAO_ROTULOS);
    private static final Pattern CAMPO_VALIDADE = campo(VALIDADE_ROTULOS);
    private static final Pattern CAMPO_NUMERO_DOCUMENTO = campo(NUMERO_DOCUMENTO_ROTULOS);
    private static final Pattern CAMPO_RENDA = campo(RENDA_ROTULOS);
    private static final Pattern QUALQUER_ROTULO = Pattern.compile(
            "(?iu)^\\h*(?:" + TODOS_ROTULOS + ")\\b(?:\\h*[/|–-].*)?(?:\\h*[:：|].*)?$");
    private static final Pattern PROXIMO_ROTULO = Pattern.compile(
            "(?iu)\\h+(?=(?:" + TODOS_ROTULOS + ")\\b)");
    private static final Pattern CPF_VALOR = Pattern.compile(
            "(?<!\\d)(\\d{3}(?:[.\\h]?\\d{3}){2}[\\h-]?\\d{2})(?!\\d)");
    private static final Pattern CEP_VALOR = Pattern.compile(
            "(?<!\\d)(\\d{5})[\\h-]?(\\d{3})(?!\\d)");
    private static final Pattern DATA_VALOR = Pattern.compile(
            "(?<!\\d)(?:(\\d{1,2})\\s*[./-]\\s*(\\d{1,2})\\s*[./-]\\s*(\\d{4})"
                    + "|(\\d{4})-(\\d{2})-(\\d{2}))(?!\\d)");
    private static final Pattern NUMERO_DOCUMENTO_VALOR = Pattern.compile(
            "(?iu)(?=[\\p{Alnum}./-]{5,20})(?=[\\p{Alnum}./-]*\\d)"
                    + "[\\p{Alnum}][\\p{Alnum}./-]{4,19}");
    private static final Pattern RENDA_VALOR = Pattern.compile(
            "(?iu)(?:R\\s?\\$\\s*)?(\\d{1,3}(?:\\.\\d{3})*(?:,\\d{2})|\\d+(?:[.,]\\d{1,2})?)");
    private static final Pattern ALIAS_BILINGUE = Pattern.compile(
            "(?iu)^[\\h/|:：–-]*(?:Name(?:\\s+Social)?|Personal\\s+Number|"
                    + "Date\\s+of\\s+(?:Birth|Expiry|Issue|Presentation)|"
                    + "Expiry\\s+Date|Expiration\\s+Date|Valid\\s+Until|"
                    + "Address|City|Municipality|ZIP\\s+Code|Postal\\s+Code|"
                    + "Sex|Gender|Nationality|Place\\s+of\\s+Birth|Signature)"
                    + "\\b[\\h/|:：–-]*");
    private static final List<Pattern> CAMPOS = List.of(
            CAMPO_NOME,
            CAMPO_CPF,
            CAMPO_ENDERECO,
            CAMPO_CIDADE,
            CAMPO_CEP,
            CAMPO_EMISSAO,
            CAMPO_VALIDADE,
            CAMPO_NUMERO_DOCUMENTO,
            CAMPO_RENDA);

    public DadosExtraidos extrair(String texto) {
        String normalizado = normalizar(texto);
        return new DadosExtraidos(
                extrairCampo(normalizado, CAMPO_NOME),
                extrairCpf(normalizado),
                extrairCampo(normalizado, CAMPO_ENDERECO),
                extrairCampo(normalizado, CAMPO_CIDADE),
                extrairData(normalizado, CAMPO_EMISSAO),
                extrairCep(normalizado),
                extrairNumeroDocumento(normalizado),
                extrairData(normalizado, CAMPO_VALIDADE),
                extrairRenda(normalizado));
    }

    private static Pattern campo(String rotulos) {
        return Pattern.compile(
                "(?ium)(?:^|\\h)(?:" + rotulos + ")\\b\\h*"
                        + "(?:[:：|]\\h*|\\h+)?(?<valor>[^\\r\\n]*?)\\h*$");
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFC)
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\u00A0', ' ')
                .replaceAll("[|¦_]+", " ")
                .replaceAll("[\\t ]+", " ");
    }

    private static String extrairCampo(String texto, Pattern campo) {
        String[] linhas = texto.split("\n", -1);
        for (int indice = 0; indice < linhas.length; indice++) {
            Matcher matcher = campo.matcher(linhas[indice]);
            if (!matcher.find()) {
                continue;
            }

            String valor = limparAliasBilingue(limparValor(matcher.group("valor")));
            if (!valor.isEmpty()) {
                return cortarNoProximoRotulo(valor);
            }
            for (int proxima = indice + 1; proxima < linhas.length; proxima++) {
                String proximaLinha = limparAliasBilingue(limparValor(linhas[proxima]));
                if (proximaLinha.isEmpty()) {
                    continue;
                }
                if (ehRotulo(proximaLinha)) {
                    break;
                }
                return cortarNoProximoRotulo(proximaLinha);
            }
        }
        return null;
    }

    private static String limparValor(String valor) {
        return valor == null ? "" : valor.trim().replaceAll("^[：:]+\\h*", "");
    }

    private static String limparAliasBilingue(String valor) {
        return ALIAS_BILINGUE.matcher(valor).replaceFirst("").trim();
    }

    private static boolean ehRotulo(String linha) {
        if (QUALQUER_ROTULO.matcher(linha).matches()) {
            return true;
        }
        for (Pattern campo : CAMPOS) {
            if (campo.matcher(linha).find()) {
                return true;
            }
        }
        return false;
    }

    private static String cortarNoProximoRotulo(String valor) {
        Matcher matcher = PROXIMO_ROTULO.matcher(valor);
        return matcher.find() ? valor.substring(0, matcher.start()).trim() : valor.trim();
    }

    private static String extrairCpf(String texto) {
        String valor = extrairValorTipado(texto, CAMPO_CPF, CPF_VALOR, 1);
        if (valor == null) {
            return null;
        }
        String digitos = valor.replaceAll("\\D", "");
        return digitos.length() == 11
                ? digitos.substring(0, 3) + "." + digitos.substring(3, 6) + "."
                        + digitos.substring(6, 9) + "-" + digitos.substring(9)
                : null;
    }

    private static String extrairCep(String texto) {
        String valor = extrairValorTipado(texto, CAMPO_CEP, CEP_VALOR, 0);
        if (valor == null) {
            return null;
        }
        String digitos = valor.replaceAll("\\D", "");
        return digitos.length() == 8 ? digitos.substring(0, 5) + "-" + digitos.substring(5) : null;
    }

    private static LocalDate extrairData(String texto, Pattern campo) {
        String valor = extrairValorTipado(texto, campo, DATA_VALOR, 0);
        if (valor == null) {
            return null;
        }
        Matcher matcher = DATA_VALOR.matcher(valor);
        if (!matcher.find()) {
            return null;
        }
        try {
            if (matcher.group(1) != null) {
                return LocalDate.of(
                        Integer.parseInt(matcher.group(3)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(1)));
            }
            return LocalDate.of(
                    Integer.parseInt(matcher.group(4)),
                    Integer.parseInt(matcher.group(5)),
                    Integer.parseInt(matcher.group(6)));
        } catch (DateTimeException | NumberFormatException e) {
            return null;
        }
    }

    private static String extrairNumeroDocumento(String texto) {
        return extrairValorTipado(texto, CAMPO_NUMERO_DOCUMENTO, NUMERO_DOCUMENTO_VALOR, 0);
    }

    private static String extrairRenda(String texto) {
        return extrairValorTipado(texto, CAMPO_RENDA, RENDA_VALOR, 1);
    }

    private static String extrairValorTipado(
            String texto,
            Pattern rotulo,
            Pattern padraoValor,
            int grupoValor) {
        String[] linhas = texto.split("\n", -1);
        for (int indice = 0; indice < linhas.length; indice++) {
            Matcher matcherRotulo = rotulo.matcher(linhas[indice]);
            if (!matcherRotulo.find()) {
                continue;
            }

            String valorNaLinha = matcherRotulo.group("valor");
            Matcher matcherValor = padraoValor.matcher(valorNaLinha);
            if (matcherValor.find()) {
                return matcherValor.group(grupoValor);
            }

            for (int proxima = indice + 1; proxima < linhas.length; proxima++) {
                String linha = limparValor(linhas[proxima]);
                if (linha.isEmpty()) {
                    continue;
                }
                if (ehRotulo(linha)) {
                    break;
                }
                matcherValor = padraoValor.matcher(cortarNoProximoRotulo(linha));
                return matcherValor.find() ? matcherValor.group(grupoValor) : null;
            }
        }
        return null;
    }
}
