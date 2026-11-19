package com.icnu.api.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioUpdateDto(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100)
        String nombre,

        @NotBlank(message = "El email es obligatorio.")
        @Email(message = "El email no tiene un formato válido.")
        String email,

        // Solo un ADMIN puede cambiarlo; el service lo ignora para un usuario normal.
        Long rolId) {
}//record
