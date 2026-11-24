package com.icnu.api.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record BitacoraRequestDto(
        LocalDateTime fecha,

        @Size(max = 2000, message = "Las notas no pueden superar 2000 caracteres.")
        String notas,

        @NotNull(message = "El paciente es obligatorio.")
        Long pacienteId,

        Long accionId) {
} // BitacraraRequestDTO