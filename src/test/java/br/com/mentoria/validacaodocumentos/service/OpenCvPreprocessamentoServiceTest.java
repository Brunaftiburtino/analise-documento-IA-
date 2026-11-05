package br.com.mentoria.validacaodocumentos.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenCvPreprocessamentoServiceTest {

    @Test
    void executaComandoComEntradaESaidaEDevolveImagemProcessada() {
        String javaExecutavel = Path.of(
                System.getProperty("java.home"),
                "bin",
                System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java"
        ).toString();
        String comando = "\"%s\" -cp \"%s\" %s".formatted(
                javaExecutavel,
                System.getProperty("java.class.path"),
                CopiarImagemCommand.class.getName());
        byte[] imagem = {1, 2, 3, 4};

        byte[] resultado = new OpenCvPreprocessamentoService(comando)
                .preprocessar(imagem, "image/png");

        assertArrayEquals(imagem, resultado);
    }

    @Test
    void rejeitaTipoDeImagemNaoSuportado() {
        OpenCvPreprocessamentoService service = new OpenCvPreprocessamentoService("comando");

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> service.preprocessar(new byte[]{1}, "application/pdf"));

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, erro.getStatusCode());
    }

    public static class CopiarImagemCommand {

        public static void main(String[] args) throws IOException {
            Files.copy(Path.of(args[0]), Path.of(args[1]), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
