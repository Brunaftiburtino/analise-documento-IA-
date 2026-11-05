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
public class OpenCvPreprocessamentoService {

    private static final long TIMEOUT_SECONDS = 60;

    private final String command;

    public OpenCvPreprocessamentoService(@Value("${app.opencv.command:}") String command) {
        this.command = command;
    }

    public boolean isConfigurado() {
        return StringUtils.hasText(command);
    }

    public byte[] preprocessar(byte[] imagem, String mimeType) {
        String extensao = extensaoPara(mimeType);
        List<String> argumentosComando = separarComando(command);

        try (ArquivosTemporarios temporarios = new ArquivosTemporarios()) {
            Path arquivoEntrada = temporarios.criar("validacao-documento-opencv-entrada-", extensao);
            Path arquivoSaida = temporarios.criar("validacao-documento-opencv-saida-", extensao);
            Path arquivoErro = temporarios.criar("validacao-documento-opencv-erro-", ".txt");
            Files.write(arquivoEntrada, imagem);

            List<String> argumentos = new ArrayList<>(argumentosComando);
            argumentos.add(arquivoEntrada.toString());
            argumentos.add(arquivoSaida.toString());

            Process processo;
            try {
                processo = new ProcessBuilder(argumentos)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(arquivoErro.toFile())
                        .start();
            } catch (IOException e) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Não foi possível iniciar o pré-processamento OpenCV. Verifique app.opencv.command.",
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
                            "O pré-processamento OpenCV excedeu o limite de 60 segundos.");
                }
            } catch (InterruptedException e) {
                processo.destroyForcibly();
                Thread.currentThread().interrupt();
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "O pré-processamento OpenCV foi interrompido.",
                        e);
            }

            if (processo.exitValue() != 0) {
                String detalhe = Files.readString(arquivoErro, StandardCharsets.UTF_8).trim();
                String mensagem = "O comando OpenCV terminou com código " + processo.exitValue();
                if (StringUtils.hasText(detalhe)) {
                    mensagem += ": " + detalhe;
                }
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, mensagem);
            }

            byte[] imagemProcessada = Files.readAllBytes(arquivoSaida);
            if (imagemProcessada.length == 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "O comando OpenCV não gerou uma imagem processada.");
            }
            return imagemProcessada;
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao preparar ou ler os arquivos temporários do OpenCV.",
                    e);
        }
    }

    private static List<String> separarComando(String comando) {
        List<String> argumentos = new ArrayList<>();
        StringBuilder argumento = new StringBuilder();
        char aspas = 0;
        boolean argumentoIniciado = false;

        for (int indice = 0; indice < comando.length(); indice++) {
            char caractere = comando.charAt(indice);
            if (aspas != 0) {
                if (caractere == aspas) {
                    aspas = 0;
                } else {
                    argumento.append(caractere);
                }
            } else if (caractere == '"' || caractere == '\'') {
                aspas = caractere;
                argumentoIniciado = true;
            } else if (Character.isWhitespace(caractere)) {
                if (argumentoIniciado) {
                    argumentos.add(argumento.toString());
                    argumento.setLength(0);
                    argumentoIniciado = false;
                }
            } else {
                argumento.append(caractere);
                argumentoIniciado = true;
            }
        }

        if (aspas != 0) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "app.opencv.command contém aspas não fechadas.");
        }
        if (argumentoIniciado) {
            argumentos.add(argumento.toString());
        }
        if (argumentos.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "app.opencv.command não contém um executável.");
        }
        return argumentos;
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
                    "O pré-processamento OpenCV aceita imagens PNG, JPEG, TIFF ou BMP.");
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
