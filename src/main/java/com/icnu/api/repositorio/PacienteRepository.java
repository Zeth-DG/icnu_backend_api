package com.icnu.api.repositorio;

import com.icnu.api.entidad.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Page<Paciente> findByCuidadores_Id(Long usuarioId, Pageable pageable);

    boolean existsByIdAndCuidadores_Id(Long pacienteId, Long usuarioId);
}//interface Paciente
