package com.icnu.api.repositorio;

import com.icnu.api.entidad.Bitacora;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BitacoraRepository extends JpaRepository<Bitacora, Long> {
}
