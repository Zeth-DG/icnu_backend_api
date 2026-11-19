package com.icnu.api.dtos;

import com.icnu.api.entidad.Rol;

public record RolResponseDto(Long id, String nombre) {
    public static RolResponseDto from(Rol rol) {
        return new RolResponseDto(rol.getId(), rol.getNombre());
    }//rol response dto
}//record
