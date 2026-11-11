package com.icnu.api.repositorio;

import com.icnu.api.entidad.Rol;
import com.icnu.api.entidad.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByNombre(String nombre);
}//interface Rol
