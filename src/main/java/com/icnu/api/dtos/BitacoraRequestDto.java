package com.icnu.api.dtos;

import java.time.LocalDateTime;

public record BitacoraRequestDto(
        LocalDateTime fecha,
        String notas,
        Long usuarioId,
        Long pacienteId,
        Long accionId

) {
} // BitacoraRequestDto
