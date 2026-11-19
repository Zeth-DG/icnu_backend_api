package com.icnu.api.config;

import com.icnu.api.entidad.Accion;
import com.icnu.api.entidad.Rol;
import com.icnu.api.entidad.Usuario;
import com.icnu.api.repositorio.AccionRepository;
import com.icnu.api.repositorio.RolRepository;
import com.icnu.api.repositorio.UsuarioRepository;
import com.icnu.api.util.Roles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
public class DataInitializer implements CommandLineRunner {
    private final RolRepository rolRepository;
    private final AccionRepository accionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder encoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(
            RolRepository rolRepository,
            AccionRepository accionRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder encoder,
            @Value("${app.bootstrap-admin.email:}") String adminEmail,
            @Value("${app.bootstrap-admin.password:}") String adminPassword){

        this.rolRepository = rolRepository;
        this.accionRepository = accionRepository;
        this.usuarioRepository = usuarioRepository;
        this.encoder = encoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }//constructor

    @Override
    @Transactional
    public void run(String... args) {
        crearRolSiNoExiste(Roles.ADMIN);
        crearRolSiNoExiste(Roles.CUIDADOR);
        crearRolSiNoExiste(Roles.RED_APOYO);

        crearAccionSiNoExiste("MEDICACION");
        crearAccionSiNoExiste("CITA_MEDICA");
        crearAccionSiNoExiste("NOTA_GENERAL");

        crearAdminInicialSiEstaConfigurado();
    }//crear roles y acciones

    private void crearRolSiNoExiste(String nombre) {
        rolRepository.findByNombre(nombre).orElseGet(() -> {
            Rol rol = new Rol();
            rol.setNombre(nombre);
            return rolRepository.save(rol);
        });
    }//crear rol

    private void crearAccionSiNoExiste(String nombre) {
        accionRepository.findByNombre(nombre).orElseGet(() -> {
            Accion accion = new Accion();
            accion.setNombre(nombre);
            return accionRepository.save(accion);
        });
    }//crear accion

    /**
     * Crea el primer ADMIN a partir de variables de entorno.
     * Si no se configuran, no hace nada. Es idempotente.
     */
    private void crearAdminInicialSiEstaConfigurado() {
        if (adminEmail == null || adminEmail.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            return;
        }

        String email = adminEmail.trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmail(email)) {
            return;
        }

        Rol admin = rolRepository.findByNombre(Roles.ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el rol ADMIN. Revisa la siembra de roles."));

        Usuario usuario = new Usuario();
        usuario.setNombre("Administrador");
        usuario.setEmail(email);
        usuario.setContrasenia(encoder.encode(adminPassword));
        usuario.setRol(admin);
        usuarioRepository.save(usuario);
    }//crear admin inicial

}//class
