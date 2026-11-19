package com.icnu.api.dtos;

import com.icnu.api.entidad.Paciente;

import java.time.LocalDate;

public record PacienteResponseDto(
        Long id,
        String nombre,
        LocalDate fechaNacimiento,
        String alergias,
        String enfermedades,
        String discapacidad,
        String familiarEmergencia,
        String medicoEmergencia) {

    public static PacienteResponseDto from(Paciente paciente) {
        return new PacienteResponseDto(
                paciente.getId(),
                paciente.getNombre(),
                paciente.getFechaNacimiento(),
                paciente.getAlergias(),
                paciente.getEnfermedades(),
                paciente.getDiscapacidad(),
                paciente.getFamiliarEmergencia(),
                paciente.getMedicoEmergencia());
    }//pciente response
}//record
