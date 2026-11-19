package com.icnu.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PacienteRequestDto(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max=100)
        String nombre,

        @NotNull(message = "La fecha de nacimiento es obligatoria.")
        @Past(message = "La fecha de nacimiento debe estar en el pasado.")
        LocalDate fechaNacimiento,

        String alergias,
        String enfermedades,
        String discapacidad,

        @Size(max = 20)
        String familiarEmergencia,

        @Size(max = 20)
        String medicoEmergencia
) {
}//record
