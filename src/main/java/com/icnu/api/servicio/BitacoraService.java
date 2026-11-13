package com.icnu.api.servicio;


import com.icnu.api.dtos.BitacoraRequestDto;
import com.icnu.api.dtos.BitacoraResponseDto;
import com.icnu.api.entidad.*;
import com.icnu.api.repositorio.AccionRepository;
import com.icnu.api.repositorio.BitacoraRepository;
import com.icnu.api.repositorio.PacienteRepository;
import com.icnu.api.repositorio.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    public BitacoraResponseDto  addBitacora (BitacoraRequestDto request) {

        //Recupera usuario que registra la bitacora
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow( () -> new RuntimeException("Usuario no encontrado"));

        // Recupera paciente que registra la bitacora
        Paciente paciente = pacienteRepository.findById(request.pacienteId())
                .orElseThrow( () -> new RuntimeException("Usuario no encontrado"));

        Bitacora bitacora = new Bitacora();  //crea nueva entidad bitacora

        //Asigna los valores
        bitacora.setPaciente(paciente);
        bitacora.setUsuario(usuario);
        bitacora.setFecha( request.fecha() != null ? request.fecha() : LocalDateTime.now() );

        if ( request.accionId() != null ){ // asigna accion sólo si se recibe

            if( usuario.getRol() != Rol.admin) {
                throw new RuntimeException("Sólo el usuario admin puede registrar acciones");
            } // if es admin

            Accion accion = accionRepository.findById(request.accionId())
                    .orElseThrow( () -> new RuntimeException("Accion no encontrada"));

            bitacora.setAccion(accion);

        } // if se recibe accion

        // Bitacora actualizada
        Bitacora b = bitacoraRepository.save(bitacora);

        // Convertir entidad a DTO de respuesta:
        return new BitacoraResponseDto(
                b.getId(),
                b.getFecha(),
                b.getNotas(),
                b.getUsuario().getId(),
                b.getPaciente().getId(),
                b.getAccion() != null ? b.getAccion().getId() : null
        );
    } // addBitacora


    @Transactional
    public Bitacora uppdateBitacora (Long id, Bitacora bitacora) {

        Bitacora  bit = bitacoraRepository.findById(id)

        .orElseThrow( () -> new RuntimeException("Bitácora no encontrada"));

        if(bitacora.getFecha() != null )
            bit.setFecha(bitacora.getFecha()); //actualiza fecha si se recibió


        if(bitacora.getNotas() !=null)
            bit.setNotas(bitacora.getNotas().trim());

        if(bitacora.getAccion() != null && bitacora.getAccion().getId() != null ) {
            Accion accion = accionRepository.findById(bitacora.getAccion().getId())
                            .orElseThrow( () -> new RuntimeException("Acción no encontrada"));

            bit.setAccion(accion);
        }

            Bitacora b= bitacoraRepository.save(bit);


        return b;
    } // updateBitacora











} //class BitacoraService
