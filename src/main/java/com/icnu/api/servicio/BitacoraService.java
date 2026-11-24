package com.icnu.api.servicio;


import com.icnu.api.dtos.BitacoraRequestDto;
import com.icnu.api.dtos.BitacoraResponseDto;
import com.icnu.api.entidad.Accion;
import com.icnu.api.entidad.Bitacora;
import com.icnu.api.entidad.Paciente;
import com.icnu.api.entidad.Usuario;
import com.icnu.api.excepciones.AccesoDenegadoException;
import com.icnu.api.excepciones.ResourceNotFoundException;
import com.icnu.api.repositorio.AccionRepository;
import com.icnu.api.repositorio.BitacoraRepository;
import com.icnu.api.repositorio.PacienteRepository;
import com.icnu.api.repositorio.UsuarioRepository;
import com.icnu.api.util.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor //recibe datos para hacer constructor
public class BitacoraService {

    private final BitacoraRepository bitacoraRepository;
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final AccionRepository accionRepository;

    @Transactional (readOnly = true)
    public BitacoraResponseDto obtener(Long id, String emailAutenticado ){
        Bitacora bitacora = buscar(id);

        verificarAccesoAlPaciente(bitacora.getPaciente(), emailAutenticado);

        if( !esAutorOAdmin (bitacora.getUsuario(), emailAutenticado)){
            throw new AccesoDenegadoException("No puedes consultar esta bitacora");
        } //if
        return BitacoraResponseDto.from(bitacora);

    }//obtener

    @Transactional (readOnly = true)
    public Page<BitacoraResponseDto> listaPorPaciente (
            Long pacienteId,
            Pageable pageable,
            String emailAutenticado
    ) {
        Paciente paciente = buscarPaciente(pacienteId);
        verificarAccesoAlPaciente (paciente, emailAutenticado);
            return bitacoraRepository.findByPaciente_IdOrderByFechaDesc(
                    pacienteId, pageable).map(BitacoraResponseDto::from);
    } //Page

    @Transactional
    public BitacoraResponseDto crear(BitacoraRequestDto dto, String emailAutenticado) {

        Usuario autor = usuarioActual(emailAutenticado);
        Paciente paciente = buscarPaciente(dto.pacienteId());
        verificarAccesoAlPaciente(paciente, emailAutenticado);

        Bitacora bitacora = new Bitacora();
        bitacora.setUsuario(autor);
        bitacora.setPaciente(paciente);
        bitacora.setFecha(dto.fecha() != null ? dto.fecha() : LocalDateTime.now());
        bitacora.setNotas(dto.notas());

        if (dto.accionId() != null){
            Accion accion = accionRepository.findById(dto.accionId())
                    .orElseThrow( () -> new ResourceNotFoundException( "La acción con el id [" + dto.accionId ()  +"] " +
                            "no existe."));
                    bitacora.setAccion(accion); }//ifAccion

        return BitacoraResponseDto.from(bitacoraRepository.save(bitacora));

    }//crearBitacora

    @Transactional
    public BitacoraResponseDto actualizar (
            Long id,
            BitacoraRequestDto dto,
            String emailAutenticado
    ) {
        Bitacora bitacora = buscar(id);
        verificarAccesoAlPaciente(bitacora.getPaciente(), emailAutenticado);

        if ( !esAutorOAdmin(bitacora.getUsuario(), emailAutenticado)) {
            throw new AccesoDenegadoException("Sólo el autor o un administrador puede editar esta bitácora");
        }//ifesAutorOAdmin

            if(dto.fecha() != null){
                bitacora.setFecha(dto.fecha());
            }
            if(dto.notas() != null){
                bitacora.setNotas(dto.notas.trim());
            }
            if(dto.accionId() != null){
                Accion accion = accionRepository.findById(dto.accionId())
                        .orElseThrow( () -> new ResourceNotFoundException("La accion con el id [" + dto.accionId() + "]" +
                                " no existe."));
                bitacora.setAccion(accion);
            }

            return BitacoraResponseDto.from(bitacora);

    }//actualizar

    @Transactional
    public void eliminar(Long id, String emailAutenticado) {

        Bitacora bitacora = buscar(id);

        if(!esAutorOAdmin(bitacora.getUsuario(), emailAutenticado)){
            throw new AccesoDenegadoException("Sólo el autor o administrador puede eliminar esta bitácora");
        }
        bitacoraRepository.delete(bitacora);
    } //eliminar


    private Bitacora buscar(Long id) {
        return bitacoraRepository.findById(id).orElseThrow( () -> new
                ResourceNotFoundException( "La bitácora con el id [" + id + "] no existe"));
    }//buscar

    private Paciente buscarPaciente(Long id) {
        return pacienteRepository.findById(id).orElseThrow( () -> new
                ResourceNotFoundException( "El paciente con el id [" + id + "] no existe"));
    }//buscarPaciente

    private void verificarAccesoAlPaciente(Paciente paciente, String emailAutenticado) {
        Usuario actual = usuarioActual(emailAutenticado);

        if(esAdmin(actual)){
            return;
        } //if

        boolean esCuidador = pacienteRepository.existsByIdAndCuidadores_Id(paciente.getId(), actual.getId());

            if( !esCuidador){
                throw new AccesoDenegadoException("No tienes acceso al historial de este paciente.");
            }//if
    }//verificarAccesoAlPaciente

    private Usuario usuarioActual(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow( () -> new ResourceNotFoundException("El usuario autenticado ya no existe"));
    }//usuarioActual

    private boolean esAutorOAdmin(Usuario usuario, String emailAutenticado) {
        Usuario actual = usuarioActual(emailAutenticado);
        Usuario autor = usuarioActual(emailAutenticado); //REVISAR CON LIZ
        return esAdmin(actual) || autor.getEmail().equals(actual.getEmail());
    } //esAutorOAdmin

    private static boolean esAdmin(Usuario usuario){
        return Roles.ADMIN.equals(usuario.getRol().getNombre());
    }//esAdmin

} //class BitacoraService
