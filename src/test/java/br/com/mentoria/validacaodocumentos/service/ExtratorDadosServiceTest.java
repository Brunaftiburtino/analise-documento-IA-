package br.com.mentoria.validacaodocumentos.service;

import br.com.mentoria.validacaodocumentos.documento.DadosExtraidos;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExtratorDadosServiceTest {

    private final ExtratorDadosService service = new ExtratorDadosService();

    @Test
    void extraiCamposDeRotulosSemDoisPontosEComNomeSocial() {
        DadosExtraidos dados = service.extrair(
                "NOME SOCIAL Ana Maria de Souza\n"
                        + "CPF/CNPJ 529.982.247-25\n"
                        + "ENDERECO DA UNIDADE CONSUMIDORA Rua das Flores, 150\n"
                        + "CEP 46430-000\n"
                        + "EMISSAO 01/11/2026");

        assertEquals("Ana Maria de Souza", dados.nome());
        assertEquals("529.982.247-25", dados.cpf());
        assertEquals("Rua das Flores, 150", dados.endereco());
        assertEquals("46430-000", dados.cep());
        assertEquals(LocalDate.of(2026, 11, 1), dados.dataEmissao());
    }

    @Test
    void extraiValorNaLinhaSeguinteSemMisturarCampos() {
        DadosExtraidos dados = service.extrair(
                "Nome:\n"
                        + "Ana Maria\n"
                        + "CPF:\n"
                        + "52998224725\n"
                        + "Logradouro:\n"
                        + "Rua das Flores, 150\n"
                        + "CEP:\n"
                        + "46430-000\n"
                        + "Data de emissao:\n"
                        + "2026-11-01");

        assertEquals("Ana Maria", dados.nome());
        assertEquals("529.982.247-25", dados.cpf());
        assertEquals("Rua das Flores, 150", dados.endereco());
        assertEquals("46430-000", dados.cep());
        assertEquals(LocalDate.of(2026, 11, 1), dados.dataEmissao());
    }

    @Test
    void interrompeValorAntesDoProximoRotuloNaMesmaLinha() {
        DadosExtraidos dados = service.extrair(
                "Nome: Ana Maria CPF: 529.982.247-25 CEP: 46430-000");

        assertEquals("Ana Maria", dados.nome());
        assertEquals("529.982.247-25", dados.cpf());
        assertEquals("46430-000", dados.cep());
    }

    @Test
    void naoConfundeDataDeNascimentoNemNumeroSemRotuloComCamposObrigatorios() {
        DadosExtraidos dados = service.extrair(
                "Data de nascimento: 05/02/1990\n"
                        + "Conta: 46430-000\n"
                        + "Registro Geral: 123456789");

        assertNull(dados.dataEmissao());
        assertNull(dados.cep());
        assertEquals("123456789", dados.numeroDocumento());
    }

    @Test
    void reconheceDataDeApresentacaoComoDataDoComprovante() {
        DadosExtraidos dados = service.extrair("Data de apresentação 15/11/2026");

        assertEquals(LocalDate.of(2026, 11, 15), dados.dataEmissao());
    }
}
