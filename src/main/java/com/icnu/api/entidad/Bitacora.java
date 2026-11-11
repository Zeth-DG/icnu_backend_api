package com.icnu.api.entidad;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data // Genera getters, setters, toString automáticamente
@NoArgsConstructor // Genera el constructor vacío

@Entity
@Table(name="bitacora")
public class Bitacora {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "notas", nullable = false, columnDefinition = "TEXT")
    private String notas;

    //claves foráneas
    @ManyToOne
    @JoinColumn(name = "usuario_id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "paciente_id_paciente",nullable = false)
    private Paciente paciente;

    @ManyToOne
    @JoinColumn(name = "accion_id_accion", nullable = false)
    private Accion accion;
}//class Bitacora
