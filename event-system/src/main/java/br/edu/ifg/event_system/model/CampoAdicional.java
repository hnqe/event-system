package br.edu.ifg.event_system.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "campo_adicional")
public class CampoAdicional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "evento_id", nullable = false)
    @JsonIgnore
    private Evento evento;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String tipo;

    private String descricao;

    private Boolean obrigatorio;

    private String opcoes;

    @Version
    private Integer version;

    /**
     * Default no-args constructor.
     *
     * This constructor is intentionally empty because:
     * 1. JPA specification requires all entity classes to have a public or protected
     *    no-args constructor (JPA 2.0 specification, section 2.1)
     * 2. The JPA provider (Hibernate) uses this constructor when instantiating entities
     *    from database records
     * 3. After construction, field values are set directly by the persistence framework
     *    using reflection, bypassing setters
     * 4. This empty constructor is essential for ORM functionality, allowing entities
     *    to be properly hydrated from the database
     */
    public CampoAdicional() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Evento getEvento() {
        return evento;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Boolean getObrigatorio() {
        return obrigatorio;
    }

    public void setObrigatorio(Boolean obrigatorio) {
        this.obrigatorio = obrigatorio;
    }

    public String getOpcoes() {
        return opcoes;
    }

    public void setOpcoes(String opcoes) {
        this.opcoes = opcoes;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

}