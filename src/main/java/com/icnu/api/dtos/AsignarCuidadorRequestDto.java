package com.icnu.api.dtos;

import jakarta.validation.constraints.NotNull;

public record AsignarCuidadorRequestDto(
        @NotNull(message = "El id de usuario es obligatorio.")
        Long usuarioId) {
}//record
