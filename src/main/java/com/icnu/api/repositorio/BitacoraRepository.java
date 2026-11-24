package com.icnu.api.repositorio;

import com.icnu.api.entidad.Bitacora;
import org.springframework.data.jpa.repository.JpaRepository;  // proporciona operaciones crud
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

public interface BitacoraRepository extends JpaRepository<Bitacora, Long> {

    @EntityGraph(attributePaths = {"usuario", "paciente", "accion"})
    Page<Bitacora> findByPaciente_IdOrderByFechaDesc(Long pacienteId, Pageable pageable);
    boolean existsByUsuario_Id(Long usuarioId);

    void deleteByPaciente_Id(Long pacienteId);
}//interface




