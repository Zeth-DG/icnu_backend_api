package com.icnu.api.servicio;

import com.icnu.api.dtos.RolRequestDto;
import com.icnu.api.dtos.RolResponseDto;
import com.icnu.api.entidad.Rol;
import com.icnu.api.excepciones.BusinessRuleException;
import com.icnu.api.excepciones.DuplicateResourceException;
import com.icnu.api.excepciones.ResourceNotFoundException;
import com.icnu.api.repositorio.RolRepository;
import com.icnu.api.repositorio.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RolService {
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<RolResponseDto> listar() {
        return rolRepository
                .findAll()
                .stream()
                .map(RolResponseDto::from)
                .toList();
    }//listar todos los roles desde el dto

    @Transactional(readOnly = true)
    public RolResponseDto obtener(Long id) {
        return RolResponseDto.from(getRol(id));
    }//obtener

    @Transactional(readOnly = true)
    public Rol getRol(Long id) {
        return rolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El rol con el id ["+ id +"] no existe."));
    }//find rol by id

    @Transactional(readOnly = true)
    public Rol getRolPorNombre(String nombre) {
        String normalizado = normalizarNombre(nombre);
        return rolRepository.findByNombre(normalizado)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El rol con el nombre ["+ normalizado +"] no existe."));
    }//obtener rol por nombre

    @Transactional
    public RolResponseDto crear(RolRequestDto dto) {
        String normalizado = normalizarNombre(dto.nombre());

        if (rolRepository.findByNombre(normalizado).isPresent()) {
            throw new DuplicateResourceException(
                    "Elrol ["+normalizado+"] ya esta registrado.");
        }//if
        Rol rol = new Rol();
        rol.setNombre(normalizado);
        return RolResponseDto.from(rolRepository.save(rol));
    }//addRol

    @Transactional
    public void eliminar(Long id) {
        Rol rol = getRol(id);

        if (usuarioRepository.existsByRol_Id(id)){
            throw new BusinessRuleException(
                    "No se puede eliminar un rol que tiene usuarios asignados.");
        }//if
        rolRepository.delete(rol);
    }//eliminar rol

    public static String normalizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new BusinessRuleException(
                    "El nombre del rol es obligatorio");
        }//if

        return nombre
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", "_");
    }//normalizar nombre

}//class
