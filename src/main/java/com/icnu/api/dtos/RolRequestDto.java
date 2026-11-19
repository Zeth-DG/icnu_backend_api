package com.icnu.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RolRequestDto(
        @NotBlank(message = "El nombre del rol es obligatorio.")
        @Size(max=50, message = "El nombre del rol no puede superar los 50 caracteres.")
        String nombre) {
}//record
