package com.icnu.api.entidad;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Optional;

@Data // Genera getters, setters, toString automáticamente
@NoArgsConstructor // Genera el constructor vacío
@Entity
@Table(name="roles")
public class Rol {
    public static Rol admin;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

}//class Rol

