package br.com.mentoria.validacaodocumentos.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class TesseractOcrService {

    private static final long TIMEOUT_SECONDS = 60;

    private final String command;
    private final String tessdataPath;
    private final String language;

    public TesseractOcrService(
            @Value("${app.ocr.command:}") String command,
            @Value("${app.ocr.tessdata-path:}") String tessdataPath,
            @Value("${app.ocr.language:por}") String language) {
        this.command = command;
        this.tessdataPath = tessdataPath;
        this.language = language;
    }

    public boolean isConfigurado() {
        return StringUtils.hasText(command);
    }

    public String extrairTexto(byte[] imagem, String mimeType) {
        String extensao = extensaoPara(mimeType);
        try (ArquivosTemporarios temporarios = new ArquivosTemporarios()) {
            Path arquivoImagem = temporarios.criar("validacao-documento-", extensao);
            Path arquivoSaida = temporarios.criar("validacao-documento-ocr-", ".txt");
            Path arquivoErro = temporarios.criar("validacao-documento-ocr-erro-", ".txt");
            Files.write(arquivoImagem, imagem);

            List<String> argumentos = new ArrayList<>(List.of(
                    command,
                    arquivoImagem.toString(),
                    "stdout",
                    "-l",
                    language));
            if (StringUtils.hasText(tessdataPath)) {
                argumentos.add("--tessdata-dir");
                argumentos.add(tessdataPath);
            }

            Process processo;
            try {
                processo = new ProcessBuilder(argumentos)
                        .redirectOutput(arquivoSaida.toFile())
                        .redirectError(arquivoErro.toFile())
                        .start();
            } catch (IOException e) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Não foi possível iniciar o Tesseract. Verifique app.ocr.command.",
                        e);
            }

            boolean concluiu;
            try {
                concluiu = processo.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!concluiu) {
                    processo.destroyForcibly();
                    processo.waitFor();
                    throw new ResponseStatusException(
                            HttpStatus.GATEWAY_TIMEOUT,
                            "O Tesseract excedeu o limite de 60 segundos.");
                }
            } catch (InterruptedException e) {
                processo.destroyForcibly();
                Thread.currentThread().interrupt();
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "O processamento de OCR foi interrompido.",
                        e);
            }

            if (processo.exitValue() != 0) {
                String detalhe = Files.readString(arquivoErro, StandardCharsets.UTF_8).trim();
                String mensagem = "O Tesseract terminou com código " + processo.exitValue();
                if (StringUtils.hasText(detalhe)) {
                    mensagem += ": " + detalhe;
                }
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, mensagem);
            }

            return Files.readString(arquivoSaida, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao preparar ou ler os arquivos temporários do OCR.",
                    e);
        }
    }

    private static String extensaoPara(String mimeType) {
        String tipo = mimeType == null
                ? ""
                : mimeType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return switch (tipo) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/tiff" -> ".tif";
            case "image/bmp" -> ".bmp";
            default -> throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "O OCR aceita imagens PNG, JPEG, TIFF ou BMP.");
        };
    }

    private static final class ArquivosTemporarios implements AutoCloseable {

        private final List<Path> arquivos = new ArrayList<>();

        private Path criar(String prefixo, String sufixo) throws IOException {
            Path arquivo = Files.createTempFile(prefixo, sufixo);
            arquivos.add(arquivo);
            return arquivo;
        }

        @Override
        public void close() throws IOException {
            IOException falha = null;
            for (Path arquivo : arquivos) {
                try {
                    Files.deleteIfExists(arquivo);
                } catch (IOException e) {
                    if (falha == null) {
                        falha = e;
                    } else {
                        falha.addSuppressed(e);
                    }
                }
            }
            if (falha != null) {
                throw falha;
            }
        }
    }
}
