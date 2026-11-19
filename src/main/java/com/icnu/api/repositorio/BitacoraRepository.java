package com.icnu.api.repositorio;

import com.icnu.api.entidad.Bitacora;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BitacoraRepository extends JpaRepository<Bitacora, Long> {

    boolean existsByUsuario_Id(Long usuarioId);

    void deleteByPaciente_Id(Long pacienteId);
}//interface


