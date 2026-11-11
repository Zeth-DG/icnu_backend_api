package com.icnu.api.servicio;

import com.icnu.api.config.JwtFilter;
import com.icnu.api.dtos.LoginRequest;
import com.icnu.api.entidad.Usuario;
import com.icnu.api.repositorio.UsuarioRepository;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {
    @Autowired
    PasswordEncoder encoder;

    private final UsuarioRepository usuarioRepository;

    public JwtService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }//constructor

    public boolean validarUsuario(LoginRequest request) {
        Optional<Usuario> usuario = usuarioRepository.findByEmail(request.getEmail());
        if(usuario.isPresent()) {
            Usuario usr = usuario.get();

            if (encoder.matches(request.getContrasenia(), usr.getContrasenia())) {
                return true;
            }//if interno
        }//if externo

        return false;
    }//validarUsuario

    public String generarToken(String email) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 24); //token que expira después de 24 horas
        return Jwts
                .builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(calendar.getTime())
                .signWith(JwtFilter.getSignInKey())
                .compact();
    }//generarToken
}//class
