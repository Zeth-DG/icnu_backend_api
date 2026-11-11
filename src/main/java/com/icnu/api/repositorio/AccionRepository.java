package com.icnu.api.repositorio;

import com.icnu.api.entidad.Accion;
import com.icnu.api.entidad.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccionRepository extends JpaRepository <Accion, Long> {
    Optional<Accion> findByNombre(String nombre);
}//interface AccionRepository
