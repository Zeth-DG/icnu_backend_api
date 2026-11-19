package com.icnu.api.dtos;

import com.icnu.api.entidad.Usuario;

public record UsuarioResponseDto(
        Long id,
        String nombre,
        String email,
        Long rolId,
        String rol) {

    public static UsuarioResponseDto from(Usuario usuario) {
        return new UsuarioResponseDto(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol() != null ? usuario.getRol().getId() : null,
                usuario.getRol() != null ? usuario.getRol().getNombre(): null);
    }//usuario response dto

}//record
