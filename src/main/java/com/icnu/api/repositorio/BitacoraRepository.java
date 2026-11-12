package com.icnu.api.repositorio;

import com.icnu.api.entidad.Bitacora;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BitacoraRepository extends JpaRepository<Bitacora, Long> {

    Optional<Bitacora> findById(Long id);
}


