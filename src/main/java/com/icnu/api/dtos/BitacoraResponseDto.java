package com.icnu.api.dtos;

import java.time.LocalDateTime;

public record BitacoraResponseDto ( //record es una forma breve de declarar una clase que solo guarda datos. Java genera automaticamente sus campos, constructor, accesores y métodos (equals, hashCode y toString)
        Long id,
        LocalDateTime fecha,
        String notas,
        Long usuarioId,
        Long pacienteId,
        Long accionId

) {
}
