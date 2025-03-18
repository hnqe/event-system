package br.edu.ifg.event_system.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class UserRequestDTO {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String nomeCompleto;

    @NotBlank
    private List<String> roles;

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }
    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public List<String> getRoles() {
        return roles;
    }
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

}
