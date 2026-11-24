package com.icnu.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccionRequestDto(
        @NotBlank(message = "El nombre de la acción es obligatorio.")
        @Size(max = 50)
        String nombre) {
}

