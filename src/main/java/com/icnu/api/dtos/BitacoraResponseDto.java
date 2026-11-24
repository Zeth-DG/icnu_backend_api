package com.icnu.api.dtos;

import com.icnu.api.entidad.Bitacora;
import java.time.LocalDateTime;

public record BitacoraResponseDto(
        Long id,
        LocalDateTime fecha,
        String notas,
        Long usuarioId,
        Long pacienteId,
        Long accionId) {

    public static BitacoraResponseDto from(Bitacora bitacora) { // from() que convierte una entidad Bitacora en un objeto BitacoraResponseDto.
        return new BitacoraResponseDto(
                bitacora.getId(),
                bitacora.getFecha(),
                bitacora.getNotas(),
                bitacora.getUsuario().getId(),
                bitacora.getPaciente().getId(),
                bitacora.getAccion() != null ? bitacora.getAccion().getId() : null);
    }
} //BitacoraResponseDto

