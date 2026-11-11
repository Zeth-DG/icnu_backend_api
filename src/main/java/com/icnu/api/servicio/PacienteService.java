package com.icnu.api.servicio;

import com.icnu.api.entidad.Paciente;
import com.icnu.api.entidad.Usuario;
import com.icnu.api.repositorio.PacienteRepository;
import com.icnu.api.repositorio.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PacienteService {
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;

    public Paciente agregarPaciente(Paciente paciente) {
        return pacienteRepository.save(paciente);
    }//agregarPaciente

    public List<Paciente> obtenerPacientesDeUsuario(Long usuarioId) {
        // Buscar al usuario en la BD
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow( () -> new IllegalArgumentException("El usuario con el id [" + usuarioId + "] no existe") );
        // Devolver la lista de pacientes que ya está dentro de ese objeto Usuario
        return usuario.getPacientes();
    }//obtener pacientes por usuario

    public Usuario asignarPaciente(Long pacienteId, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow( () -> new IllegalArgumentException("Usuario no encontrado") );

        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow( () -> new IllegalArgumentException("Paciente no encontrado") );

        // Agregar el paciente a la lista del usuario
        usuario.getPacientes().add(paciente);

        // Guardar al usuario para que Hibernate actualice la tabla intermedia
        return usuarioRepository.save(usuario);
    }//asignar paciente a un usuario

}//class
