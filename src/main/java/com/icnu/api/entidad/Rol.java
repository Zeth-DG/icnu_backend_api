package com.icnu.api.entidad;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data // Genera getters, setters, toString automáticamente
@NoArgsConstructor // Genera el constructor vacío
@Entity
@Table(name="roles")
public class Rol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

}//class Rol
