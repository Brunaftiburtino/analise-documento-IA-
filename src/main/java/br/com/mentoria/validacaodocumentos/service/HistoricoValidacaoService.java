package br.com.mentoria.validacaodocumentos.service;

import br.com.mentoria.validacaodocumentos.documento.RespostaValidacao;
import br.com.mentoria.validacaodocumentos.historico.ValidacaoDocumentoEntity;
import br.com.mentoria.validacaodocumentos.historico.ValidacaoDocumentoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class HistoricoValidacaoService {

    private final ValidacaoDocumentoRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public HistoricoValidacaoService(
            ValidacaoDocumentoRepository repository,
            ObjectMapper objectMapper,
            Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public RespostaValidacao salvar(RespostaValidacao resposta) {
        String id = UUID.randomUUID().toString();
        LocalDateTime criadaEm = LocalDateTime.now(clock);
        RespostaValidacao persistida = new RespostaValidacao(
                id,
                resposta.tipoDocumento(),
                resposta.nomeArquivo(),
                criadaEm,
                resposta.etapas(),
                resposta.dadosExtraidos(),
                resposta.resultado());
        try {
            repository.save(new ValidacaoDocumentoEntity(
                    id,
                    resposta.tipoDocumento().name(),
                    resposta.nomeArquivo(),
                    criadaEm,
                    objectMapper.writeValueAsString(persistida)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível serializar o resultado da validação.", e);
        }
        return persistida;
    }

    @Transactional(readOnly = true)
    public RespostaValidacao consultar(String id) {
        ValidacaoDocumentoEntity entidade = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Não foi encontrada uma validação com o identificador informado."));
        return desserializar(entidade);
    }

    @Transactional(readOnly = true)
    public List<RespostaValidacao> listarRecentes() {
        return repository.findTop100ByOrderByCriadaEmDesc()
                .stream()
                .map(this::desserializar)
                .toList();
    }

    private RespostaValidacao desserializar(ValidacaoDocumentoEntity entidade) {
        try {
            return objectMapper.readValue(entidade.getRespostaJson(), RespostaValidacao.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "O resultado armazenado para a validação " + entidade.getId() + " está inválido.",
                    e);
        }
    }
}
