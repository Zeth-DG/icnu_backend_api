package com.icnu.api.entidad;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

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

    @Column(name = "contrasenia", nullable = false, length = 60)
    private String contrasenia;

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
