package com.icnu.api.repositorio;

import com.icnu.api.entidad.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Override
    @EntityGraph(attributePaths = "rol")
    Page<Usuario> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "rol")
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRol_Id(Long rolId);

    List<Usuario> findByPacientes_Id(Long pacienteId);
}//interface Usuario
