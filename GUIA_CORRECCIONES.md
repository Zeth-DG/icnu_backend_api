# Guía de corrección paso a paso — ICNU API

> **Estado:** guía de estudio. No se modificó ninguna clase Java.
> **Perfil:** estás aprendiendo Java/Spring. Cada fase explica *qué* se cambia, *por qué* y *cómo comprobarlo*.
> **Rama recomendada:** `develop` está limpia. Crea una rama antes de empezar:

```powershell
git checkout -b fix/seguridad-y-controllers
```

---

## Índice

- [Cómo usar esta guía](#cómo-usar-esta-guía)
- [Mapa de errores → causa → fase](#mapa-de-errores--causa--fase)
- [Pruebas HTTP con Postman (archivo aparte)](GUIA_POSTMAN.md)
- [Fase 0 — Preparación y línea base](#fase-0--preparación-y-línea-base)
- [Fase 1 — Dependencias y configuración](#fase-1--dependencias-y-configuración)
- [Fase 2 — Seguridad y JWT](#fase-2--seguridad-y-jwt)
- [Fase 3 — DTOs, validación y errores](#fase-3--dtos-validación-y-errores)
- [Fase 4 — AuthController y UsuarioController](#fase-4--authcontroller-y-usuariocontroller)
- [Fase 5 — RolController y datos semilla](#fase-5--rolcontroller-y-datos-semilla)
- [Fase 6 — PacienteController](#fase-6--pacientecontroller)
- [Fase 7 — Bitácora y Acciones](#fase-7--bitácora-y-acciones)
- [Fase 8 — Pruebas](#fase-8--pruebas)
- [Fase 9 — Limpieza del modelo](#fase-9--limpieza-del-modelo)
- [Fase 10 — Documentación del contrato](#fase-10--documentación-del-contrato)
- [Apéndice A — Checklist final](#apéndice-a--checklist-final)
- [Apéndice B — Errores comunes al aplicar la guía](#apéndice-b--errores-comunes-al-aplicar-la-guía)
- [Apéndice C — Conceptos clave](#apéndice-c--conceptos-clave)
- [Apéndice D — Bitácora de la mentoría](#apéndice-d--bitácora-de-la-mentoría)

---

## Cómo usar esta guía

1. Haz **una fase por sesión**. No mezcles fases: cada una deja el proyecto compilando.
2. Antes de cada fase: `.\gradlew.bat compileJava`. Al final: `.\gradlew.bat build`.
3. Aplica los cambios en el orden indicado. Hay dependencias reales: el filtro nuevo necesita el `JwtService` nuevo, y los controllers necesitan los DTOs.
4. Si algo falla, consulta el [Apéndice B](#apéndice-b--errores-comunes-al-aplicar-la-guía) antes de improvisar.
5. Los bloques **"Por qué"** son la parte formativa. Léelos aunque el código ya te funcione.
6. Los bloques **"Verifica"** son tu red de seguridad. No avances sin pasarlos. Las comprobaciones HTTP se hacen con la colección de [`GUIA_POSTMAN.md`](GUIA_POSTMAN.md).

### Archivos que vas a crear

```text
src/main/java/com/icnu/api/
├── config/
│   ├── SecurityConfig.java              (reescritura)
│   ├── JwtAuthenticationFilter.java     (nuevo, sustituye JwtFilter)
│   └── DataInitializer.java             (nuevo)
├── controlador/
│   ├── AuthController.java              (nuevo)
│   ├── UsuarioController.java           (nuevo)
│   ├── RolController.java               (nuevo)
│   ├── PacienteController.java          (reescritura)
│   ├── BitacoraController.java          (nuevo)
│   └── AccionController.java            (nuevo)
├── dtos/
│   ├── LoginRequest.java                (record, reemplaza la clase)
│   ├── LoginResponse.java               (nuevo)
│   ├── RegistroRequestDto.java          (nuevo)
│   ├── UsuarioRequestDto.java           (nuevo)
│   ├── UsuarioUpdateDto.java            (nuevo)
│   ├── UsuarioResponseDto.java          (nuevo)
│   ├── ContraseniaDto.java              (record, reemplaza la clase)
│   ├── RolRequestDto.java               (nuevo)
│   ├── RolResponseDto.java              (nuevo)
│   ├── PacienteRequestDto.java          (nuevo)
│   ├── PacienteResponseDto.java         (nuevo)
│   ├── AsignarCuidadorRequestDto.java   (nuevo)
│   ├── BitacoraRequestDto.java          (ajuste: quitar usuarioId)
│   ├── BitacoraResponseDto.java         (ajuste: add from())
│   ├── AccionRequestDto.java            (nuevo)
│   ├── AccionResponseDto.java           (nuevo)
│   └── PageResponse.java                (nuevo)
├── excepciones/
│   ├── GlobalExceptionHandler.java      (nuevo)
│   ├── BusinessRuleException.java       (nuevo)
│   ├── CredencialesInvalidasException.java (nuevo)
│   └── AccesoDenegadoException.java     (nuevo)
├── servicio/
│   ├── AuthService.java                 (nuevo)
│   ├── JwtService.java                  (reescritura)
│   ├── UsuarioService.java              (reescritura)
│   ├── RolService.java                  (reescritura)
│   ├── PacienteService.java             (reescritura)
│   ├── BitacoraService.java             (reescritura)
│   └── AccionService.java               (ajuste)
└── util/
    └── Roles.java                       (nuevo)
```

### Archivos que vas a eliminar

```text
src/main/java/com/icnu/api/config/JwtFilter.java   (lo reemplaza JwtAuthenticationFilter)
src/main/java/com/icnu/api/dtos/TokenJwt.java      (lo reemplaza LoginResponse)
```

> **Truco de aprendizaje:** cuando borres `JwtFilter`, hazlo al final de la Fase 2, no al principio. Así compilas contra el código nuevo y ves exactamente qué referencias quedaron colgando.

---

## Mapa de errores → causa → fase

| Síntoma | Causa real | Fase |
|---|---|---|
| No puedo hacer login | No existe `AuthController` | 2 y 4 |
| `403` en todo | El filtro no llena el `SecurityContext` | 2 |
| `@PreAuthorize` no hace nada | Falta `@EnableMethodSecurity` | 2 |
| `500` con "Token inválido" | El filtro lanza `ServletException`, no `401` | 2 |
| `500` al consultar un paciente con cuidador | Recursión JSON `Paciente → Usuario → Paciente` | 3 y 6 |
| `PUT /api/pacientes/{id}` borra el paciente | El método llama a `eliminarPaciente` | 6 |
| "El email ya existe" devuelve `500` | No hay `@RestControllerAdvice` | 3 |
| `POST` crea/actualiza registros ajenos | Se recibe la entidad y se hace `save()` con `id` | 3, 4 y 6 |
| Login funciona en minúsculas y falla en mayúsculas | Registro normaliza email, login no | 4 |
| "Sólo el admin puede registrar acciones" siempre falla | `Rol.admin` es `null` y se compara con `!=` | 5 y 7 |
| `deleteBitacora` lanza `NullPointerException` | Se usa una variable `null` antes de asignarla | 7 |
| Se pierden las `notas` al crear bitácora | Nunca se llama a `setNotas` | 7 |
| Borrar paciente/usuario falla por FK | No se limpian relaciones ni bitácoras | 6 y 9 |

---

## Fase 0 — Preparación y línea base

### 0.1 Comprueba que hoy compila

```powershell
.\gradlew.bat compileJava
```

Debe terminar en `BUILD SUCCESSFUL`. Si no compila **antes** de empezar, anota el error y no sigas: no sabrás si el fallo es tuyo o preexistente.

### 0.2 Comprueba qué errores son de negocio y no de compilación

No hace falta levantar MySQL todavía. La compilación es tu primer filtro.

### 0.3 Entiende el punto de partida

Hoy el flujo es este:

```text
Cliente → JwtFilter → (valida token a mano) → Controller → Service
```

Problemas estructurales:

- `JwtFilter` **no es un bean** (`new JwtFilter()` en `SecurityConfig:20`), así que no puede inyectar nada.
- `JwtFilter` **valida** el token pero no **autentica**: nunca hace `SecurityContextHolder.getContext().setAuthentication(...)`.
- `SecurityConfig` dice `anyRequest().permitAll()`. La seguridad real vive en `if`s dentro del filtro.
- No existe forma de obtener un token.

El flujo objetivo es:

```text
Cliente → JwtAuthenticationFilter → SecurityContext → AuthorizationManager → @PreAuthorize → Controller → Service
                ↑
        JwtService valida firma y extrae rol
```

**Por qué importa:** en Spring Security, "quién eres" (autenticación) y "qué puedes hacer" (autorización) son dos pasos distintos. Si el filtro no deja tu identidad en el contexto, el segundo paso no puede funcionar. `@PreAuthorize` no adivina: lee las `GrantedAuthority` del `Authentication` actual.

---

## Fase 1 — Dependencias y configuración

### Paso 1.1 — Añade Bean Validation

**Archivo:** `build.gradle`

Dentro de `dependencies { ... }`, añade:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-validation'
```

**Por qué:** sin este starter, `@Valid`, `@NotBlank`, `@Email` y `@Size` se ignoran silenciosamente (o fallan al arrancar). Es la causa de que hoy todas las validaciones estén hechas a mano con `if`s dentro de los services.

**Verifica:**

```powershell
.\gradlew.bat compileJava
.\gradlew.bat dependencies --configuration runtimeClasspath | Select-String "hibernate-validator"
```

Debe aparecer `hibernate-validator`.

### Paso 1.2 — Saca los secretos del código

**Archivo:** `src/main/resources/application.properties` (está en `.gitignore`, correcto)

Reemplaza el contenido por:

```properties
spring.application.name=api

spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/icnu_db}
spring.datasource.username=${DB_USER:root}
spring.datasource.password=${DB_PASSWORD}

# Durante el aprendizaje: update. Antes de producción: validate + Flyway.
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false

# Mínimo 32 caracteres. Nunca lo subas a Git.
app.jwt.secret=${JWT_SECRET}
app.jwt.expiration-hours=24

# Solo se usa la primera vez para crear el ADMIN inicial.
# Déjalas vacías después de crear el usuario.
app.bootstrap-admin.email=${BOOTSTRAP_ADMIN_EMAIL:}
app.bootstrap-admin.password=${BOOTSTRAP_ADMIN_PASSWORD:}
```

Actualiza también `src/main/resources/application.properties.template` con las mismas claves, pero con valores de ejemplo:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/icnu_db
spring.datasource.username=root
spring.datasource.password=CAMBIAR
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
app.jwt.secret=CAMBIAR_POR_UN_SECRETO_DE_AL_MENOS_32_CARACTERES
app.jwt.expiration-hours=24
app.bootstrap-admin.email=
app.bootstrap-admin.password=
```

**Por qué:**
- `${VAR}` hace que Spring lea variables de entorno. Si `DB_PASSWORD` no existe, la app no arranca: es mejor fallar rápido que arrancar con una contraseña vacía.
- `open-in-view=false` obliga a que las consultas perezosas ocurran dentro del `@Transactional`. Es incómodo al principio, pero evita el clásico `LazyInitializationException` en producción y el antipatrón de mantener la sesión de BD abierta durante toda la petición HTTP.
- El secreto JWT **no** puede estar en una constante `public static` del código: hoy cualquiera con el repositorio puede firmar tokens válidos.

Genera un secreto (PowerShell):

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { [byte](Get-Random -Maximum 256) }))
```

Guárdalo en la sesión actual:

```powershell
$env:JWT_SECRET = "pega-aqui-el-valor-generado"
$env:DB_PASSWORD = "tu-password-local"
```

En IntelliJ: **Run → Edit Configurations → Environment variables**.

**Verifica:** arranca la app con `.\gradlew.bat bootRun`. Si ves `Could not resolve placeholder 'JWT_SECRET'`, no configuraste la variable.

### Paso 1.3 — Borra el secreto hardcodeado (aún no borres el archivo)

En `JwtFilter.java`, las líneas 22-26 contienen el secreto. Las eliminaremos en la Fase 2 al reemplazar la clase. **No** las copies a ningún sitio.

> ⚠️ Ese secreto ya está en el historial de Git. Rótalo (usa uno nuevo) y, si el repo es público, considera limpiar el historial más adelante.

---

## Fase 2 — Seguridad y JWT

Esta es la fase más importante. Si la haces mal, todo lo demás dará `403` o `500`.

### Paso 2.1 — Crea la clase de constantes de roles

**Archivo nuevo:** `src/main/java/com/icnu/api/util/Roles.java`

```java
package com.icnu.api.util;

public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String CUIDADOR = "CUIDADOR";
    public static final String RED_APOYO = "RED_APOYO";

    private Roles() {
        // clase de constantes: no se instancia
    }
}
```

**Por qué:** hoy los nombres de rol están escritos a mano en tres sitios (`@PreAuthorize`, `RolService`, `BitacoraService`) con formatos distintos (`ADMIN`, `Admin`, `CUIDADOR`). Un typo silencioso se convierte en un `403` imposible de depurar. Una constante se equivoca una vez, no tres.

### Paso 2.2 — Reescribe `JwtService`

**Archivo:** `src/main/java/com/icnu/api/servicio/JwtService.java`

Sustituye todo el contenido por:

```java
package com.icnu.api.servicio;

import com.icnu.api.entidad.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-hours:24}") long expirationHours) {

        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                "app.jwt.secret debe tener al menos 32 caracteres (HS256).");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofHours(expirationHours);
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("uid", usuario.getId())
                .claim("rol", usuario.getRol().getNombre())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /**
     * Lanza io.jsonwebtoken.JwtException si el token es inválido, está
     * manipulado o ha expirado. El filtro es quien decide el HTTP 401.
     */
    public Claims parsear(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

**Qué se elimina y por qué:**

- `validarUsuario(LoginRequest)`: mezclaba autenticación con emisión de tokens. Ahora el login vive en `AuthService`.
- `Calendar`: API legacy. `Instant`/`Duration` es más claro y no mutable.
- `JwtFilter.getSignInKey()`: el servicio ya no depende del filtro (evitaba una dependencia circular conceptual).

**Por qué el claim `rol`:** `@PreAuthorize("hasAuthority('ADMIN')")` compara exactamente con las autoridades del `Authentication`. Si el token solo lleva el email, no hay forma de saber el rol sin consultar la BD en cada petición. Incluirlo en el token hace la autorización O(1).

**Trade-off que debes conocer:** si cambias el rol de un usuario, su token viejo seguirá diciendo el rol anterior hasta que expire. Para un MVP de 24 h es aceptable. La alternativa (consultar la BD en cada request) es más correcta pero más costosa; se puede añadir después con caché.

### Paso 2.3 — Crea el filtro como bean

**Archivo nuevo:** `src/main/java/com/icnu/api/config/JwtAuthenticationFilter.java`

> **Spring Boot 4 usa Jackson 3.** El `ObjectMapper` ya no vive en `com.fasterxml.jackson.databind`, sino en **`tools.jackson.databind`**. Si tu `build.gradle` declara Boot 3.x, usa `com.fasterxml.jackson.databind.ObjectMapper`. Compruébalo antes de copiar:
>
> ```powershell
> Select-String -Path .\build.gradle -Pattern "org.springframework.boot' version"
> ```
>
> Las **anotaciones** (`@JsonIgnore`, `@JsonProperty`) siguen en `com.fasterxml.jackson.annotation`, así que no hay que cambiarlas.

```java
package com.icnu.api.config;

import tools.jackson.databind.ObjectMapper;
import com.icnu.api.servicio.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // Sin header: dejamos pasar. SecurityConfig decidirá si la ruta es pública.
        if (header == null || !header.startsWith(PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.parsear(header.substring(PREFIX.length()));
            String rol = claims.get("rol", String.class);

            List<SimpleGrantedAuthority> authorities = (rol == null)
                    ? List.of()
                    : List.of(new SimpleGrantedAuthority(rol));

            var authentication = new UsernamePasswordAuthenticationToken(
                    claims.getSubject(),   // principal = email
                    null,                  // credenciales: no aplican con JWT
                    authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);

        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            escribirNoAutorizado(response);
        }
    }

    private void escribirNoAutorizado(HttpServletResponse response) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Token inválido o expirado. Inicia sesión de nuevo.");

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
```

**Diferencias clave con el `JwtFilter` viejo:**

| Antes | Ahora | Por qué |
|---|---|---|
| `new JwtFilter()` | `@Component` + inyección | Spring gestiona dependencias y ciclo de vida |
| Whitelist con `URI.contains(...)` | Lo decide `SecurityConfig` | Una sola fuente de verdad |
| No llenaba `SecurityContext` | `setAuthentication(...)` | Sin esto, `@PreAuthorize` no puede funcionar |
| `throw new ServletException` | `401` con `ProblemDetail` | Un cliente distingue "no autorizado" de "error del servidor" |
| `System.out.println(claims...)` | Nada | No filtrar emails a los logs |
| `GenericFilterBean` | `OncePerRequestFilter` | Evita ejecuciones duplicadas en forwards/async |

**Por qué dejar pasar cuando no hay header:** si el filtro lanza error por ausencia de token, las rutas públicas (`/api/auth/login`) se rompen. La regla correcta es: el filtro **solo autentica si hay credenciales**; quien decide si la ruta requiere autenticación es la cadena de seguridad.

### Paso 2.4 — Reescribe `SecurityConfig`

**Archivo:** `src/main/java/com/icnu/api/config/SecurityConfig.java`

```java
package com.icnu.api.config;

import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/usuarios/registro").permitAll()
                    .requestMatchers("/error").permitAll()
                    .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authEx) ->
                            escribirError(response, HttpStatus.UNAUTHORIZED,
                                    "Debes iniciar sesión para acceder a este recurso."))
                    .accessDeniedHandler((request, response, deniedEx) ->
                            escribirError(response, HttpStatus.FORBIDDEN,
                                    "No tienes permisos para realizar esta acción.")))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void escribirError(
            jakarta.servlet.http.HttpServletResponse response,
            HttpStatus status,
            String detalle) throws java.io.IOException {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detalle);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }

    /**
     * Un Filter anotado con @Component lo registra Spring Boot también como
     * filtro de servlet global, además de dentro de la cadena de seguridad.
     * Resultado: se ejecuta dos veces. Este bean lo desactiva como filtro
     * global, sin impedir que la cadena de seguridad lo use.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter) {

        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Ajusta estos orígenes al puerto real de tu frontend.
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://localhost:4200"));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

**Por qué cada decisión:**

- **`csrf.disable()`**: CSRF protege formularios con cookies de sesión. Una API stateless con `Authorization: Bearer` no las usa. Desactivarlo es correcto **solo** por eso; no es un atajo universal.
- **`STATELESS`**: evita que Spring cree `HttpSession`. El estado vive en el token.
- **`@EnableMethodSecurity`**: sin esta anotación, `@PreAuthorize` es texto decorativo. Es el bug silencioso más común en proyectos que empiezan.
- **`authenticationEntryPoint` / `accessDeniedHandler`**: diferencian `401` (no sé quién eres) de `403` (sé quién eres, pero no puedes). Sin esto, Spring devuelve HTML o un body vacío.
- **`CORS`**: sin esto, el navegador bloquea las peticiones del frontend aunque el backend responda bien. El error aparece solo en la consola del navegador y suele confundirse con un fallo de backend.
- **`anyRequest().authenticated()`**: cierra todo por defecto. Es el principio de "denegar salvo que se permita explícitamente".

**Cuidado con `allowedOrigins("*")` + `allowCredentials(true)`**: Spring lo rechaza en tiempo de ejecución (y es una mala práctica). Si necesitas cualquier origen, usa `setAllowedOriginPatterns`.

### Paso 2.5 — Elimina el filtro viejo

1. Borra `src/main/java/com/icnu/api/config/JwtFilter.java`.
2. Busca referencias:

```powershell
Select-String -Path .\src\**\*.java -Pattern "JwtFilter" -Recurse
```

No debe quedar ninguna. `JwtService` ya no lo importa.

### Verifica la Fase 2

```powershell
.\gradlew.bat compileJava
```

**Debe compilar.** `JwtService` ya no depende de `LoginRequest`, y nadie llamaba a `validarUsuario`. Si aparece un error de `JwtFilter`, busca referencias que hayas dejado sin borrar:

```powershell
Select-String -Path .\src\**\*.java -Pattern "JwtFilter" -Recurse
```

Comprobación conceptual antes de seguir:

- [ ] `SecurityConfig` ya no tiene `permitAll()` general.
- [ ] `@EnableMethodSecurity` está presente.
- [ ] El filtro es un `@Component` y setea `SecurityContextHolder`.
- [ ] No queda ningún secreto en el código.

---

## Fase 3 — DTOs, validación y errores

### Paso 3.1 — Entiende por qué DTOs y no entidades

Hoy un controller devuelve `Paciente`, y `Paciente` contiene `List<Usuario>`, que contiene `List<Paciente>`, etc. Jackson intenta serializar el grafo completo y entra en recursión infinita. Además, aceptar una entidad en el `@RequestBody` permite que el cliente envíe `{"id": 7, ...}`; `JpaRepository.save()` con `id` no nulo hace `merge`, y terminas actualizando un registro que no debería tocarse (mass assignment).

Regla de oro del curso:

> **La entidad nunca cruza la frontera HTTP. Entra un DTO, sale un DTO.**

### Paso 3.2 — Crea `PageResponse`

**Archivo nuevo:** `src/main/java/com/icnu/api/dtos/PageResponse.java`

```java
package com.icnu.api.dtos;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return from(page.map(mapper));
    }
}
```

**Por qué:** serializar `Page<T>` directamente funciona, pero su JSON no es estable entre versiones de Spring Data y expone detalles internos (`pageable`, `sort`, `first`, `last`...). Un DTO te da un contrato predecible para el frontend.

### Paso 3.3 — Crea las excepciones de negocio

**Archivo nuevo:** `src/main/java/com/icnu/api/excepciones/BusinessRuleException.java`

```java
package com.icnu.api.excepciones;

public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
```

**Archivo nuevo:** `src/main/java/com/icnu/api/excepciones/CredencialesInvalidasException.java`

```java
package com.icnu.api.excepciones;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos.");
    }
}
```

**Archivo nuevo:** `src/main/java/com/icnu/api/excepciones/AccesoDenegadoException.java`

```java
package com.icnu.api.excepciones;

public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String message) {
        super(message);
    }
}
```

**Por qué una excepción por caso:** cada tipo se mapea a un código HTTP distinto. `IllegalArgumentException` no dice si el problema es "no existe", "ya existe" o "no tienes permiso", y todo termina en `500`.

### Paso 3.4 — Crea el manejador global

**Archivo nuevo:** `src/main/java/com/icnu/api/excepciones/GlobalExceptionHandler.java`

```java
package com.icnu.api.excepciones;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail noEncontrado(ResourceNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail duplicado(DuplicateResourceException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ProblemDetail credenciales(CredencialesInvalidasException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail reglaDeNegocio(BusinessRuleException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ProblemDetail accesoDenegado(AccesoDenegadoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail accesoDenegadoSpring(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "No tienes permisos para realizar esta acción.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacion(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Hay campos inválidos en la solicitud.");

        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.put(error.getField(), error.getDefaultMessage()));

        problem.setProperty("errores", errores);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail jsonInvalido(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no es un JSON válido o tiene un tipo incorrecto.");
    }

    /**
     * Sin este handler, el catch genérico convertiría una URL inexistente
     * en 500. Con él, sigue siendo 404.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail rutaNoEncontrada(NoResourceFoundException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "La ruta solicitada no existe.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail inesperado(Exception ex) {
        // Se registra completo, pero NO se devuelve al cliente: evita filtrar detalles internos.
        log.error("Error no controlado", ex);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Intenta de nuevo más tarde.");
    }
}
```

**Por qué `ProblemDetail`:** es el estándar RFC 7807. El frontend recibe siempre la misma forma (`type`, `title`, `status`, `detail`) y puedes añadir campos (`errores`) sin inventar un formato propio.

**Por qué el `catch` genérico al final:** sin él, cualquier excepción no prevista sale con el stacktrace por defecto de Spring Boot (que en dev está bien, pero en prod filtra rutas y versiones). Registra con `log.error` y responde genérico.

**Verifica:** aún no hay endpoints que lo usen. Se prueba en la Fase 4.

### Paso 3.5 — Crea los DTOs de autenticación

**Archivo:** `src/main/java/com/icnu/api/dtos/LoginRequest.java` (reemplaza la clase actual)

```java
package com.icnu.api.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El email es obligatorio.")
        @Email(message = "El email no tiene un formato válido.")
        String email,

        @NotBlank(message = "La contraseña es obligatoria.")
        String contrasenia) {
}
```

**Archivo nuevo:** `src/main/java/com/icnu/api/dtos/LoginResponse.java`

```java
package com.icnu.api.dtos;

public record LoginResponse(
        String token,
        Long id,
        String nombre,
        String rol) {
}
```

**Qué cambia:** `LoginRequest` pasa de clase con getters a `record`. Los accesores ahora son `email()` y `contrasenia()`, no `getEmail()`/`getContrasenia()`. Java genera constructor, `equals`, `hashCode` y `toString`. **Nunca** dejes que un DTO de login genere un `toString` con la contraseña: el `record` lo incluye. Si te preocupa, sobreescríbelo:

```java
@Override
public String toString() {
    return "LoginRequest[email=" + email + "]";
}
```

**Por qué `record`:** es inmutable por defecto, no tiene setters y expresa "esto solo transporta datos". Ideal para request/response.

### Paso 3.6 — Crea los DTOs de usuario

**Archivo nuevo:** `src/main/java/com/icnu/api/dtos/RegistroRequestDto.java`

```java
package com.icnu.api.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequestDto(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres.")
        String nombre,

        @NotBlank(message = "El email es obligatorio.")
        @Email(message = "El email no tiene un formato válido.")
        @Size(max = 255, message = "El email no puede superar 255 caracteres.")
        String email,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 8, max = 72,
              message = "La contraseña debe tener entre 8 y 72 caracteres.")
        String contrasenia) {
}
```

> **Nota técnica:** BCrypt solo usa los primeros **72 bytes**. Por eso el máximo. Ojo: 72 *caracteres* con acentos pueden superar 72 bytes en UTF-8; es una limitación aceptada en la mayoría de proyectos.

**Archivo nuevo:** `src/main/java/com/icnu/api/dtos/UsuarioRequestDto.java` (creación por admin, con rol)

```java
package com.icnu.api.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequestDto(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100)
        String nombre,

        @NotBlank(message = "El email es obligatorio.")
        @Email(message = "El email no tiene un formato válido.")
        String email,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 8, max = 72)
        String contrasenia,

        @NotNull(message = "El rol es obligatorio.")
        Long rolId) {
}
```

**Archivo nuevo:** `src/main/java/com/icnu/api/dtos/UsuarioUpdateDto.java`

```java
package com.icnu.api.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioUpdateDto(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100)
        String nombre,

        @NotBlank(message = "El email es obligatorio.")
        @Email(message = "El email no tiene un formato válido.")
        String email,

        // Solo un ADMIN puede cambiarlo; el service lo ignora para un usuario normal.
        Long rolId) {
}
```

**Archivo nuevo:** `src/main/java/com/icnu/api/dtos/UsuarioResponseDto.java`

```java
package com.icnu.api.dtos;

import com.icnu.api.entidad.Usuario;

public record UsuarioResponseDto(
        Long id,
        String nombre,
        String email,
        Long rolId,
        String rol) {

    public static UsuarioResponseDto from(Usuario usuario) {
        return new UsuarioResponseDto(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol() != null ? usuario.getRol().getId() : null,
                usuario.getRol() != null ? usuario.getRol().getNombre() : null);
    }
}
```

**Por qué un método `from`:** evita repetir el mapeo en cada controller y mantiene el DTO cerca de su entidad. Cuando el modelo cambie, hay un solo sitio que actualizar. (En proyectos grandes se usa MapStruct, pero para aprender, `from` es explícito y suficiente.)

### Paso 3.7 — Reemplaza `ContraseniaDto` por un record

**Archivo:** `src/main/java/com/icnu/api/dtos/ContraseniaDto.java`

```java
package com.icnu.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContraseniaDto(
        @NotBlank(message = "La contraseña actual es obligatoria.")
        String contraseniaActual,

        @NotBlank(message = "La nueva contraseña es obligatoria.")
        @Size(min = 8, max = 72,
              message = "La nueva contraseña debe tener entre 8 y 72 caracteres.")
        String contraseniaNueva) {
}
```

**Consecuencia:** `UsuarioService` usa hoy `dto.getContraseniaActual()`. Al reescribirlo en la Fase 4, cambia a `dto.contraseniaActual()`.

### Paso 3.8 — DTOs de rol

**Archivo nuevo:** `src/main/java/com/icnu/api/dtos/RolRequestDto.java`

```java
package com.icnu.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RolRequestDto(
        @NotBlank(message = "El nombre del rol es obligatorio.")
        @Size(max = 50, message = "El nombre del rol no puede superar 50 caracteres.")
        String nombre) {
}
```

**Archivo nuevo:** `src/main/java/com/icnu/api/dtos/RolResponseDto.java`

```java
package com.icnu.api.dtos;

import com.icnu.api.entidad.Rol;

public record RolResponseDto(Long id, String nombre) {

    public static RolResponseDto from(Rol rol) {
        return new RolResponseDto(rol.getId(), rol.getNombre());
    }
}
```

### Paso 3.9 — DTOs de paciente

**Archivo nuevo:** `src/main/java/com/icnu/api/dtos/PacienteRequestDto.java`

```java
package com.icnu.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PacienteRequestDto(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100)
        String nombre,

        @NotNull(message = "La fecha de nacimiento es obligatoria.")
        @Past(message = "La fecha de nacimiento debe estar en el pasado.")
        LocalDate fechaNacimiento,

        String alergias,
        String enfermedades,
        String discapacidad,

        @Size(max = 50)
        String familiarEmergencia,

        @Size(max = 50)
        String medicoEmergencia) {
}
```

**Archivo nuevo:** `src/main/java/com/icnu/api/dtos/PacienteResponseDto.java`

```java
package com.icnu.api.dtos;

import com.icnu.api.entidad.Paciente;

import java.time.LocalDate;

public record PacienteResponseDto(
        Long id,
        String nombre,
        LocalDate fechaNacimiento,
        String alergias,
        String enfermedades,
        String discapacidad,
        String familiarEmergencia,
        String medicoEmergencia) {

    public static PacienteResponseDto from(Paciente paciente) {
        return new PacienteResponseDto(
                paciente.getId(),
                paciente.getNombre(),
                paciente.getFechaNacimiento(),
                paciente.getAlergias(),
                paciente.getEnfermedades(),
                paciente.getDiscapacidad(),
                paciente.getFamiliarEmergencia(),
                paciente.getMedicoEmergencia());
    }
}
```

**Por qué no incluye cuidadores:** listar pacientes con sus cuidadores provoca ciclos y N+1. Si el frontend necesita los cuidadores, crea un endpoint específico `GET /api/pacientes/{id}/cuidadores` con su propio DTO.

**Archivo nuevo:** `src/main/java/com/icnu/api/dtos/AsignarCuidadorRequestDto.java`

```java
package com.icnu.api.dtos;

import jakarta.validation.constraints.NotNull;

public record AsignarCuidadorRequestDto(
        @NotNull(message = "El id del usuario es obligatorio.")
        Long usuarioId) {
}
```

### Verifica la Fase 3

```powershell
.\gradlew.bat compileJava
```

Fallará en `UsuarioService` porque `ContraseniaDto` pasó de clase a `record` y los accesores cambiaron (`getContraseniaActual()` → `contraseniaActual()`). Es esperado; lo resolvemos en la Fase 4.

Checklist:

- [ ] Todos los DTOs son `record` (excepto `PageResponse`, que también lo es).
- [ ] Ninguno acepta `id` en el body de creación.
- [ ] Ningún DTO de respuesta incluye entidades JPA anidadas.

---

## Fase 4 — AuthController y UsuarioController

### Paso 4.1 — Crea `AuthService`

**Archivo nuevo:** `src/main/java/com/icnu/api/servicio/AuthService.java`

```java
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

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(CredencialesInvalidasException::new);

        // Mismo mensaje para email inexistente y contraseña errónea:
        // evita que un atacante descubra qué correos están registrados.
        if (!encoder.matches(request.contrasenia(), usuario.getContrasenia())) {
            throw new CredencialesInvalidasException();
        }

        String token = jwtService.generarToken(usuario);

        return new LoginResponse(
                token,
                usuario.getId(),
                usuario.getNombre(),
                usuario.getRol().getNombre());
    }
}
```

**Por qué normalizar aquí:** el registro guarda el email en minúsculas. Si el login busca la cadena tal cual, `Juan@Mail.com` no encuentra `juan@mail.com`. Este era un bug real del código anterior (`JwtService.java:28`).

**Por qué el mismo error:** si devuelves "el usuario no existe" vs "contraseña incorrecta", regalas un oráculo para enumerar cuentas.

### Paso 4.2 — Crea `AuthController`

**Archivo nuevo:** `src/main/java/com/icnu/api/controlador/AuthController.java`

```java
package com.icnu.api.controlador;

import com.icnu.api.dtos.LoginRequest;
import com.icnu.api.dtos.LoginResponse;
import com.icnu.api.servicio.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
```

**Por qué `@Valid`:** sin él, `@NotBlank` y `@Email` del record no se evalúan. Es el error más frecuente al empezar con Bean Validation.

### Paso 4.3 — Reescribe `UsuarioService`

**Archivo:** `src/main/java/com/icnu/api/servicio/UsuarioService.java`

```java
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
    private final PasswordEncoder encoder;
    private final BitacoraRepository bitacoraRepository;

    @Transactional(readOnly = true)
    public Page<UsuarioResponseDto> listar(Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(UsuarioResponseDto::from);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDto obtener(Long id, String emailAutenticado) {
        Usuario objetivo = buscar(id);
        Usuario actual = buscarPorEmail(emailAutenticado);

        if (!esAdmin(actual) && !actual.getId().equals(objetivo.getId())) {
            throw new AccesoDenegadoException("Solo puedes consultar tu propio perfil.");
        }
        return UsuarioResponseDto.from(objetivo);
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(normalizarEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario autenticado ya no existe."));
    }

    /** Registro público: SIEMPRE crea un CUIDADOR. */
    @Transactional
    public UsuarioResponseDto registrar(RegistroRequestDto dto) {
        return crear(dto.nombre(), dto.email(), dto.contrasenia(), Roles.CUIDADOR);
    }

    /** Creación por un ADMIN: permite elegir rol. */
    @Transactional
    public UsuarioResponseDto crearComoAdmin(UsuarioRequestDto dto) {
        Rol rol = rolService.getRol(dto.rolId());
        return crear(dto.nombre(), dto.email(), dto.contrasenia(), rol.getNombre());
    }

    private UsuarioResponseDto crear(
            String nombre,
            String email,
            String contrasenia,
            String rolNombre) {

        String emailNormalizado = normalizarEmail(email);

        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new DuplicateResourceException("El correo electrónico ya está registrado.");
        }

        Rol rol = rolService.getRolPorNombre(rolNombre);

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre.trim());
        usuario.setEmail(emailNormalizado);
        usuario.setContrasenia(encoder.encode(contrasenia));
        usuario.setRol(rol);

        return UsuarioResponseDto.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponseDto actualizar(
            Long id,
            UsuarioUpdateDto dto,
            String emailAutenticado) {

        Usuario objetivo = buscar(id);
        Usuario actual = buscarPorEmail(emailAutenticado);

        if (!esAdmin(actual) && !actual.getId().equals(id)) {
            throw new AccesoDenegadoException("Solo puedes editar tu propio perfil.");
        }

        String emailNormalizado = normalizarEmail(dto.email());

        usuarioRepository.findByEmail(emailNormalizado)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new DuplicateResourceException(
                            "El correo electrónico ya está registrado.");
                });

        objetivo.setNombre(dto.nombre().trim());
        objetivo.setEmail(emailNormalizado);

        // Un usuario normal no puede cambiarse el rol a sí mismo.
        if (esAdmin(actual) && dto.rolId() != null) {
            objetivo.setRol(rolService.getRol(dto.rolId()));
        }

        return UsuarioResponseDto.from(objetivo);
    }

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
        }

        // Rompe la relación N:M por el lado dueño antes de borrar.
        usuario.getPacientes().clear();
        usuarioRepository.delete(usuario);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario con el id [" + id + "] no existe."));
    }

    private static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean esAdmin(Usuario usuario) {
        return Roles.ADMIN.equals(usuario.getRol().getNombre());
    }
}
```

> Este archivo usa `existsByEmail` (Paso 4.5) y `existsByUsuario_Id` (Paso 4.6). Compilará cuando termines ambos pasos. Es normal que falle entre medias.

**Decisiones que debes entender:**

1. **Registro público sin `rolId`.** Si aceptas el rol del body, cualquiera se registra como `ADMIN`. El registro público fuerza `CUIDADOR`; crear un admin es una operación de admin.
2. **`@Transactional` en cada método que escribe.** Sin él, Hibernate no garantiza que los cambios se agrupen. Con él, si algo falla, no quedan datos a medias.
3. **No se llama a `save()` después de modificar una entidad ya cargada.** Dentro de una transacción, la entidad está "gestionada" (*managed*): Hibernate detecta los cambios y hace `UPDATE` al final (**dirty checking**). Llamar a `save()` no está mal, pero es ruido.
4. **`cambiarContrasenia` compara con `encoder.matches`.** Nunca compares contraseñas con `equals`: la BD guarda el hash, no el texto.
5. **`eliminar` limpia `pacientes`.** Si el usuario es dueño de la relación N:M, borrarlo sin limpiar deja filas en `usuario_has_paciente`. Además, las bitácoras del usuario tienen FK: en la Fase 7 decides si borrarlas o impedir el borrado.

> **Decisión de producto:** un usuario con bitácoras **no se borra**; se devuelve `400` con un mensaje claro. La alternativa (borrar en cascada) destruiría historia clínica. Si necesitas desactivar cuentas, añade un campo `activo` en lugar de borrar.

#### Por qué el `PUT` no puede recibir un `UsuarioResponseDto`

`UsuarioResponseDto` describe lo que **sale**; `UsuarioUpdateDto`, lo que **entra**. Son dos contratos distintos y no se reutilizan:

| Campo | `UsuarioResponseDto` (salida) | `UsuarioUpdateDto` (entrada) |
|---|---|---|
| `id` | Lo asigna la base de datos | No se acepta: el id va en la URL |
| `nombre` | Se lee | Se escribe |
| `email` | Se lee | Se escribe |
| `rolId` | Se lee | Solo un ADMIN puede cambiarlo |
| `rol` | Nombre del rol, derivado | No se acepta: se deriva del `rolId` |

Si usas el de salida como entrada, el cliente queda obligado a enviar `rol` (un nombre que tu service no usa para nada) y, además, **puede enviar `id` en el body**. Ese `id` no sirve: el recurso ya está en la URL (`/api/usuarios/{id}`). Y es peor de lo que parece: si algún día ese body llega a un `save()`, Hibernate hace `MERGE` sobre el id del body y terminas editando otro registro (*mass assignment* / IDOR).

Además `UsuarioResponseDto` tiene un `from(Usuario)`: está diseñado para construirse **desde** una entidad, nunca para recibirse. Un DTO de salida con factory usado como entrada es la señal de que el contrato está mal.

Regla práctica: **un DTO por caso de uso**. `XxxRequestDto` / `XxxUpdateDto` para entrar, `XxxResponseDto` para salir. Que compartan campos es casualidad, no diseño.

### Paso 4.4 — Crea `UsuarioController`

**Archivo nuevo:** `src/main/java/com/icnu/api/controlador/UsuarioController.java`

```java
package com.icnu.api.controlador;

import com.icnu.api.dtos.ContraseniaDto;
import com.icnu.api.dtos.PageResponse;
import com.icnu.api.dtos.RegistroRequestDto;
import com.icnu.api.dtos.UsuarioRequestDto;
import com.icnu.api.dtos.UsuarioResponseDto;
import com.icnu.api.dtos.UsuarioUpdateDto;
import com.icnu.api.servicio.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    /** Público: alta de cuidador. Nunca permite elegir rol. */
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponseDto> registrar(
            @Valid @RequestBody RegistroRequestDto dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.registrar(dto));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping
    public ResponseEntity<PageResponse<UsuarioResponseDto>> listar(
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(
                PageResponse.from(usuarioService.listar(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDto> obtener(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                usuarioService.obtener(id, authentication.getName()));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public ResponseEntity<UsuarioResponseDto> crear(
            @Valid @RequestBody UsuarioRequestDto dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.crearComoAdmin(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateDto dto,
            Authentication authentication) {

        return ResponseEntity.ok(
                usuarioService.actualizar(id, dto, authentication.getName()));
    }

    @PutMapping("/{id}/contrasenia")
    public ResponseEntity<Void> cambiarContrasenia(
            @PathVariable Long id,
            @Valid @RequestBody ContraseniaDto dto,
            Authentication authentication) {

        usuarioService.cambiarContrasenia(id, dto, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Por qué `Authentication authentication`:** el email autenticado viene del token, no del cliente. Si lo tomaras de un path param o del body, cualquiera podría decir "soy el usuario 5". Este es el patrón para evitar IDOR (Insecure Direct Object Reference).

**Por qué `@PreAuthorize` solo en algunos métodos:** en los que dependen de "quién eres" (listar todos, borrar, crear con rol) se resuelve en el controller. En los que dependen de "¿este recurso es tuyo?" se resuelve en el service, porque necesita consultar la BD.

### Paso 4.5 — Ajusta `UsuarioRepository`

**Archivo:** `src/main/java/com/icnu/api/repositorio/UsuarioRepository.java`

```java
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
}
```

**Qué se elimina:** el import `javax.swing.text.html.Option` (sí, traía Swing al backend) y el `Optional` sin usar.

**Por qué `@EntityGraph`:** al mapear `UsuarioResponseDto` se accede a `usuario.getRol().getNombre()`. Sin `@EntityGraph`, Hibernate hace una consulta extra por cada usuario (problema N+1). Con él, trae el rol en la misma consulta. Se aplica tanto a `findByEmail` (login) como a `findAll` (listado paginado).

**Por qué `findByPacientes_Id`:** lo necesita `PacienteService` para desvincular el paciente de todos sus cuidadores antes de borrarlo.

### Paso 4.6 — Adelanta un método en `BitacoraRepository`

`UsuarioService.eliminar` necesita saber si el usuario tiene bitácoras. Añade ese método ahora para que la Fase 4 compile.

**Archivo:** `src/main/java/com/icnu/api/repositorio/BitacoraRepository.java`

```java
package com.icnu.api.repositorio;

import com.icnu.api.entidad.Bitacora;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BitacoraRepository extends JpaRepository<Bitacora, Long> {

    boolean existsByUsuario_Id(Long usuarioId);
}
```

> En la Fase 6 añadiremos `deleteByPaciente_Id` y en la Fase 7 la consulta paginada. El archivo crece por pasos para que cada fase compile.

### Verifica la Fase 4

```powershell
.\gradlew.bat compileJava
```

`BUILD SUCCESSFUL` y la app arranca con `api bootRun`. Los tres checks HTTP de esta fase (login con credenciales inexistentes `401`, ruta protegida sin token `401` y validación `400`) están en [`GUIA_POSTMAN.md` → Verifica la Fase 4](GUIA_POSTMAN.md#verifica-la-fase-4).

> El registro y el login con un usuario real necesitan el rol `CUIDADOR` sembrado, así que se comprueban al final de la Fase 5.

---

## Fase 5 — RolController y datos semilla

### Paso 5.1 — Reescribe `RolService`

**Archivo:** `src/main/java/com/icnu/api/servicio/RolService.java`

```java
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

@Service
@RequiredArgsConstructor
public class RolService {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<RolResponseDto> listar() {
        return rolRepository.findAll().stream()
                .map(RolResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RolResponseDto obtener(Long id) {
        return RolResponseDto.from(getRol(id));
    }

    @Transactional(readOnly = true)
    public Rol getRol(Long id) {
        return rolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El rol con el id [" + id + "] no existe."));
    }

    @Transactional(readOnly = true)
    public Rol getRolPorNombre(String nombre) {
        String normalizado = normalizarNombre(nombre);
        return rolRepository.findByNombre(normalizado)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El rol [" + normalizado + "] no existe."));
    }

    @Transactional
    public RolResponseDto crear(RolRequestDto dto) {
        String normalizado = normalizarNombre(dto.nombre());

        if (rolRepository.findByNombre(normalizado).isPresent()) {
            throw new DuplicateResourceException(
                    "El rol [" + normalizado + "] ya está registrado.");
        }

        Rol rol = new Rol();
        rol.setNombre(normalizado);
        return RolResponseDto.from(rolRepository.save(rol));
    }

    @Transactional
    public void eliminar(Long id) {
        Rol rol = getRol(id);

        if (usuarioRepository.existsByRol_Id(id)) {
            throw new BusinessRuleException(
                    "No se puede eliminar un rol que tiene usuarios asignados.");
        }
        rolRepository.delete(rol);
    }

    /**
     * Única fuente de verdad para el formato de un nombre de rol.
     * "cuidador principal" -> "CUIDADOR_PRINCIPAL"
     */
    public static String normalizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new BusinessRuleException("El nombre del rol es obligatorio.");
        }
        return nombre.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", "_");
    }
}
```

**Corrección clave:** antes, `addRol` normalizaba pero `getRolPorNombre` solo hacía `trim()`. Buscar `"admin"` no encontraba `"ADMIN"`. Ahora ambas usan `normalizarNombre`.

**Por qué `getRol` es público:** `UsuarioService` y `PacienteService` necesitan la entidad, no el DTO.

### Paso 5.2 — Crea `RolController`

**Archivo nuevo:** `src/main/java/com/icnu/api/controlador/RolController.java`

```java
package com.icnu.api.controlador;

import com.icnu.api.dtos.RolRequestDto;
import com.icnu.api.dtos.RolResponseDto;
import com.icnu.api.servicio.RolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolService rolService;

    @GetMapping
    public ResponseEntity<List<RolResponseDto>> listar() {
        return ResponseEntity.ok(rolService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RolResponseDto> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(rolService.obtener(id));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public ResponseEntity<RolResponseDto> crear(@Valid @RequestBody RolRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rolService.crear(dto));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        rolService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Por qué los roles son catálogo:** `ADMIN`, `CUIDADOR`, `RED_APOYO` no son datos que el usuario inventa; son parte de las reglas del sistema. Permitir crearlos libremente llena la tabla de duplicados y hace imposible razonar sobre los permisos. Deja `POST` solo para admin y, en la práctica, siémbralos.

### Paso 5.3 — Siembra los roles al arrancar

**Archivo nuevo:** `src/main/java/com/icnu/api/config/DataInitializer.java`

```java
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
            @Value("${app.bootstrap-admin.password:}") String adminPassword) {

        this.rolRepository = rolRepository;
        this.accionRepository = accionRepository;
        this.usuarioRepository = usuarioRepository;
        this.encoder = encoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

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
    }

    private void crearRolSiNoExiste(String nombre) {
        rolRepository.findByNombre(nombre).orElseGet(() -> {
            Rol rol = new Rol();
            rol.setNombre(nombre);
            return rolRepository.save(rol);
        });
    }

    private void crearAccionSiNoExiste(String nombre) {
        accionRepository.findByNombre(nombre).orElseGet(() -> {
            Accion accion = new Accion();
            accion.setNombre(nombre);
            return accionRepository.save(accion);
        });
    }

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
    }
}
```

**Por qué `CommandLineRunner`:** se ejecuta una vez al arrancar. Es idempotente (`orElseGet` solo crea si falta). Para un proyecto real, migrarías esto a `data.sql` con Flyway; para aprender, es perfecto porque es Java y lo puedes depurar.

**Por qué el admin de arranque:** el registro público fuerza `CUIDADOR`. Sin este mecanismo, nadie podría ser `ADMIN` y los endpoints de administración quedarían inaccesibles para siempre (problema del huevo y la gallina). Es un patrón habitual: un admin inicial creado desde configuración y desactivado después.

> **Advertencia:** `CommandLineRunner` se ejecuta en cada arranque, incluido producción. Con `orElseGet` y `existsByEmail` es seguro. Nunca pongas aquí datos de prueba con contraseñas.

### Paso 5.4 — Crea tu primer administrador

En la configuración de ejecución (`api bootRun`), define las variables **antes** de arrancar:

```powershell
$env:BOOTSTRAP_ADMIN_EMAIL = "admin@icnu.local"
$env:BOOTSTRAP_ADMIN_PASSWORD = "AdminSegura123"
```

Arranca la app una vez. Luego comprueba el login en Postman con `00 · Auth → Login admin` (ver [GUIA_POSTMAN.md → Verifica la Fase 5](GUIA_POSTMAN.md#verifica-la-fase-5)): debe devolver `200` y `rol: "ADMIN"`.

El environment de Postman ya trae esos mismos valores en `adminEmail` y `adminPassword`; si cambias la contraseña en un sitio, cámbiala en el otro.

Después, deja las variables vacías y reinicia:

```powershell
$env:BOOTSTRAP_ADMIN_EMAIL = ""
$env:BOOTSTRAP_ADMIN_PASSWORD = ""
```

**Por qué desactivarlo:** una contraseña de admin en una variable de entorno permanente es otra credencial que puede filtrarse. El bootstrap debe ser una operación de una sola vez.

> Alternativa si no quieres tocar `DataInitializer`: registra un usuario normal y promuévelo con SQL una vez:
> ```sql
> UPDATE usuarios
> SET rol_id = (SELECT id FROM roles WHERE nombre = 'ADMIN')
> WHERE email = 'ana@mail.com';
> ```
> Después vuelve a hacer login (el token anterior conserva el rol viejo).

### Verifica la Fase 5

Checklist (el paso a paso, con requests, bodies y asserts, está en [`GUIA_POSTMAN.md` → Verifica la Fase 5](GUIA_POSTMAN.md#verifica-la-fase-5)):

- [ ] El admin de bootstrap hace login y devuelve `rol: "ADMIN"`.
- [ ] `ADMIN`, `CUIDADOR` y `RED_APOYO` existen, y siguen existiendo una sola vez tras reiniciar.
- [ ] Registro de Ana → `201` con email normalizado y sin contraseña; login → `200`.
- [ ] `GET /api/usuarios` con token de Ana → `403`; sin token → `401`.
- [ ] `POST /api/roles` con admin → `201`; con cuidador → `403`.

---

## Fase 6 — PacienteController

### Paso 6.1 — Ajusta los repositorios

**Archivo:** `src/main/java/com/icnu/api/repositorio/BitacoraRepository.java`

Añade el borrado por paciente (lo usa `PacienteService.eliminar`):

```java
package com.icnu.api.repositorio;

import com.icnu.api.entidad.Bitacora;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BitacoraRepository extends JpaRepository<Bitacora, Long> {

    boolean existsByUsuario_Id(Long usuarioId);

    void deleteByPaciente_Id(Long pacienteId);
}
```

**Archivo:** `src/main/java/com/icnu/api/repositorio/PacienteRepository.java`

```java
package com.icnu.api.repositorio;

import com.icnu.api.entidad.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Page<Paciente> findByCuidadores_Id(Long usuarioId, Pageable pageable);

    boolean existsByIdAndCuidadores_Id(Long pacienteId, Long usuarioId);
}
```

**Qué se elimina:** el `findById` declarado a mano. `JpaRepository` ya lo trae; redeclararlo no aporta nada y confunde.

**Por qué `existsByIdAndCuidadores_Id`:** permite comprobar pertenencia con una consulta booleana, sin cargar el grafo de cuidadores en memoria. Más barato y más claro.

### Paso 6.2 — Reescribe `PacienteService`

**Archivo:** `src/main/java/com/icnu/api/servicio/PacienteService.java`

```java
package com.icnu.api.servicio;

import com.icnu.api.dtos.PacienteRequestDto;
import com.icnu.api.dtos.PacienteResponseDto;
import com.icnu.api.entidad.Paciente;
import com.icnu.api.entidad.Usuario;
import com.icnu.api.excepciones.AccesoDenegadoException;
import com.icnu.api.excepciones.ResourceNotFoundException;
import com.icnu.api.repositorio.BitacoraRepository;
import com.icnu.api.repositorio.PacienteRepository;
import com.icnu.api.repositorio.UsuarioRepository;
import com.icnu.api.util.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final BitacoraRepository bitacoraRepository;

    @Transactional(readOnly = true)
    public Page<PacienteResponseDto> listar(Pageable pageable, String emailAutenticado) {
        Usuario actual = usuarioActual(emailAutenticado);

        Page<Paciente> pagina = esAdmin(actual)
                ? pacienteRepository.findAll(pageable)
                : pacienteRepository.findByCuidadores_Id(actual.getId(), pageable);

        return pagina.map(PacienteResponseDto::from);
    }

    @Transactional(readOnly = true)
    public PacienteResponseDto obtener(Long id, String emailAutenticado) {
        Paciente paciente = buscar(id);
        verificarAcceso(paciente, emailAutenticado);
        return PacienteResponseDto.from(paciente);
    }

    @Transactional
    public PacienteResponseDto crear(PacienteRequestDto dto, String emailAutenticado) {
        Usuario actual = usuarioActual(emailAutenticado);

        Paciente paciente = new Paciente();
        aplicar(dto, paciente);
        paciente = pacienteRepository.save(paciente);

        // Lado dueño de la relación N:M: Usuario.pacientes
        actual.getPacientes().add(paciente);

        return PacienteResponseDto.from(paciente);
    }

    @Transactional
    public PacienteResponseDto actualizar(
            Long id,
            PacienteRequestDto dto,
            String emailAutenticado) {

        Paciente paciente = buscar(id);
        verificarAcceso(paciente, emailAutenticado);

        aplicar(dto, paciente);
        return PacienteResponseDto.from(paciente);
    }

    @Transactional
    public PacienteResponseDto asignarCuidador(Long pacienteId, Long usuarioId) {
        Paciente paciente = buscar(pacienteId);

        Usuario cuidador = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario con el id [" + usuarioId + "] no existe."));

        boolean yaAsignado = cuidador.getPacientes().stream()
                .anyMatch(p -> p.getId().equals(pacienteId));

        if (!yaAsignado) {
            cuidador.getPacientes().add(paciente);
        }
        return PacienteResponseDto.from(paciente);
    }

    @Transactional
    public void desasignarCuidador(Long pacienteId, Long usuarioId) {
        Usuario cuidador = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario con el id [" + usuarioId + "] no existe."));

        cuidador.getPacientes().removeIf(p -> p.getId().equals(pacienteId));
    }

    @Transactional
    public void eliminar(Long id) {
        Paciente paciente = buscar(id);

        // 1. Las bitácoras tienen FK al paciente: hay que borrarlas primero.
        bitacoraRepository.deleteByPaciente_Id(id);

        // 2. El paciente es el lado inverso del N:M: hay que desvincularlo.
        List<Usuario> cuidadores = usuarioRepository.findByPacientes_Id(id);
        cuidadores.forEach(cuidador ->
                cuidador.getPacientes().removeIf(p -> p.getId().equals(id)));

        // 3. Ahora sí.
        pacienteRepository.delete(paciente);
    }

    private void aplicar(PacienteRequestDto dto, Paciente paciente) {
        paciente.setNombre(dto.nombre().trim());
        paciente.setFechaNacimiento(dto.fechaNacimiento());
        paciente.setAlergias(dto.alergias());
        paciente.setEnfermedades(dto.enfermedades());
        paciente.setDiscapacidad(dto.discapacidad());
        paciente.setFamiliarEmergencia(dto.familiarEmergencia());
        paciente.setMedicoEmergencia(dto.medicoEmergencia());
    }

    private Paciente buscar(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El paciente con el id [" + id + "] no existe."));
    }

    private void verificarAcceso(Paciente paciente, String emailAutenticado) {
        Usuario actual = usuarioActual(emailAutenticado);

        if (esAdmin(actual)) {
            return;
        }

        boolean esCuidador = pacienteRepository
                .existsByIdAndCuidadores_Id(paciente.getId(), actual.getId());

        if (!esCuidador) {
            throw new AccesoDenegadoException(
                    "No tienes acceso a este paciente.");
        }
    }

    private Usuario usuarioActual(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario autenticado ya no existe."));
    }

    private static boolean esAdmin(Usuario usuario) {
        return Roles.ADMIN.equals(usuario.getRol().getNombre());
    }
}
```

**Por qué `@Transactional` en `crear`:** sin él, `save(paciente)` y `actual.getPacientes().add(...)` ocurren en transacciones distintas. Si el segundo paso falla, queda un paciente huérfano. Con `@Transactional`, ambos se confirman o ambos se revierten.

**Por qué no se llama a `save(usuario)`:** el usuario se cargó con `findByEmail` dentro de la transacción, así que está gestionado. Al añadir el paciente a su lista, Hibernate inserta la fila en `usuario_has_paciente` al hacer commit. Si llamas a `save`, no rompe nada, pero no es necesario.

**Por qué se desvincula antes de borrar:** `Paciente.cuidadores` es `mappedBy`, o sea, el **lado inverso**. Hibernate no gestiona solo la tabla intermedia desde ese lado. Si borras el paciente sin limpiar `Usuario.pacientes`, la FK de `usuario_has_paciente` impide el `DELETE`.

**Por qué `bitacoraRepository.deleteByPaciente_Id`:** las bitácoras son historia clínica. Tienes dos opciones de producto:
- **Borrado físico** (lo que hace este código): se pierde el historial.
- **Borrado lógico** (recomendado a futuro): añadir `activo boolean` y filtrar, conservando trazabilidad.

Para un MVP, el borrado físico es aceptable, pero debe ser una decisión explícita, no un `500` por FK.

### Paso 6.3 — Reescribe `PacienteController`

**Archivo:** `src/main/java/com/icnu/api/controlador/PacienteController.java`

```java
package com.icnu.api.controlador;

import com.icnu.api.dtos.AsignarCuidadorRequestDto;
import com.icnu.api.dtos.PageResponse;
import com.icnu.api.dtos.PacienteRequestDto;
import com.icnu.api.dtos.PacienteResponseDto;
import com.icnu.api.servicio.PacienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;

    @GetMapping
    public ResponseEntity<PageResponse<PacienteResponseDto>> listar(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {

        return ResponseEntity.ok(PageResponse.from(
                pacienteService.listar(pageable, authentication.getName())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PacienteResponseDto> obtener(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                pacienteService.obtener(id, authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<PacienteResponseDto> crear(
            @Valid @RequestBody PacienteRequestDto dto,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pacienteService.crear(dto, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PacienteResponseDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PacienteRequestDto dto,
            Authentication authentication) {

        return ResponseEntity.ok(
                pacienteService.actualizar(id, dto, authentication.getName()));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/{id}/cuidadores")
    public ResponseEntity<PacienteResponseDto> asignarCuidador(
            @PathVariable Long id,
            @Valid @RequestBody AsignarCuidadorRequestDto dto) {

        return ResponseEntity.ok(
                pacienteService.asignarCuidador(id, dto.usuarioId()));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}/cuidadores/{usuarioId}")
    public ResponseEntity<Void> desasignarCuidador(
            @PathVariable Long id,
            @PathVariable Long usuarioId) {

        pacienteService.desasignarCuidador(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        pacienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Comparación con el controller anterior:**

| Antes | Ahora |
|---|---|
| `PUT /{id}` sin body y borraba | `PUT /{id}` con body y actualiza |
| No había `DELETE` | `DELETE /{id}` (solo admin) |
| `POST /usuario/{usuarioId}` | `POST /pacientes` + `POST /{id}/cuidadores` |
| Devolvía `Paciente` (recursión) | Devuelve `PacienteResponseDto` |
| Sin paginación | `PageResponse` |
| `usuarioId` del path | Usuario del token |

**Por qué `POST /{id}/cuidadores` en vez de `/usuario/{usuarioId}`:** el recurso que estás modificando es el paciente; el cuidador es un subrecurso. La URL refleja el modelo, no la tabla intermedia. Además permite asignar varios cuidadores con el mismo endpoint.

### Verifica la Fase 6

Checklist (detalle en [`GUIA_POSTMAN.md` → Verifica la Fase 6](GUIA_POSTMAN.md#verifica-la-fase-6)):

- [ ] `POST /api/pacientes` → `201` con DTO plano (sin `cuidadores`).
- [ ] Ana ve su paciente; Beto recibe `403`.
- [ ] `PUT` actualiza y **no** borra.
- [ ] `DELETE` con Ana → `403`; con admin → `204` y luego `404`.
- [ ] Asignar a Beto le da acceso sin recursión JSON.

---

## Fase 7 — Bitácora y Acciones

### Paso 7.1 — Corrige la entidad `Bitacora`

**Archivo:** `src/main/java/com/icnu/api/entidad/Bitacora.java`

Elimina el método:

```java
public boolean existById(Long id) {
    return true;
}
```

**Por qué:** devolver siempre `true` no es una comprobación, es un placeholder que se coló en producción. La existencia se comprueba con el repositorio (`findById(...).isPresent()` o `existsById`).

### Paso 7.2 — Ajusta `BitacoraRepository`

**Archivo:** `src/main/java/com/icnu/api/repositorio/BitacoraRepository.java`

```java
package com.icnu.api.repositorio;

import com.icnu.api.entidad.Bitacora;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BitacoraRepository extends JpaRepository<Bitacora, Long> {

    @EntityGraph(attributePaths = {"usuario", "paciente", "accion"})
    Page<Bitacora> findByPaciente_IdOrderByFechaDesc(Long pacienteId, Pageable pageable);

    void deleteByPaciente_Id(Long pacienteId);

    boolean existsByUsuario_Id(Long usuarioId);
}
```

**Por qué `OrderByFechaDesc`:** un historial se lee de lo más reciente a lo más antiguo. Si lo ordenas en memoria, pierdes la paginación eficiente.

### Paso 7.3 — Ajusta los DTOs de bitácora

**Archivo:** `src/main/java/com/icnu/api/dtos/BitacoraRequestDto.java`

```java
package com.icnu.api.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record BitacoraRequestDto(
        LocalDateTime fecha,

        @Size(max = 2000, message = "Las notas no pueden superar 2000 caracteres.")
        String notas,

        @NotNull(message = "El paciente es obligatorio.")
        Long pacienteId,

        Long accionId) {
}
```

**Qué se elimina:** `usuarioId`. El usuario que registra sale del token, no del body. Si lo aceptaras, un cuidador podría escribir una nota a nombre de otro.

**Archivo:** `src/main/java/com/icnu/api/dtos/BitacoraResponseDto.java`

```java
package com.icnu.api.dtos;

import com.icnu.api.entidad.Bitacora;

import java.time.LocalDateTime;

public record BitacoraResponseDto(
        Long id,
        LocalDateTime fecha,
        String notas,
        Long usuarioId,
        Long pacienteId,
        Long accionId) {

    public static BitacoraResponseDto from(Bitacora bitacora) {
        return new BitacoraResponseDto(
                bitacora.getId(),
                bitacora.getFecha(),
                bitacora.getNotas(),
                bitacora.getUsuario().getId(),
                bitacora.getPaciente().getId(),
                bitacora.getAccion() != null ? bitacora.getAccion().getId() : null);
    }
}
```

### Paso 7.4 — Reescribe `BitacoraService`

**Archivo:** `src/main/java/com/icnu/api/servicio/BitacoraService.java`

```java
package com.icnu.api.servicio;

import com.icnu.api.dtos.BitacoraRequestDto;
import com.icnu.api.dtos.BitacoraResponseDto;
import com.icnu.api.entidad.Accion;
import com.icnu.api.entidad.Bitacora;
import com.icnu.api.entidad.Paciente;
import com.icnu.api.entidad.Usuario;
import com.icnu.api.excepciones.AccesoDenegadoException;
import com.icnu.api.excepciones.ResourceNotFoundException;
import com.icnu.api.repositorio.AccionRepository;
import com.icnu.api.repositorio.BitacoraRepository;
import com.icnu.api.repositorio.PacienteRepository;
import com.icnu.api.repositorio.UsuarioRepository;
import com.icnu.api.util.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BitacoraService {

    private final BitacoraRepository bitacoraRepository;
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final AccionRepository accionRepository;

    @Transactional(readOnly = true)
    public BitacoraResponseDto obtener(Long id, String emailAutenticado) {
        Bitacora bitacora = buscar(id);
        verificarAccesoAlPaciente(bitacora.getPaciente(), emailAutenticado);

        if (!esAutorOAdmin(bitacora.getUsuario(), emailAutenticado)) {
            throw new AccesoDenegadoException(
                    "No puedes consultar esta bitácora.");
        }
        return BitacoraResponseDto.from(bitacora);
    }

    @Transactional(readOnly = true)
    public Page<BitacoraResponseDto> listarPorPaciente(
            Long pacienteId,
            Pageable pageable,
            String emailAutenticado) {

        Paciente paciente = buscarPaciente(pacienteId);
        verificarAccesoAlPaciente(paciente, emailAutenticado);

        return bitacoraRepository
                .findByPaciente_IdOrderByFechaDesc(pacienteId, pageable)
                .map(BitacoraResponseDto::from);
    }

    @Transactional
    public BitacoraResponseDto crear(
            BitacoraRequestDto dto,
            String emailAutenticado) {

        Usuario autor = usuarioActual(emailAutenticado);
        Paciente paciente = buscarPaciente(dto.pacienteId());
        verificarAccesoAlPaciente(paciente, emailAutenticado);

        Bitacora bitacora = new Bitacora();
        bitacora.setUsuario(autor);
        bitacora.setPaciente(paciente);
        bitacora.setFecha(dto.fecha() != null ? dto.fecha() : LocalDateTime.now());
        bitacora.setNotas(dto.notas()); // <-- el bug anterior: nunca se asignaba

        if (dto.accionId() != null) {
            Accion accion = accionRepository.findById(dto.accionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "La acción con el id [" + dto.accionId() + "] no existe."));
            bitacora.setAccion(accion);
        }

        return BitacoraResponseDto.from(bitacoraRepository.save(bitacora));
    }

    @Transactional
    public BitacoraResponseDto actualizar(
            Long id,
            BitacoraRequestDto dto,
            String emailAutenticado) {

        Bitacora bitacora = buscar(id);
        verificarAccesoAlPaciente(bitacora.getPaciente(), emailAutenticado);

        if (!esAutorOAdmin(bitacora.getUsuario(), emailAutenticado)) {
            throw new AccesoDenegadoException(
                    "Solo el autor o un administrador pueden editar esta bitácora.");
        }

        if (dto.fecha() != null) {
            bitacora.setFecha(dto.fecha());
        }
        if (dto.notas() != null) {
            bitacora.setNotas(dto.notas().trim());
        }
        if (dto.accionId() != null) {
            Accion accion = accionRepository.findById(dto.accionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "La acción con el id [" + dto.accionId() + "] no existe."));
            bitacora.setAccion(accion);
        }

        return BitacoraResponseDto.from(bitacora);
    }

    @Transactional
    public void eliminar(Long id, String emailAutenticado) {
        Bitacora bitacora = buscar(id);

        if (!esAutorOAdmin(bitacora.getUsuario(), emailAutenticado)) {
            throw new AccesoDenegadoException(
                    "Solo el autor o un administrador pueden eliminar esta bitácora.");
        }
        bitacoraRepository.delete(bitacora);
    }

    private Bitacora buscar(Long id) {
        return bitacoraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La bitácora con el id [" + id + "] no existe."));
    }

    private Paciente buscarPaciente(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El paciente con el id [" + id + "] no existe."));
    }

    private Usuario usuarioActual(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario autenticado ya no existe."));
    }

    private void verificarAccesoAlPaciente(Paciente paciente, String emailAutenticado) {
        Usuario actual = usuarioActual(emailAutenticado);

        if (esAdmin(actual)) {
            return;
        }

        boolean esCuidador = pacienteRepository
                .existsByIdAndCuidadores_Id(paciente.getId(), actual.getId());

        if (!esCuidador) {
            throw new AccesoDenegadoException(
                    "No tienes acceso al historial de este paciente.");
        }
    }

    private boolean esAutorOAdmin(Usuario autor, String emailAutenticado) {
        Usuario actual = usuarioActual(emailAutenticado);
        return esAdmin(actual) || autor.getEmail().equals(actual.getEmail());
    }

    private static boolean esAdmin(Usuario usuario) {
        return Roles.ADMIN.equals(usuario.getRol().getNombre());
    }
}
```

**Bugs corregidos, uno por uno:**

| Bug | Línea original | Corrección |
|---|---|---|
| NPE al borrar | `BitacoraService.java:36-38` | Ya no se usa una variable nula; `buscar(id)` lanza 404 |
| `existById` falso | `Bitacora.java:39-41` | Eliminado |
| `notas` nunca se guardan | `BitacoraService.java:57-78` | `bitacora.setNotas(dto.notas())` |
| Admin siempre denegado | `BitacoraService.java:66` | `Roles.ADMIN.equals(...)` en lugar de `!= Rol.admin` |
| Mensaje erróneo | `BitacoraService.java:55` | "El paciente... no existe" |
| `RuntimeException` genérica | varias | `ResourceNotFoundException` / `AccesoDenegadoException` |
| Typo `uppdateBitacora` | `:93` | `actualizar` |

**Nota sobre la regla "solo el admin registra acciones":** la eliminé. Era una regla de producto aplicada con una comparación de entidades rota. Si tu producto la necesita, impleméntala así:

```java
if (dto.accionId() != null && !esAdmin(autor)) {
    throw new BusinessRuleException("Solo un administrador puede registrar acciones.");
}
```

Pero hazlo de forma consciente y documentada. En una bitácora de cuidados, lo normal es que cualquier cuidador registre una acción (medicación, cita).

### Paso 7.5 — Crea `BitacoraController`

**Archivo nuevo:** `src/main/java/com/icnu/api/controlador/BitacoraController.java`

```java
package com.icnu.api.controlador;

import com.icnu.api.dtos.BitacoraRequestDto;
import com.icnu.api.dtos.BitacoraResponseDto;
import com.icnu.api.dtos.PageResponse;
import com.icnu.api.servicio.BitacoraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bitacoras")
@RequiredArgsConstructor
public class BitacoraController {

    private final BitacoraService bitacoraService;

    @GetMapping
    public ResponseEntity<PageResponse<BitacoraResponseDto>> listarPorPaciente(
            @RequestParam Long pacienteId,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {

        return ResponseEntity.ok(PageResponse.from(
                bitacoraService.listarPorPaciente(
                        pacienteId, pageable, authentication.getName())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BitacoraResponseDto> obtener(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                bitacoraService.obtener(id, authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<BitacoraResponseDto> crear(
            @Valid @RequestBody BitacoraRequestDto dto,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bitacoraService.crear(dto, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BitacoraResponseDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody BitacoraRequestDto dto,
            Authentication authentication) {

        return ResponseEntity.ok(
                bitacoraService.actualizar(id, dto, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            Authentication authentication) {

        bitacoraService.eliminar(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
```

### Paso 7.6 — Ajusta `AccionService` y crea su controller

**Archivo:** `src/main/java/com/icnu/api/servicio/AccionService.java`

```java
package com.icnu.api.servicio;

import com.icnu.api.dtos.AccionRequestDto;
import com.icnu.api.dtos.AccionResponseDto;
import com.icnu.api.entidad.Accion;
import com.icnu.api.excepciones.DuplicateResourceException;
import com.icnu.api.excepciones.ResourceNotFoundException;
import com.icnu.api.repositorio.AccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AccionService {

    private final AccionRepository accionRepository;

    @Transactional(readOnly = true)
    public List<AccionResponseDto> listar() {
        return accionRepository.findAll().stream()
                .map(AccionResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccionResponseDto obtener(Long id) {
        return AccionResponseDto.from(buscar(id));
    }

    @Transactional
    public AccionResponseDto crear(AccionRequestDto dto) {
        String nombre = dto.nombre().trim().toUpperCase(Locale.ROOT);

        if (accionRepository.findByNombre(nombre).isPresent()) {
            throw new DuplicateResourceException(
                    "La acción [" + nombre + "] ya está registrada.");
        }

        Accion accion = new Accion();
        accion.setNombre(nombre);
        return AccionResponseDto.from(accionRepository.save(accion));
    }

    private Accion buscar(Long id) {
        return accionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La acción con el id [" + id + "] no existe."));
    }
}
```

**Archivo nuevo:** `src/main/java/com/icnu/api/dtos/AccionRequestDto.java`

```java
package com.icnu.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccionRequestDto(
        @NotBlank(message = "El nombre de la acción es obligatorio.")
        @Size(max = 50)
        String nombre) {
}
```

**Archivo nuevo:** `src/main/java/com/icnu/api/dtos/AccionResponseDto.java`

```java
package com.icnu.api.dtos;

import com.icnu.api.entidad.Accion;

public record AccionResponseDto(Long id, String nombre) {

    public static AccionResponseDto from(Accion accion) {
        return new AccionResponseDto(accion.getId(), accion.getNombre());
    }
}
```

**Archivo nuevo:** `src/main/java/com/icnu/api/controlador/AccionController.java`

```java
package com.icnu.api.controlador;

import com.icnu.api.dtos.AccionRequestDto;
import com.icnu.api.dtos.AccionResponseDto;
import com.icnu.api.servicio.AccionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/acciones")
@RequiredArgsConstructor
public class AccionController {

    private final AccionService accionService;

    @GetMapping
    public ResponseEntity<List<AccionResponseDto>> listar() {
        return ResponseEntity.ok(accionService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccionResponseDto> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(accionService.obtener(id));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public ResponseEntity<AccionResponseDto> crear(
            @Valid @RequestBody AccionRequestDto dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accionService.crear(dto));
    }
}
```

### Verifica la Fase 7

Checklist (detalle en [`GUIA_POSTMAN.md` → Verifica la Fase 7](GUIA_POSTMAN.md#verifica-la-fase-7)):

- [ ] Crear bitácora → `201` con `notas` y `usuarioId` del token.
- [ ] Beto recibe `403` al leer y al editar la bitácora de Ana.
- [ ] Ana puede editar y borrar la suya.
- [ ] `pacienteId` inexistente → `404`, no `500`.
- [ ] Borrar un paciente con bitácoras → `204` sin error de FK.

---

## Fase 8 — Pruebas

### Paso 8.1 — Arregla el test que hoy depende de MySQL

Hoy `ApiApplicationTests` hace `@SpringBootTest`, que levanta el contexto completo y **necesita conexión real** a MySQL. En una máquina limpia o en CI, falla.

Opciones:

**A. Testcontainers (recomendado).** En `build.gradle`:

```gradle
testImplementation 'org.springframework.boot:spring-boot-testcontainers'
testImplementation 'org.testcontainers:junit-jupiter'
testImplementation 'org.testcontainers:mysql'
```

Y el test:

```java
package com.icnu.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class ApiApplicationTests {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Test
    void contextLoads() {
    }
}
```

**Por qué:** Testcontainers levanta un MySQL desechable en Docker. El test es realista y no depende de tu base local. `@ServiceConnection` conecta el contenedor a `spring.datasource` automáticamente (Boot 3.1+).

Requisito: Docker corriendo. Si no lo tienes, usa la opción B temporalmente.

**B. H2 en memoria (rápido pero menos fiel).** Añade `testRuntimeOnly 'com.h2database:h2'` y `src/test/resources/application.properties` con:

```properties
spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
app.jwt.secret=test-secret-de-al-menos-32-caracteres-para-hs256
```

**Trade-off:** H2 no se comporta igual que MySQL (tipos, collation, FK). Sirve para tests unitarios, no para validar SQL complejo.

### Paso 8.2 — Test unitario de servicio (sin Spring)

**Archivo nuevo:** `src/test/java/com/icnu/api/servicio/RolServiceTest.java`

```java
package com.icnu.api.servicio;

import com.icnu.api.dtos.RolRequestDto;
import com.icnu.api.dtos.RolResponseDto;
import com.icnu.api.excepciones.BusinessRuleException;
import com.icnu.api.repositorio.RolRepository;
import com.icnu.api.repositorio.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolServiceTest {

    @Mock
    private RolRepository rolRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private RolService rolService;

    @Test
    void normalizaElNombreAntesDeGuardar() {
        when(rolRepository.findByNombre("CUIDADOR_PRINCIPAL"))
                .thenReturn(Optional.empty());
        when(rolRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RolResponseDto creado = rolService.crear(
                new RolRequestDto("  cuidador   principal "));

        assertThat(creado.nombre()).isEqualTo("CUIDADOR_PRINCIPAL");
    }

    @Test
    void rechazaNombreVacio() {
        assertThatThrownBy(() -> rolService.crear(new RolRequestDto("   ")))
                .isInstanceOf(BusinessRuleException.class);
    }
}
```

**Por qué sin Spring:** un test unitario debe ser rápido y no depender de BD ni de contexto. Mockito simula el repositorio. Si el test tarda 5 segundos, alguien dejará de ejecutarlo.

**Qué estás probando realmente:** la normalización, que es donde estaba el bug (`getRolPorNombre` no normalizaba). No pruebes getters.

### Paso 8.3 — Test de controller con seguridad desactivada

**Archivo nuevo:** `src/test/java/com/icnu/api/controlador/PacienteControllerTest.java`

```java
package com.icnu.api.controlador;

import com.icnu.api.dtos.PacienteResponseDto;
import com.icnu.api.servicio.PacienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PacienteController.class)
@AutoConfigureMockMvc(addFilters = false)
class PacienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PacienteService pacienteService;

    @Test
    void devuelveElPacienteComoDto() throws Exception {
        when(pacienteService.obtener(eq(1L), any()))
                .thenReturn(new PacienteResponseDto(
                        1L, "Rosa", LocalDate.of(1940, 5, 1),
                        null, null, null, null, null));

        mockMvc.perform(get("/api/pacientes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Rosa"))
                .andExpect(jsonPath("$.cuidadores").doesNotExist());
    }
}
```

**Notas importantes:**

- `@MockitoBean` es la forma actual (Spring Boot 3.4+/4). `@MockBean` está obsoleto/eliminado.
- `addFilters = false` desactiva la cadena de seguridad en este slice. Es útil para probar el mapeo HTTP, **no** la autorización.
- El `Authentication` será `null` con filtros desactivados. Por eso el test usa `any()`. Para probar autorización de verdad, necesitas un test de integración con token real.
- `@WebMvcTest` no carga `SecurityConfig` completo, así que `@PreAuthorize` no se evalúa aquí.

### Paso 8.4 — Test de integración del flujo de auth

Para probar `401/403` de verdad, necesitas contexto completo + BD. Con Testcontainers:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthFlowIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @LocalServerPort
    int port;

    @Test
    void loginYAccesoProtegido() {
        // 1. registrar, 2. login, 3. GET con token, 4. GET sin token -> 401
    }
}
```

Usa `TestRestTemplate` o `RestClient`. Este test es el que realmente demuestra que la seguridad funciona. Los unitarios no lo pueden demostrar.

**Orden de valor:** integración de auth > unitarios de normalización > tests de controller con filtros apagados.

### Verifica la Fase 8

```powershell
.\gradlew.bat test
```

- [ ] Los tests unitarios pasan sin Docker ni MySQL.
- [ ] El test de contexto pasa (con Testcontainers o perfil de test).
- [ ] Al menos un test demuestra `401` sin token y `403` con rol insuficiente.

---

## Fase 9 — Limpieza del modelo

Esta fase no es urgente para que la API funcione, pero evita bugs futuros. Hazla **después** de tener tests.

### Paso 9.1 — Sustituye `@Data` en entidades

`@Data` genera `equals`, `hashCode` y `toString` sobre **todos** los campos, incluidas las colecciones. Con JPA eso es peligroso:

- `toString` puede disparar cargas perezosas y llenar los logs.
- `hashCode` cambia cuando cambia un campo mutable, rompiendo `HashSet`.
- `equals` entre entidades con el mismo contenido pero distinto `id` da resultados incorrectos.

Patrón recomendado:

```java
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pacientes")
public class Paciente {
    // ...
}
```

Y si necesitas `equals`/`hashCode` (por ejemplo, para `contains` en colecciones), defínelos por `id`:

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Paciente otro)) return false;
    return id != null && id.equals(otro.id);
}

@Override
public int hashCode() {
    return getClass().hashCode();
}
```

**Por qué `getClass().hashCode()`:** el `id` es `null` antes de persistir; un `hashCode` basado en un campo mutable rompe las colecciones. Este patrón es estable.

Aplícalo a `Usuario`, `Paciente`, `Rol`, `Bitacora` y `Accion`.

### Paso 9.2 — Elimina `Rol.admin`

**Archivo:** `Rol.java`

```java
public static Rol admin;
```

**Por qué:** es un campo estático mutable, compartido por toda la JVM, que nunca se inicializa. La comparación `usuario.getRol() != Rol.admin` compara referencias, no contenido, y siempre es `true`. Ya lo reemplazamos por `Roles.ADMIN.equals(...)`.

### Paso 9.3 — Defensa en profundidad contra ciclos JSON

Aunque los controllers ya devuelven DTOs, añade:

```java
@JsonIgnore
private List<Paciente> pacientes;
```

en `Usuario`, y

```java
@JsonIgnore
private List<Usuario> cuidadores;
```

en `Paciente`.

**Por qué:** si algún día alguien devuelve una entidad por error, el fallo será un JSON incompleto, no un `StackOverflowError`. Es barato y evita un incidente.

### Paso 9.4 — Limpia imports

- `UsuarioRepository`: quita `javax.swing.text.html.Option`.
- `RolRepository`: quita `com.icnu.api.entidad.Usuario` (no se usa).
- `PacienteService`: quita `java.util.Optional` si queda.
- `Rol.java`: quita `java.util.Optional` (no se usa).

**Por qué:** los imports muertos no rompen nada, pero indican copia/pega y despistan al IDE. En una revisión de código, son ruido.

### Paso 9.5 — Decide qué hacer con `spring.jpa.hibernate.ddl-auto`

Hoy está en `update`. Para aprender, está bien: Hibernate crea las tablas.

Para producción:

1. Añade Flyway:

```gradle
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-mysql'
```

2. Crea `src/main/resources/db/migration/V1__schema_inicial.sql` con el DDL.
3. Cambia a `spring.jpa.hibernate.ddl-auto=validate`.

**Por qué:** `update` puede alterar columnas de forma silenciosa y no tiene rollback. Las migraciones son versionadas, revisables y reproducibles. Además, resuelven el drift actual entre el `.mwb` y las entidades.

### Paso 9.6 — Revisa el esquema del diagrama

El `SQL_script_ICNU.sql` tiene decisiones que no coinciden con el código:

| Diagrama | Problema | Acción |
|---|---|---|
| `UNIQUE` en `contrasenia` | Dos usuarios no podrían compartir hash (y es semánticamente absurdo) | Quitar |
| `accion_id_accion` `NOT NULL` | La entidad lo permite nulo | Alinear |
| `rol.id` sin `AUTO_INCREMENT` | La entidad usa `IDENTITY` | Alinear |
| Nombres `id_usuario`, `id_paciente` | Las entidades usan `id` | Elegir una convención |
| Esquema `icnu_database` | La app usa `icnu_db` | Unificar |
| `bitacora` con PK compuesta | La entidad tiene `id` simple | Unificar |

**Por qué importa:** mientras haya dos fuentes de verdad, un compañero puede crear la BD con el SQL y la app conectarse a otra. Los bugs de "en mi máquina funciona" nacen aquí.

---

## Fase 10 — Documentación del contrato

### Paso 10.1 — Documenta antes de que el frontend consuma

Con los controllers listos, genera el contrato:

```gradle
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0'
```

> Verifica la versión compatible con Spring Boot 4 en la documentación oficial de springdoc. No copies una versión de un tutorial viejo.

Luego:

- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/v3/api-docs`

**Por qué:** el frontend puede trabajar con el contrato antes de que el backend esté desplegado. Además, Swagger UI te deja probar el flujo de login sin escribir `curl`.

### Paso 10.2 — Actualiza el README

El README actual dice que falta `UsuarioController` y menciona `application.properties.example`, pero el archivo es `.template`. Actualiza:

- Estado real de los endpoints.
- Cómo configurar `JWT_SECRET` y `DB_PASSWORD`.
- Cómo correr los tests.
- Roles disponibles y qué puede hacer cada uno.

### Paso 10.3 — Tabla de endpoints final

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/login` | Público | Devuelve JWT |
| POST | `/api/usuarios/registro` | Público | Crea cuidador |
| GET | `/api/usuarios` | ADMIN | Lista paginada |
| GET | `/api/usuarios/{id}` | Self/ADMIN | Detalle |
| POST | `/api/usuarios` | ADMIN | Crea con rol |
| PUT | `/api/usuarios/{id}` | Self/ADMIN | Actualiza perfil |
| PUT | `/api/usuarios/{id}/contrasenia` | Self | Cambia contraseña |
| DELETE | `/api/usuarios/{id}` | ADMIN | Elimina |
| GET | `/api/roles` | Autenticado | Catálogo |
| GET | `/api/roles/{id}` | Autenticado | Detalle |
| POST | `/api/roles` | ADMIN | Crea rol |
| DELETE | `/api/roles/{id}` | ADMIN | Elimina si no está en uso |
| GET | `/api/pacientes` | ADMIN ve todos; cuidador ve los suyos | Lista paginada |
| GET | `/api/pacientes/{id}` | ADMIN o cuidador asignado | Detalle |
| POST | `/api/pacientes` | Autenticado | Crea y vincula al autor |
| PUT | `/api/pacientes/{id}` | ADMIN o cuidador asignado | Actualiza |
| POST | `/api/pacientes/{id}/cuidadores` | ADMIN | Asigna cuidador |
| DELETE | `/api/pacientes/{id}/cuidadores/{usuarioId}` | ADMIN | Desasigna |
| DELETE | `/api/pacientes/{id}` | ADMIN | Elimina (y sus bitácoras) |
| GET | `/api/bitacoras?pacienteId=` | ADMIN o cuidador asignado | Historial paginado |
| GET | `/api/bitacoras/{id}` | Autor o ADMIN | Detalle |
| POST | `/api/bitacoras` | Cuidador asignado | Registra nota |
| PUT | `/api/bitacoras/{id}` | Autor o ADMIN | Edita |
| DELETE | `/api/bitacoras/{id}` | Autor o ADMIN | Elimina |
| GET | `/api/acciones` | Autenticado | Catálogo |
| POST | `/api/acciones` | ADMIN | Crea acción |

---

## Apéndice A — Checklist final

### Seguridad

- [ ] No hay secretos en el código ni en Git.
- [ ] `JWT_SECRET` y `DB_PASSWORD` vienen de variables de entorno.
- [ ] `@EnableMethodSecurity` está activo.
- [ ] El filtro setea `SecurityContext` y devuelve `401`.
- [ ] `anyRequest().authenticated()` y whitelist explícita.
- [ ] CORS configurado con el origen real del frontend.
- [ ] El login devuelve el mismo error para email inexistente y contraseña errónea.
- [ ] El registro público no permite elegir rol.
- [ ] El `id` del usuario sale del token, no del body/path.

### Contrato

- [ ] Ningún controller devuelve entidades JPA.
- [ ] Ningún DTO de creación acepta `id`.
- [ ] Todas las entradas usan `@Valid`.
- [ ] Los errores usan `ProblemDetail`.
- [ ] `201` en creaciones, `204` en borrados, `404` en ausencias, `409` en duplicados.
- [ ] Listados paginados.

### Persistencia

- [ ] Los métodos que escriben tienen `@Transactional`.
- [ ] Las relaciones N:M se modifican por el lado dueño (`Usuario.pacientes`).
- [ ] El borrado limpia relaciones y bitácoras.
- [ ] `open-in-view=false`.
- [ ] Plan de migración a Flyway.

### Calidad

- [ ] `.\gradlew.bat build` pasa.
- [ ] Hay tests unitarios de las reglas de negocio.
- [ ] Hay un test de integración de auth.
- [ ] El README refleja el estado real.

---

## Apéndice B — Errores comunes al aplicar la guía

### `bootRun` termina con `exit value 1`

Ese mensaje es genérico: Gradle solo dice que el proceso Java terminó mal. La causa real está **arriba**, en la primera línea `Caused by:` o en `APPLICATION FAILED TO START`. Ejecuta con log completo:

```powershell
.\gradlew.bat bootRun --console=plain *> bootrun.log
Select-String -Path bootrun.log -Pattern "Caused by|APPLICATION FAILED|Access denied|Connection refused"
```

### `Connection refused` al conectar a MySQL

No hay ningún servidor escuchando en `localhost:3306`. Comprueba:

```powershell
Test-NetConnection -ComputerName 127.0.0.1 -Port 3306 -InformationLevel Quiet
```

- Si es `False`, MySQL no está arrancado.
- Si usas MySQL dentro de **WSL/Ubuntu**, ábrelo y ejecuta `sudo service mysql start` (WSL no arranca servicios automáticamente si no tiene `systemd`).
- Si usas XAMPP/Laragon, inicia MySQL desde su panel.
- Si usas Docker: `docker ps` y arranca el contenedor.

### `Access denied for user 'root'@'localhost' (using password: YES)`

El servidor responde, pero la contraseña enviada no es válida. Ojo al detalle:

- `using password: YES` significa que **sí** se envió una contraseña, no que sea correcta.
- Si `DB_PASSWORD` no está definida, Spring puede enviar el texto literal `${DB_PASSWORD}` como contraseña. Ese es el caso más habitual al empezar.

Comprueba que la variable existe **en la misma terminal**:

```powershell
Write-Output "[$env:DB_PASSWORD]"
```

Si sale `[]`, defínela (con comillas simples si contiene `$`):

```powershell
$env:DB_PASSWORD = 'TuPasswordReal'
```

Y verifica la conexión directamente, sin pasar por Spring:

```powershell
mysql -h 127.0.0.1 -P 3306 -u root -p -e "SHOW DATABASES;"
```

Si el comando `mysql` no existe en el PATH, usa el de Workbench:

```powershell
& 'C:\Program Files\MySQL\MySQL Workbench 8.0 CE\mysql.exe' -h 127.0.0.1 -P 3306 -u root -p -e "SHOW DATABASES;"
```

### `Unknown database 'icnu_db'`

MySQL responde y las credenciales son correctas, pero falta la base:

```sql
CREATE DATABASE IF NOT EXISTS icnu_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### En IntelliJ, "Edit Configurations" aparece vacío

No es un error: el proyecto todavía no tiene configuraciones guardadas. Pulsa el botón **`+`** (arriba a la izquierda del diálogo) y elige una:

- **Gradle** → Gradle project: `api`, Tasks: `bootRun`.
- **Application** (IntelliJ Community) → Main class: `com.icnu.api.ApiApplication`, module `api.main`.

Luego, en **Environment variables**, escribe las variables separadas por punto y coma:

```text
DB_PASSWORD=TuPasswordReal;JWT_SECRET=TuSecretoDe32CaracteresOMas
```

> Las variables que defines con `$env:` en una terminal **solo viven en esa terminal**. El botón Run de IntelliJ no las hereda: usa la configuración de ejecución o arranca con `.\gradlew.bat bootRun` desde esa misma terminal.

También puedes llegar a la configuración desde el panel de Gradle: `View → Tool Windows → Gradle → api → Tasks → application → bootRun` (clic derecho → `Modify Run Configuration…`).

### `Could not resolve placeholder 'JWT_SECRET'`

No configuraste la variable de entorno. En PowerShell: `$env:JWT_SECRET = '...'` (comillas simples si contiene `$`). Reinicia el proceso: IntelliJ no recoge variables de una terminal externa.

### `No qualifying bean of type 'JwtAuthenticationFilter'`

Olvidaste `@Component`, o el paquete no está bajo `com.icnu.api` (el `@SpringBootApplication` escanea ese paquete y subpaquetes).

### `@PreAuthorize` devuelve 403 con un token de admin válido

1. Comprueba que `@EnableMethodSecurity` está en `SecurityConfig`.
2. Comprueba que el claim `rol` es exactamente `ADMIN` (sin `ROLE_`).
3. Comprueba que el filtro setea el `Authentication` **antes** del `AuthorizationFilter`.

### Todo devuelve 401, incluido el login

El login debe estar en `permitAll()`. Si moviste la ruta, actualiza el matcher.

### `StackOverflowError` en la respuesta

Algún controller sigue devolviendo una entidad. Busca `ResponseEntity<Paciente>`, `ResponseEntity<Usuario>` o `ResponseEntity<Bitacora>`.

### `LazyInitializationException`

Estás mapeando a DTO fuera de la transacción, o accedes a una colección perezosa en el controller. La conversión debe ocurrir dentro del service `@Transactional`. No lo arregles con `open-in-view=true`.

### `Cannot delete or update a parent row: a foreign key constraint fails`

Falta limpiar una relación antes del `DELETE`. Revisa:
- `bitacora.paciente_id_paciente`
- `bitacora.usuario_id_usuario`
- `usuario_has_paciente`

### `Data too long for column 'contrasenia'`

Estás guardando texto plano en vez del hash. Verifica que `encoder.encode(...)` se aplica siempre y que la columna es `VARCHAR(60)`.

### `401` al usar el token en Postman

Suele ser un espacio de más o falta `Bearer ` (con espacio). El filtro exige el prefijo exacto.

### `Authorization: Bearer <token>` da `401`, pero sin espacio funciona

El error está en la constante del filtro:

```java
private static final String PREFIX = "Bearer";   // mal
```

`startsWith("Bearer")` acepta `BearerXYZ`, pero **no** acepta `Bearer XYZ`. Y `header.substring("Bearer".length())` corta un carácter de menos, dejando un token que empieza por espacio y falla la firma.

La constante correcta incluye el espacio:

```java
private static final String PREFIX = "Bearer ";
```

Formas de comprobarlo:

```text
Authorization: Bearer <token>   -> substring deja " <token>"  -> firma inválida -> 401
Authorization: Bearer<token>    -> substring deja "<token>"   -> firma válida   -> 200/403
```

Si ves exactamente ese patrón, ya sabes dónde mirar. Es un `off-by-one` en el prefijo, no un problema del token.

### El filtro se ejecuta dos veces por petición

`@Component` registra el filtro en **dos** sitios: la cadena de seguridad y la cadena de servlets de Spring Boot. La solución es desactivar el registro global:

```java
@Bean
public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
        JwtAuthenticationFilter filter) {

    FilterRegistrationBean<JwtAuthenticationFilter> registration =
            new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
}
```

No lo notarás como error funcional, pero duplica el trabajo y puede falsear logs o métricas. El comentario en el código no basta: **el bean tiene que existir**.


### Decenas de `cannot find symbol` sobre getters que sí existen

Síntoma: el compilador reporta 50–100 errores del tipo `symbol: method getId()` en entidades que tienen `@Data`, y también `symbol: variable log` en una clase con `@Slf4j`.

Lombok no es magia del IDE: es un **procesador de anotaciones** que corre *dentro* de `javac` y genera esos métodos antes de compilar. Si `javac` encuentra un error en una fase **anterior** al procesamiento de anotaciones (por ejemplo, una anotación aplicada donde no corresponde), **no ejecuta ningún procesador** en esa compilación. Resultado: no existe ningún getter y cada uso produce un error.

Cómo encontrar el error de verdad, sin leer los 86:

1. Busca el **primer** error que **no** sea `cannot find symbol`. Ese es la causa.
2. Arréglalo y recompila: el resto desaparece solo.

Caso real de esta guía: `@RequestMapping` sobre un parámetro en `UsuarioController`. Arreglando **solo esa línea**, la compilación pasó de **86 errores a 7**. Excluyendo ese archivo, quedaban **2**.

> Regla general: ante una avalancha de símbolos faltantes, desconfía de la lista y busca el error estructural. El compilador reporta consecuencias, no causas.

### `Ambiguous @ExceptionHandler method mapped for [...]`

Spring falla **al arrancar**, no al compilar, con:

```text
Failed to introspect @ControllerAdvice class [com.icnu.api.excepciones.GlobalExceptionHandler]
Caused by: IllegalStateException: Ambiguous @ExceptionHandler method mapped for
           [class com.icnu.api.excepciones.AccesoDenegadoException]
```

Causa: dos métodos anotados con `@ExceptionHandler` para la **misma** excepción. Ocurre al copiar un handler para añadir el de Spring y olvidar cambiar el tipo:

```java
@ExceptionHandler(AccesoDenegadoException.class)          // tuya
public ProblemDetail accesoDenegado(...) { ... }

@ExceptionHandler(AccesoDenegadoException.class)          // copia sin cambiar
public ProblemDetail accesoDenegadoSpring(...) { ... }    // ✗ duplicado
```

El segundo debe ser la excepción de Spring Security:

```java
import org.springframework.security.access.AccessDeniedException;

@ExceptionHandler(AccessDeniedException.class)
public ProblemDetail accesoDenegadoSpring(AccessDeniedException ex) {
    return ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "No tienes permisos para realizar esta acción.");
}
```

Desde Spring Security 6, `@PreAuthorize` lanza `AuthorizationDeniedException`, que **extiende** `AccessDeniedException`. Por eso el handler funciona igual.

### `@Valid` en un método de `@Service` no valida nada

`@Valid` solo dispara validación cuando algo lo intercepta: el `@Valid` de un `@RequestBody` en el controller, o un proxy de method validation. Un `@Service` sin `@Validated` **no** está proxeado para eso, así que:

```java
public UsuarioResponseDto actualizar(Long id, @Valid UsuarioUpdateDto dto, ...)
//                                         ^^^^^^ decorativo
```

No rompe nada, pero da una falsa sensación de seguridad. O lo quitas (la validación ya ocurrió en el controller) o pones `@Validated` en la clase. No mezcles: dos capas validando lo mismo con mensajes distintos confunde más de lo que protege.

### El `record` no tiene `getEmail()`

Los `record` usan `email()`. Si mezclas clases y records, revisa cada llamada. El compilador te lo dirá.

### `@MockBean` no compila

En Spring Boot 4 usa `@MockitoBean` (`org.springframework.test.context.bean.override.mockito.MockitoBean`).

### El test de contexto falla sin MySQL

`@SpringBootTest` levanta la app completa. Usa Testcontainers o un perfil de test con H2. No lo desactives con `@Disabled`: oculta el problema.

---

## Apéndice C — Conceptos clave

### JWT

Un token firmado con una clave secreta. Contiene *claims* (subject, rol, expiración). No está cifrado: cualquiera puede leerlo; la firma solo garantiza que no fue alterado. **Nunca pongas datos sensibles dentro** (contraseñas, datos médicos).

### Filtro vs interceptor vs `@PreAuthorize`

- **Filtro**: actúa antes de Spring MVC, sobre el request HTTP. Ideal para leer el token.
- **`@PreAuthorize`**: actúa en el método del controller/service, con el `Authentication` ya disponible. Ideal para reglas de rol.
- **Service**: ideal para reglas que necesitan consultar la BD ("¿este paciente es tuyo?").

### `@Transactional`

Abre una transacción al entrar al método y hace commit al salir (o rollback si hay una `RuntimeException`). Dentro, las entidades cargadas están *managed* y sus cambios se persisten solos (**dirty checking**).

Trampa: la autoinvocación (`this.metodo()`) **no** pasa por el proxy de Spring, así que `@Transactional` no se aplica. Si necesitas que un método interno sea transaccional, muévelo a otro bean o llama desde fuera.

### DTO vs entidad

- **Entidad**: representa una fila y sus relaciones. Vive en la capa de persistencia.
- **DTO**: representa el contrato HTTP. Es inmutable, sin relaciones y sin setters.

Mezclarlos acopla tu API al esquema de la BD y filtra información interna.

### 401 vs 403

- **401 Unauthorized**: no sé quién eres (falta token, expiró, firma inválida).
- **403 Forbidden**: sé quién eres, pero no puedes hacer esto.

Confundirlos hace que el frontend no sepa si debe redirigir al login o mostrar "sin permisos".

### Lado dueño de una relación

En `@ManyToMany`, el lado con `@JoinTable` es el dueño. Solo ese lado decide las filas de la tabla intermedia. El otro usa `mappedBy` y es de solo lectura para la relación. En este proyecto, el dueño es `Usuario.pacientes`.

### IDOR

*Insecure Direct Object Reference*: aceptar un `id` del cliente y devolver el recurso sin comprobar que pertenece al usuario autenticado. Es el fallo de seguridad más común en APIs CRUD. La defensa es tomar la identidad del token y verificar pertenencia en el service.

---

## Orden recomendado de trabajo (resumen)

| Día | Fase | Resultado |
|---|---|---|
| 1 | 0, 1, 2 | La app arranca con secretos externos y seguridad real |
| 2 | 3, 4 | Login, registro, usuarios y errores HTTP correctos |
| 3 | 5, 6 | Roles, pacientes, asignación y borrado sin FK roto |
| 4 | 7 | Bitácoras funcionales y acciones |
| 5 | 8 | Tests que impiden volver atrás |
| 6 | 9, 10 | Modelo limpio, migraciones y contrato documentado |

> **Regla de oro:** no avances de fase sin que la anterior compile y pase sus "Verifica". El objetivo no es escribir mucho código rápido, sino que cada capa sea confiable antes de construir la siguiente.

---

## Apéndice D — Bitácora de la mentoría

> Este apéndice existe para que una sesión futura (humana o IA) retome el trabajo sin tener que releer toda la conversación. Registra el rol acordado, las decisiones cerradas, la evidencia recogida y lo pendiente.

### D.1 Rol y reglas de trabajo

El acuerdo fue: **mentor técnico, no programador**. Eso implica:

| Regla | Motivo |
|---|---|
| No se modifica código en `src/` | El proyecto se aprende escribiéndolo; copiarlo no deja nada |
| Cada cambio se explica con el *por qué* | Un `@RequestBody` pegado no enseña; entender que el JSON del cuerpo se deserializa, sí |
| El único archivo que se edita es esta guía | Evita que guía y código diverjan |
| Los diagnósticos se comprueban, no se adivinan | Cada afirmación se respalda con `compileJava`, `javap`, `git` o una petición HTTP real |
| No se avanza de fase sin superar su "Verifica" | Evita apilar errores y perder la causa raíz |

Corolario importante: ante una avalancha de errores, **primero se aísla la causa y después se explica**. Nunca se listan 86 errores como si fueran 86 problemas.

### D.2 Decisiones de arquitectura cerradas

Si algo contradice esta tabla, es un bug:

| Decisión | Motivo |
|---|---|
| DTOs en la frontera HTTP; las entidades nunca se serializan | Evita ciclos (`Usuario` ↔ `Paciente`), `StackOverflowError` y *mass assignment* |
| Un DTO de entrada **no** es un DTO de salida | `UsuarioUpdateDto` (sin `id`, sin `rol`) para escribir; `UsuarioResponseDto` para leer |
| El `id` del recurso va en la URL, nunca en el body | Un `id` en el body habilita sobrescribir otro registro |
| `POST /api/usuarios/registro` fuerza `CUIDADOR` | Si acepta `rolId`, cualquiera se registra como `ADMIN` |
| El ADMIN inicial se crea con `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD` | Un admin sembrado en código es una puerta trasera permanente |
| JWT stateless con `SessionCreationPolicy.STATELESS` | El servidor no guarda sesión; el token es la identidad |
| La identidad sale del token (`Authentication.getName()`), no del body | Es la defensa contra IDOR |
| `ProblemDetail` (`application/problem+json`) para todos los errores | Contrato de error uniforme y estándar |
| `@EnableMethodSecurity` + `hasAuthority('ADMIN')` (sin prefijo `ROLE_`) | Las autoridades son los nombres de rol tal cual |
| Login con un solo mensaje para email inexistente y contraseña errónea | Evita enumeración de cuentas |
| Secretos solo por variables de entorno | Nada de credenciales versionadas |

### D.3 Estado verificado (17 nov 2026)

| Fase | Estado | Evidencia |
|---|---|---|
| 0 — Línea base | ✅ | `compileJava` OK |
| 1 — Dependencias y secretos | ✅ | `hibernate-validator:9.1.5.Final` en el classpath; `application.properties.template` con placeholders |
| 2 — Seguridad y JWT | ✅ | Prueba en vivo en el puerto 18080: sin token → `401`, token basura → `401`, rol `USER` en ruta de admin → `403`, token válido → autentica |
| 3 — DTOs y errores | ✅ | DTOs creados; `GlobalExceptionHandler` sin handlers duplicados |
| 4 — Auth y Usuario | ✅ verificado en vivo | `BUILD SUCCESSFUL` y puerto 18080: `/api/auth/login` responde `401` con credenciales inexistentes y `400` con body inválido; ruta protegida sin token → `401` |
| 5 — Roles y admin inicial | ⏳ **bloquea el registro** | La tabla `roles` está vacía: `POST /api/usuarios/registro` devuelve `404 El rol con el nombre [CUIDADOR] no existe` |
| 6 — Paciente | ⏳ | Sigue devolviendo entidades y `actualizarPaciente` llama a `eliminarPaciente` |
| 7 — Bitácora | ⏳ | `BitacoraService` es el código viejo (NPE, `Rol.admin`, sin `setNotas`) |
| 8, 9, 10 | ⏳ | Tests, limpieza del modelo, documentación |

### D.4 Hallazgos no obvios de esta sesión

1. **86 errores eran 1 error + una cascada de Lombok.** Un `@RequestMapping` sobre un parámetro aborta el procesamiento de anotaciones antes de que Lombok genere getters. Medido: 86 errores → 7 arreglando solo esa línea → 2 excluyendo el archivo. *Regla:* ante muchos `cannot find symbol`, busca el primer error que **no** sea `cannot find symbol`.
2. **El autocompletado del IDE eligió dos clases homónimas equivocadas:** `java.awt.print.Pageable` y `org.apache.tomcat.util.net.openssl.ciphers.Authentication`. Un import mal elegido no falla donde está el import.
3. **`PREFIX = "Bearer"` producía un `off-by-one`:** `"Bearer <token>"` no empieza por `"Bearer"` + `substring` deja el espacio delante. Corregido a `"Bearer "`.
4. **`@Component` en un filtro lo registra dos veces:** en la cadena de servlets y en la de seguridad. Se desactiva el global con `FilterRegistrationBean`.
5. **Dos `@ExceptionHandler` para la misma excepción impiden arrancar** (`Ambiguous @ExceptionHandler method mapped for`). El de Spring Security debe ser `AccessDeniedException`, no la excepción propia.
6. **`AuthController` acabó como clase estática anidada dentro de `PacienteController`.** Compila, Spring la escanea y el endpoint responde (`/api/auth/login` → `401` real, no `404`). Es un problema de organización, no de funcionamiento: un archivo deja de contener lo que su nombre promete.
7. **El secreto JWT y la contraseña reales nunca llegaron a un commit:** la versión en `HEAD` del template ya tenía placeholders. No hay que reescribir historia; rotar el secreto es prevención, no urgencia.
8. **El registro no funciona antes de la Fase 5.** No es un bug de código: `crear()` busca el rol `CUIDADOR` en la tabla `roles` y la tabla está vacía. Cualquier prueba de alta de usuarios debe esperar a que existan los roles semilla.
9. **Los campos desconocidos del JSON se ignoran en silencio.** Enviar `"rolId":1,"rol":"ADMIN"` a `/registro` no escala privilegios: el record no los declara y Spring Boot desactiva `FAIL_ON_UNKNOWN_PROPERTIES`. La protección existe, pero el cliente no recibe aviso de que su campo fue descartado.

### D.5 Pendientes, en orden

1. Mover `AuthController` a `src/main/java/com/icnu/api/controlador/AuthController.java` como archivo propio.
2. Quitar el `@Valid` del parámetro en `UsuarioService.actualizar` (o añadir `@Validated` a la clase): sin eso no valida nada.
3. Fase 5: `RolService`, `RolController` y el `CommandLineRunner` del admin inicial.
4. Fase 6: `PacienteService`/`PacienteController` con DTOs; corregir `actualizarPaciente`, que hoy borra.
5. Fase 7: `BitacoraService` y `AccionService`.
6. Fases 8–10: tests, limpieza de entidades, contrato documentado.

### D.6 Cómo retomar

```powershell
# 1. Compilar (lo primero, siempre)
.\gradlew.bat compileJava --console=plain

# 2. Arrancar en un puerto de pruebas
$env:DB_PASSWORD = 'TuPasswordReal'      # comillas simples si contiene $
$env:JWT_SECRET  = 'UnSecretoDe32CaracteresOMasParaHS256'
.\gradlew.bat bootRun --args='--server.port=18080' --console=plain

# 3. Comprobar que el login existe (401, no 404)
curl.exe -s -i -X POST http://localhost:18080/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"email\":\"nadie@test.com\",\"contrasenia\":\"x\"}'
```

Entorno: MySQL 8.0.46 en WSL/Ubuntu publicando el 3306; base `icnu_db`; usuario `root`. En PowerShell, define `DB_PASSWORD` entre comillas simples. Usa `& curl.exe` con un array de argumentos (nunca un parámetro llamado `$args`).

