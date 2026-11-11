package com.icnu.api.servicio;

import com.icnu.api.dtos.ContraseniaDto;
import com.icnu.api.entidad.Usuario;
import com.icnu.api.repositorio.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder encoder; // inyectar el encriptador

    public List<Usuario> obtenerUsuarios() {
        return usuarioRepository.findAll();
    }//obtener todos los usuarios

    public Usuario getUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El usuario con el id [" + id + "] no existe."));
    }//obtener un usuario

    public Usuario addUsuario(Usuario usuario) {
        // Verificar si el correo ya existe
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El correo electrónico ya está registrado.");
        }//if
        // Encriptar contraseña
        usuario.setContrasenia(encoder.encode(usuario.getContrasenia()));
        // Guardar en la base de datos y devolver usuario
        return usuarioRepository.save(usuario);
    }//agregar usuario

    public Usuario deleteUsuario(Long id) {
        // 1. Buscamos al usuario. Si no existe, lanzamos la excepción automáticamente.
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El usuario con el id [" + id + "] no existe, no se puede eliminar."));

        // 2. Lo borramos
        usuarioRepository.delete(usuario);

        // 3. Lo devolvemos (útil para que el frontend sepa qué se borró)
        return usuario;
    }//borrar usuario

    public Usuario updateUsuario(Long id, ContraseniaDto dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El usuario con el id [" + id + "] no existe."));

        // Verificamos que la contraseña actual coincida con la de la base de datos
        if (encoder.matches(dto.getContraseniaActual(), usuario.getContrasenia())) {
            usuario.setContrasenia(encoder.encode(dto.getContraseniaNueva()));
            return usuarioRepository.save(usuario);
        } else {
            throw new IllegalArgumentException("La contraseña actual es incorrecta.");
        }//if
    }//update usuario

}//class