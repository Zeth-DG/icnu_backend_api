package com.icnu.api.servicio;

import com.icnu.api.dtos.PacienteRequestDto;
import com.icnu.api.dtos.PacienteResponseDto;
import com.icnu.api.entidad.Paciente;
import com.icnu.api.entidad.Usuario;
import com.icnu.api.excepciones.AccesoDenegadoException;
import com.icnu.api.excepciones.ResourceNotFoundException;
import com.icnu.api.repositorio.BitacoraRepository;
import com.icnu.api.repositorio.PacienteRepository;
import com.icnu.api.repositorio.UsuarioRepository;
import com.icnu.api.util.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final BitacoraRepository bitacoraRepository;

    @Transactional(readOnly = true)
    public Page<PacienteResponseDto> listar(Pageable pageable, String emailAutenticado) {
        Usuario actual = usuarioActual(emailAutenticado);

        Page<Paciente> pagina = esAdmin(actual)
                ? pacienteRepository.findAll(pageable)
                : pacienteRepository.findByCuidadores_Id(actual.getId(), pageable);
        return pagina.map(PacienteResponseDto::from);
    }//listar

    @Transactional(readOnly = true)
    public PacienteResponseDto obtener(Long id, String emailAutenticado) {
        Paciente paciente = buscar(id);
        verificarAcceso(paciente, emailAutenticado);
        return PacienteResponseDto.from(paciente);
    }//obtener

    @Transactional
    public PacienteResponseDto crear(PacienteRequestDto dto, String emailAutenticado) {
        Usuario actual = usuarioActual(emailAutenticado);

        Paciente paciente = new Paciente();
        aplicar(dto, paciente);
        paciente = pacienteRepository.save(paciente);

        // Lado dueño de la relación N:M: Usuario.pacientes
        actual.getPacientes().add(paciente);

        return PacienteResponseDto.from(paciente);
    }//agregarPaciente


    @Transactional
    public PacienteResponseDto actualizar(
            Long id,
            PacienteRequestDto dto,
            String emailAutenticado) {

        Paciente paciente = buscar(id);
        verificarAcceso(paciente, emailAutenticado);

        aplicar(dto, paciente);
        return PacienteResponseDto.from(paciente);
    }//actualizar información del paciente

    @Transactional
    public PacienteResponseDto asignarCuidador(Long pacienteId, Long usuarioId) {
        Paciente paciente = buscar(pacienteId);

        Usuario cuidador = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario con el id [" + usuarioId + "] no existe."));

        boolean yaAsignado = cuidador.getPacientes().stream()
                .anyMatch(p -> p.getId().equals(pacienteId));

        if (!yaAsignado) {
            cuidador.getPacientes().add(paciente);
        }
        return PacienteResponseDto.from(paciente);
    }//asignar

    @Transactional
    public void desasignarCuidador(Long pacienteId, Long usuarioId) {
        Usuario cuidador = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario con el id [" + usuarioId + "] no existe."));

        cuidador.getPacientes().removeIf(p -> p.getId().equals(pacienteId));
    }//desasignar

    @Transactional
    public void eliminar(Long id) {
        Paciente paciente = buscar(id);

        // 1. Las bitácoras tienen FK al paciente: hay que borrarlas primero.
        bitacoraRepository.deleteByPaciente_Id(id);

        // 2. El paciente es el lado inverso del N:M: hay que desvincularlo.
        List<Usuario> cuidadores = usuarioRepository.findByPacientes_Id(id);
        cuidadores.forEach(cuidador ->
                cuidador.getPacientes().removeIf(p -> p.getId().equals(id)));

        // 3. Ahora sí.
        pacienteRepository.delete(paciente);
    }//eliminarPaciente

    private void aplicar(PacienteRequestDto dto, Paciente paciente) {
        paciente.setNombre(dto.nombre().trim());
        paciente.setFechaNacimiento(dto.fechaNacimiento());
        paciente.setAlergias(dto.alergias());
        paciente.setEnfermedades(dto.enfermedades());
        paciente.setDiscapacidad(dto.discapacidad());
        paciente.setFamiliarEmergencia(dto.familiarEmergencia());
        paciente.setMedicoEmergencia(dto.medicoEmergencia());
    }//aplicar cambios

    private Paciente buscar(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El paciente con el id [" + id + "] no existe."));
    }//buscar

    private void verificarAcceso(Paciente paciente, String emailAutenticado) {
        Usuario actual = usuarioActual(emailAutenticado);

        if (esAdmin(actual)) {
            return;
        }//if

        boolean esCuidador = pacienteRepository
                .existsByIdAndCuidadores_Id(paciente.getId(), actual.getId());

        if (!esCuidador) {
            throw new AccesoDenegadoException(
                    "No tienes acceso a este paciente.");
        }//if
    }//verificar acceso

    private Usuario usuarioActual(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario autenticado ya no existe."));
    }//usuarioActual

    private static boolean esAdmin(Usuario usuario) {
        return Roles.ADMIN.equals(usuario.getRol().getNombre());
    }//es admin?

}//class
