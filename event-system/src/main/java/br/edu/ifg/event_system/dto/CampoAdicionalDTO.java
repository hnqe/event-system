package br.edu.ifg.event_system.dto;

public class CampoAdicionalDTO {
    private Long id;
    private String nome;
    private String tipo;
    private String descricao;
    private Boolean obrigatorio;
    private String opcoes;

    public CampoAdicionalDTO() {
        // This constructor is intentionally empty because it's required by
        // JSON/XML serialization frameworks (Jackson, JAXB) for object deserialization.
        // Framework uses reflection to create an instance and then populate fields.
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

}