package br.edu.ifg.event_system.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    @JsonIgnore
    private String password;

    private String nomeCompleto;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private List<Role> roles = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "user_campus",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "campus_id"))
    private List<Campus> campusQueAdministro = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "user_departamento",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "departamento_id"))
    private List<Departamento> departamentosQueAdministro = new ArrayList<>();

    public User() {
    }

    public User(String username, String password, String nomeCompleto) {
        this.username = username;
        this.password = password;
        this.nomeCompleto = nomeCompleto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public List<Campus> getCampusQueAdministro() {
        return campusQueAdministro;
    }

    public void setCampusQueAdministro(List<Campus> campusQueAdministro) {
        this.campusQueAdministro = campusQueAdministro;
    }

    public List<Departamento> getDepartamentosQueAdministro() {
        return departamentosQueAdministro;
    }

    public void setDepartamentosQueAdministro(List<Departamento> departamentosQueAdministro) {
        this.departamentosQueAdministro = departamentosQueAdministro;
    }

}