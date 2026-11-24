package com.icnu.api.dtos;
import com.icnu.api.entidad.Accion;

public record AccionResponseDto(Long id, String nombre) {

    public static AccionResponseDto from(Accion accion) {
        return new AccionResponseDto(accion.getId(), accion.getNombre());
    }
}
