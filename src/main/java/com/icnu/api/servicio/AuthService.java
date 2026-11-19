package com.icnu.api.servicio;

import com.icnu.api.dtos.LoginRequest;
import com.icnu.api.dtos.LoginResponse;
import com.icnu.api.entidad.Usuario;
import com.icnu.api.excepciones.CredencialesInvalidasException;
import com.icnu.api.repositorio.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository
                .findByEmail(email)
                .orElseThrow(CredencialesInvalidasException::new);
        if(!encoder.matches(request.contrasenia(),usuario.getContrasenia())){
            throw new CredencialesInvalidasException();
        }//if

        String token = jwtService.generarToken(usuario);

        return new LoginResponse(
                token,
                usuario.getId(),
                usuario.getNombre(),
                usuario.getRol().getNombre());
    }//login

}//class
