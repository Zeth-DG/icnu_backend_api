package com.icnu.api.entidad;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data // Genera getters, setters, toString automáticamente
@NoArgsConstructor // Genera el constructor vacío

@Entity
@Table(name="pacientes")
public class Paciente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "alergias", columnDefinition = "TEXT")
    private String alergias;

    @Column(name = "enfermedades", columnDefinition = "TEXT")
    private String enfermedades;

    @Column(name = "discapacidad", columnDefinition = "TEXT")
    private String discapacidad;

    @Column(name = "familiar_emergencia")
    private String familiarEmergencia;

    @Column(name = "medico_emergencia")
    private String medicoEmergencia;

    // Llave foránea
    // Relación Muchos a Muchos con Usuario
    @ManyToMany(mappedBy = "pacientes")
    // excluir para evitar errores
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Usuario> cuidadores = new ArrayList<>();
}//class Paciente