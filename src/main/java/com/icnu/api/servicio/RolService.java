package com.icnu.api.servicio;

import com.icnu.api.entidad.Rol;
import com.icnu.api.excepciones.DuplicateResourceException;
import com.icnu.api.excepciones.ResourceNotFoundException;
import com.icnu.api.repositorio.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RolService {
    private final RolRepository rolRepository;

    @Transactional(readOnly = true)
    public List<Rol> obtenerRoles() {
        return rolRepository.findAll();
    }//Obtener todos los roles

    @Transactional(readOnly = true)
    public Rol getRol(Long id) {
        return rolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El rol con el id ["+ id +"] no existe."));
    }//find rol by id

    @Transactional(readOnly = true)
    public Rol getRolPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del rol es obligatorio.");
        }//if
        String nombreNormalizado = nombre.trim();
        return rolRepository.findByNombre(nombreNormalizado)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El rol con el nombre ["+ nombreNormalizado +"] no existe."));
    }//obtener rol por nombre

    @Transactional
    public Rol addRol(Rol rol) {
        if (rol.getNombre() == null || rol.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del rol es obligatorio.");
        }//if rol vacio
        String nombreNormalizado = rol.getNombre().trim().toUpperCase().replace(" ", "_");

        if(rolRepository.findByNombre(nombreNormalizado).isPresent()) {
            throw new DuplicateResourceException(
                    "El rol ["+ nombreNormalizado +"] ya esta registrado");
        }//if esta presente

        rol.setNombre(nombreNormalizado);
        return  rolRepository.save(rol);
    }//addRol

}//class
