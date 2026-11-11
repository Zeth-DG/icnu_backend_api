package com.icnu.api.servicio;

import com.icnu.api.entidad.Accion;
import com.icnu.api.repositorio.AccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccionService {
    private final AccionRepository accionRepository;

    public List<Accion> obtenerAcciones() {
        return accionRepository.findAll();
    }//obtener todas las acciones

    public Accion getAccion(Long id) {
        return accionRepository.findById(id)
                .orElseThrow( () -> new IllegalArgumentException("La acción con el id [" + id + "] no existe.") );
    }//obtener acción por id

    public Accion addAccion(Accion accion) {
        if (accionRepository.findByNombre(accion.getNombre()).isPresent()) {
            throw new IllegalArgumentException("La acción ya está registrada.");
        }//if
        return accionRepository.save(accion);
    }//agregar una accion

}//class
