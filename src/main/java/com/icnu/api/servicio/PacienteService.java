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

    public Paciente agregarPaciente(Paciente paciente, Long usuarioId) {
        // Buscar al usuario en la BD
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow( () -> new IllegalArgumentException("El usuario con el id [" + usuarioId + "] no existe") );

        //guardar el paciente para que se genere su ID en la BD
        Paciente nuevoPaciente = pacienteRepository.save(paciente);
        //asignar al usuario
        usuario.getPacientes().add(nuevoPaciente);
        //actualizar la tabla intermedia
        usuarioRepository.save(usuario);

        return nuevoPaciente;
    }//agregarPaciente

    public List<Paciente> obtenerPacientesDeUsuario(Long usuarioId) {
        // Buscar al usuario en la BD
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow( () -> new IllegalArgumentException("El usuario con el id [" + usuarioId + "] no existe") );
        // Devolver la lista de pacientes que ya está dentro de ese objeto Usuario
        return usuario.getPacientes();
    }//obtener pacientes por usuario

    public Paciente obtenerPacientePorId(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El paciente con el id ["+id+"] no existe"));
    }//obtener paciente por id

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

    public Paciente actualizarPaciente(Long id, Paciente pacienteActualizado) {
        //buscar paciente
        Paciente pacienteOriginal = pacienteRepository.findById(id)
                .orElseThrow(()-> new IllegalArgumentException("El paciente con el id ["+id+"] no existe"));

        //actualizar los campos
        pacienteOriginal.setNombre(pacienteActualizado.getNombre());
        pacienteOriginal.setFechaNacimiento(pacienteActualizado.getFechaNacimiento());
        pacienteOriginal.setAlergias(pacienteActualizado.getAlergias());
        pacienteOriginal.setEnfermedades(pacienteActualizado.getEnfermedades());
        pacienteOriginal.setDiscapacidad(pacienteActualizado.getDiscapacidad());
        pacienteOriginal.setFamiliarEmergencia(pacienteActualizado.getFamiliarEmergencia());
        pacienteOriginal.setMedicoEmergencia(pacienteActualizado.getMedicoEmergencia());

        //guardamos los cambios
        return pacienteRepository.save(pacienteOriginal);
    }//actualizar información del paciente

    public void eliminarPaciente(Long id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El paciente con el id ["+id+"] no existe"));
        pacienteRepository.delete(paciente);
    }//eliminarPaciente


}//class
