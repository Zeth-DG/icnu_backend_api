package com.icnu.api.servicio;

import com.icnu.api.dtos.ContraseniaDto;
import com.icnu.api.dtos.RegistroRequestDto;
import com.icnu.api.dtos.UsuarioRequestDto;
import com.icnu.api.dtos.UsuarioResponseDto;
import com.icnu.api.dtos.UsuarioUpdateDto;
import com.icnu.api.entidad.Rol;
import com.icnu.api.entidad.Usuario;
import com.icnu.api.excepciones.AccesoDenegadoException;
import com.icnu.api.excepciones.BusinessRuleException;
import com.icnu.api.excepciones.DuplicateResourceException;
import com.icnu.api.excepciones.ResourceNotFoundException;
import com.icnu.api.repositorio.BitacoraRepository;
import com.icnu.api.repositorio.UsuarioRepository;
import com.icnu.api.util.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolService rolService;
    private final PasswordEncoder encoder; // inyectar el encriptador
    private final BitacoraRepository bitacoraRepository;

    @Transactional(readOnly = true)
    public Page<UsuarioResponseDto> listar(Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(UsuarioResponseDto::from);
    }//listar

    @Transactional(readOnly = true)
    public UsuarioResponseDto obtener(Long id, String emailAutenticado) {
        Usuario objetivo = buscar(id);
        Usuario actual = buscarPorEmail(emailAutenticado);

        if(!esAdmin(actual) && !actual.getId().equals(objetivo.getId())) {
            throw new AccesoDenegadoException("Solo puedes consultar tu propio perfil.");
        }//if
        return UsuarioResponseDto.from(objetivo);
    }//obtener un usuario

    @Transactional(readOnly = true)
    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(normalizarEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario no existe."));
    }//obtener usuario por email

    /**Registro público siempre crea un cuidador*/
    @Transactional
    public UsuarioResponseDto registrar(RegistroRequestDto dto) {
        return crear(dto.nombre(), dto.email(), dto.contrasenia(), Roles.CUIDADOR);
    }//agregar usuario

    /**Creación por un admin, se puede elegir rol*/
    @Transactional
    public UsuarioResponseDto crearComoAdmin(UsuarioRequestDto dto) {
        Rol rol = rolService.getRol(dto.rolId());
        return crear(dto.nombre(), dto.email(), dto.contrasenia(), rol.getNombre());
    }//agregar usuario como admin

    private UsuarioResponseDto crear(
            String nombre,
            String email,
            String contrasenia,
            String rolNombre) {
        String emailNormalizado = normalizarEmail(email);

        if(usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new DuplicateResourceException("El correo electrónico ya está registrado.");
        }//if

        Rol rol = rolService.getRolPorNombre(rolNombre);

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre.trim());
        usuario.setEmail(emailNormalizado);
        usuario.setContrasenia(encoder.encode(contrasenia));
        usuario.setRol(rol);

        return UsuarioResponseDto.from(usuarioRepository.save(usuario));
    }//crear

    @Transactional
    public UsuarioResponseDto actualizar(
            Long id,
            UsuarioUpdateDto dto,
            String emailAutenticado) {
        Usuario objetivo = buscar(id);
        Usuario actual = buscarPorEmail(emailAutenticado);

        if(!esAdmin(actual) && !actual.getId().equals(id)) {
            throw new AccesoDenegadoException("Solo puedes editar tu propio perfil.");
        }//if

        String emailNormalizado = normalizarEmail(dto.email());

        usuarioRepository.findByEmail(emailNormalizado)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new DuplicateResourceException("El correo electrónico ya está registrado.");
                });
        objetivo.setNombre(dto.nombre().trim());
        objetivo.setEmail(emailNormalizado);

        //un usuario normal no puede cambiarse el rol a si mismo
        if (esAdmin(actual) && dto.rolId() !=null) {
            objetivo.setRol(rolService.getRol(dto.rolId()));
        }//if
        return UsuarioResponseDto.from(objetivo);
    }//actualizar

    @Transactional
    public void cambiarContrasenia(
            Long id,
            ContraseniaDto dto,
            String emailAutenticado) {

        Usuario objetivo = buscar(id);
        Usuario actual = buscarPorEmail(emailAutenticado);

        if (!actual.getId().equals(id)) {
            throw new AccesoDenegadoException(
                    "Solo puedes cambiar tu propia contraseña.");
        }

        if (!encoder.matches(dto.contraseniaActual(), objetivo.getContrasenia())) {
            throw new BusinessRuleException("La contraseña actual es incorrecta.");
        }

        if (encoder.matches(dto.contraseniaNueva(), objetivo.getContrasenia())) {
            throw new BusinessRuleException(
                    "La nueva contraseña debe ser diferente a la actual.");
        }

        objetivo.setContrasenia(encoder.encode(dto.contraseniaNueva()));
    }

    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = buscar(id);

        // Las bitácoras tienen FK al usuario. No las borramos en silencio:
        // son historia clínica, así que se impide el borrado y se avisa.
        if (bitacoraRepository.existsByUsuario_Id(id)) {
            throw new BusinessRuleException(
                    "No se puede eliminar un usuario que tiene bitácoras registradas.");
        }//if

        // Rompe la relación N:M por el lado dueño antes de borrar.
        usuario.getPacientes().clear();
        usuarioRepository.delete(usuario);
    }//eliminar

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario con el id [" + id + "] no existe."));
    }//buscar

    private static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }//normalizar email

    private static boolean esAdmin(Usuario usuario) {
        return Roles.ADMIN.equals(usuario.getRol().getNombre());
    }//esAdmin?
}//class