package com.icnu.api.servicio;

import com.icnu.api.dtos.ContraseniaDto;
import com.icnu.api.entidad.Rol;
import com.icnu.api.entidad.Usuario;
import com.icnu.api.excepciones.DuplicateResourceException;
import com.icnu.api.excepciones.ResourceNotFoundException;
import com.icnu.api.repositorio.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolService rolService;
    private final PasswordEncoder encoder; // inyectar el encriptador

    @Transactional(readOnly = true)
    public List<Usuario> obtenerUsuarios() {
        return usuarioRepository.findAll();
    }//obtener todos los usuarios

    @Transactional(readOnly = true)
    public Usuario getUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario con el id [" + id + "] no existe."));
    }//obtener un usuario

    @Transactional(readOnly = true)
    public Usuario getUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario con el email ["+ email +"] no existe."));
    }//obtener usuario por email

    @Transactional
    public Usuario addUsuario(Usuario usuario) {
        //verificar que se ingrese el email
        if( usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio.");
        }//if email no se ingresa
        String emailNormalizado = usuario.getEmail().trim().toLowerCase();

        if(usuarioRepository.findByEmail(emailNormalizado).isPresent()) {
            throw new DuplicateResourceException("El correo electrónico ya está registrado");
        }//if duplicado

        // contraseña obligatoria
        if (usuario.getContrasenia() == null || usuario.getContrasenia().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }//if no se ingresa contraseña

        Rol rol = resolverRol(usuario.getRol());
        usuario.setRol(rol);

        usuario.setEmail(emailNormalizado);
        usuario.setContrasenia(encoder.encode(usuario.getContrasenia()));

        return usuarioRepository.save(usuario);
    }//agregar usuario

    @Transactional
    public Usuario deleteUsuario(Long id) {
        // 1. Buscar al usuario. Si no existe, lanzamos la excepción
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario con el id [" + id + "] no existe, no se puede eliminar."));

        //2. desasociar la relación con pacientes antes de borrar
        usuario.getPacientes().clear();

        // 3. Lo borramos
        usuarioRepository.delete(usuario);

        // 4. Lo devolvemos (útil para que el frontend sepa qué se borró)
        return usuario;
    }//borrar usuario

    @Transactional
    public Usuario updateContrasenia(Long id, ContraseniaDto dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario con el id ["+ id +"] no existe."));

        if (dto.getContraseniaActual() == null || dto.getContraseniaActual().isBlank()) {
            throw  new IllegalArgumentException("La contraseña actual es obligatoria.");
        }//if contraseña actual esta vacía

        if(dto.getContraseniaNueva() == null || dto.getContraseniaNueva().isBlank()) {
          throw new IllegalArgumentException("La nueva contraseña no puede estar vacía.");
        }//if nueva contraseña esta vacia

        if(dto.getContraseniaNueva().length() < 8) {
            throw  new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres.");
        }//if contraseña corta

        if(dto.getContraseniaNueva().equals(dto.getContraseniaActual())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente a la actual");
        }//if misma contraseña que la actual

        if(!encoder.matches(dto.getContraseniaActual(), usuario.getContrasenia())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta.");
        }//if contraseña actual incorrecta

        usuario.setContrasenia(encoder.encode(dto.getContraseniaNueva()));
        return usuarioRepository.save(usuario);
    }//update contraseña de usuario

    private Rol resolverRol(Rol rolEntrada) {
        if(rolEntrada == null) {
            throw new IllegalArgumentException("El rol del usuario es obligatorio");
        }

        // Si el ID NO es nulo, buscamos por ID
        if(rolEntrada.getId() != null) {
            return rolService.getRol(rolEntrada.getId());
        }

        // Si no hay ID, pero hay nombre, buscamos por nombre
        if(rolEntrada.getNombre() != null && !rolEntrada.getNombre().isBlank()) {
            return rolService.getRolPorNombre(rolEntrada.getNombre());
        }

        throw new IllegalArgumentException("El rol del usuario es obligatorio.");
    }//resolverRol
}//class