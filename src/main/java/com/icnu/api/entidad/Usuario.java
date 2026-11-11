package com.icnu.api.entidad;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data // Genera getters, setters, toString automáticamente
@NoArgsConstructor // Genera el constructor vacío

@Entity
@Table(name="usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    //para evitar que la contraseña salga en los JSON
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "contrasenia", nullable = false, length = 60)
    private String contrasenia;

    @ManyToOne
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;
    //clave foránea con pacientes
    @ManyToMany
    @JoinTable(
            name = "usuario_has_paciente",
            joinColumns = @JoinColumn(name = "usuario_id_usuario"),
            inverseJoinColumns = @JoinColumn(name = "paciente_id_paciente")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Paciente> pacientes = new ArrayList<>();

}//class
