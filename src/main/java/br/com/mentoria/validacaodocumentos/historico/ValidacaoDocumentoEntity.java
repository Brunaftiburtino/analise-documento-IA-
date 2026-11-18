package br.com.mentoria.validacaodocumentos.historico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "validacao_documento")
public class ValidacaoDocumentoEntity {

    @Id
    private String id;

    @Column(nullable = false, length = 40)
    private String tipoDocumento;

    @Column(nullable = false)
    private String nomeArquivo;

    @Column(nullable = false)
    private LocalDateTime criadaEm;

    @Lob
    @Column(nullable = false)
    private String respostaJson;

    protected ValidacaoDocumentoEntity() {
    }

    public ValidacaoDocumentoEntity(
            String id,
            String tipoDocumento,
            String nomeArquivo,
            LocalDateTime criadaEm,
            String respostaJson) {
        this.id = id;
        this.tipoDocumento = tipoDocumento;
        this.nomeArquivo = nomeArquivo;
        this.criadaEm = criadaEm;
        this.respostaJson = respostaJson;
    }

    public String getId() {
        return id;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public String getRespostaJson() {
        return respostaJson;
    }
}
