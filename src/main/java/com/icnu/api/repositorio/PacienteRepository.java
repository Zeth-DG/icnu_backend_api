package com.icnu.api.repositorio;

import com.icnu.api.entidad.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long > {

    Optional <Paciente> findById (Long Id);
}//interface Paciente
