package com.icnu.api.servicio;

import com.icnu.api.entidad.Rol;
import com.icnu.api.repositorio.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RolService {
    private final RolRepository rolRepository;

    public List<Rol> obtenerRoles() {
        return rolRepository.findAll();
    }//Obtener todos los roles

    public Rol getRol(Long id) {
        return rolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El rol con el id [" + id + "] no existe."));
    }//find rol by id

    public Rol addRol(Rol rol) {
        Optional<Rol> rolExistente = rolRepository.findByNombre(rol.getNombre());
        if(rolExistente.isPresent()) {
            throw new IllegalArgumentException("El rol ya está registrado");
        }//if
        return  rolRepository.save(rol);
    }//addRol


}//class
