package br.com.mentoria.validacaodocumentos.historico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ValidacaoDocumentoRepository extends JpaRepository<ValidacaoDocumentoEntity, String> {

    List<ValidacaoDocumentoEntity> findTop100ByOrderByCriadaEmDesc();
}
