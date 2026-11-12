package com.icnu.api.servicio;


import com.icnu.api.entidad.Bitacora;
import com.icnu.api.entidad.Paciente;
import com.icnu.api.entidad.Usuario;
import com.icnu.api.repositorio.AccionRepository;
import com.icnu.api.repositorio.BitacoraRepository;
import com.icnu.api.repositorio.PacienteRepository;
import com.icnu.api.repositorio.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor //recibe datos para hacer constructor
public class BitacoraService {

    private final BitacoraRepository bitacoraRepository;
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final AccionRepository accionRepository;


    public Bitacora getBitacora(Long id) {
        return bitacoraRepository
                .findById(id)
                .orElseThrow( () -> new IllegalArgumentException("No existe una bitacora con id " + id));
    } //getBitacora

    public Bitacora deleteBitacora(Long id) {
        Bitacora bitacora = null;

        if( bitacora.existById(id)) {  // 1- buscar bitacora por ID

            bitacora = bitacoraRepository.findById(id).get();
            bitacoraRepository.delete(bitacora);
        } // if
        return bitacora;
    } //deleteBitacora

    @Transactional
    public Bitacora addBitacora (Bitacora bitacora) {

        // Validar los datos recibidos
        if( bitacora.getFecha() == null){
            throw new IllegalArgumentException("La fecha es obligatoria");
        }//if

        if(bitacora.getUsuario() == null || bitacora.getPaciente() == null){
            throw new IllegalArgumentException("Usuario y paciente son obligatorios");
        } //if

        // Recupera las entidades relacionadas
        Long usuarioId = bitacora.getUsuario().getId();
        Usuario usuario = usuarioRepository.findById(usuarioId).get();

        Long pacienteId = bitacora.getPaciente().getId();
        Paciente paciente  = pacienteRepository.findById(pacienteId).get();

        LocalDateTime fecha = bitacora.getFecha();

        //Asigna los valores
        bitacora.setFecha(fecha);
        bitacora.setPaciente(paciente);
        bitacora.setUsuario(usuario);

        return bitacoraRepository.save(bitacora);
    } // addBitacora



    public Bitacora uppdateBitacora () {
        return null;
    } // updateBitacora











} //class BitacoraService
