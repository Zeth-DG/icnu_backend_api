# ICNU - Backend API (en desarrollo activo)

> **El cuidado nos conecta.** 
> API REST para la plataforma ICNU, diseñada para coordinar el cuidado de adultos mayores. Este backend nació como MVP para el Hackathon "Build for Her 2026" y está siendo reconstruido desde cero con **Java y Spring Boot** para convertirlo en un producto real.

## 🛠️ Stack Tecnológico Actual
*   **Java 21** + **Spring Boot 4.1.2**
*   **Gradle** (Gestor de dependencias)
*   **MySQL 8** (Base de datos relacional)
*   **Spring Data JPA / Hibernate** (ORM)
*   **Spring Security + JJWT** (Autenticación y Autorización)
*   **Lombok** (Reducción de código boilerplate)

---

## ¿Qué está implementado hasta ahora? (Done)

He completado la base del backend siguiendo una arquitectura en capas:

### 1. Capa de Entidades (`/entidad`)
*   `Usuario`: Relación N:M con Pacientes. Campos de autenticación (email, contraseña encriptada).
*   `Paciente`: Datos médicos y personales.
*   `Rol`: Catálogo (Admin, Cuidador Principal, Red de Apoyo). Relación 1:N con Usuario.
*   `Accion`: Catálogo de tipos de eventos para la bitácora.
*   `Bitacora`: Entidad central que conecta un `Usuario`, un `Paciente` y una `Accion` (Relaciones 1:N).

### 2. Capa de repositorios (`/repositorio`)
*   Interfaces extendiendo `JpaRepository`.
*   Consultas personalizadas implementadas (ej. `findByEmail` en Usuario, `findByNombre` en Rol y Accion).

### 3. Capa de servicios (`/servicio`)
*   `UsuarioService`: CRUD básico, validación de email duplicado y encriptación de contraseñas con `BCryptPasswordEncoder`.
*   `RolService` y `AccionService`: CRUD y validación de nombres duplicados.
*   `PacienteService`: Creación de pacientes y obtención de la lista de pacientes asignados a un usuario (`obtenerPacientesDeUsuario`).

---

## ¿Qué FALTA implementar? (To-Do)

Esta es la lista de tareas pendientes para tener la API funcional:

### 🔴 Prioridad Alta: Controladores y Endpoints REST
*   [ ] **`AuthController`**: Crear el endpoint de `POST /api/auth/login`. Debe recibir email/contraseña, validarlos usando `UsuarioService` y generar un Token JWT.
*   [ ] **`UsuarioController`**: Endpoints para crear usuarios (registro), listar usuarios y actualizar contraseñas.
*   [ ] **`PacienteController`**: Endpoints para crear pacientes y asignar cuidadores a un paciente (`POST /api/pacientes/{id}/asignar`).
*   [ ] **`BitacoraController`**: Endpoints para que los cuidadores registren notas (usando el ID del usuario extraído del token) y lean el historial de un paciente.

### 🟡 Prioridad Media: Seguridad y Configuración
*   [ ] **Ajustar el `JwtFilter`**: Actualmente tiene lógica ajustado de otro proyecto.
*   [ ] **Implementar `UserDetailsService`**: Para que Spring Security pueda buscar al usuario en la base de datos durante el login.
*   [ ] **Autorización basada en Roles (RBAC)**: Usar anotaciones `@PreAuthorize` en los controladores para que, por ejemplo, solo el `ADMIN` pueda borrar pacientes, o el `CUIDADOR_SECUNDARIO` solo pueda agregar bitácoras pero no editar perfiles.

### 🟢 Prioridad Baja (Mejoras a futuro)
*   [ ] **Manejo Global de Excepciones (`@ControllerAdvice`)**: Para que los errores de validación (como "El email ya existe") devuelvan respuestas HTTP 400 amigables en JSON, en lugar de un error 500 crudo.
*   [ ] **Data Seeding**: Crear un archivo `data.sql` o un `CommandLineRunner` para que las tablas de `Rol` y `Accion` se llenen automáticamente al arrancar la app por primera vez.
*   [ ] **Diseño de DTOs (Data Transfer Objects)**: Para no exponer las contraseñas ni las entidades completas en las respuestas JSON de la API.

---

## ⚙️ Configuración para desarrollo Local

1. Clonar el repositorio y abrir en IntelliJ IDEA.
2. En `src/main/resources/`, copia el archivo `application.properties.example` y renómbralo a `application.properties`.
3. Configura tus credenciales locales de MySQL:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/icnu_db
   spring.datasource.username=root
   spring.datasource.password=tu_contraseña
   ```
4. Ejecutar el proyecto. Hibernate creará las tablas automáticamente.


---

## DTOs de Bitácora

Un DTO (*Data Transfer Object*) define los datos que la API recibe o devuelve. Para Bitácora, se recomienda separar el DTO de entrada (`Request`) del DTO de salida (`Response`), porque el frontend no necesita enviar y recibir exactamente los mismos datos.

### Request DTO: datos que recibe la API

`BitacoraRequestDTO` representa los datos necesarios para crear una bitácora. Recibe los IDs de las entidades relacionadas, no los objetos completos `Usuario`, `Paciente` y `Accion`. No incluye el ID de Bitácora porque este se genera automáticamente al guardar el registro.


`BitacoraResponsetDTO` representa los datos que la API devuelve después de guardar la bitácora. Puede incluir el ID generado y solo la información necesaria para el frontend.

### ¿Por qué separar Request y Response?

- El cliente no envía el ID autoincremental; la base de datos lo genera.
- La API controla qué datos acepta y cuáles devuelve.
- Se evita exponer directamente la entidad JPA y todas sus relaciones.
- Se pueden cambiar los datos de entrada y salida sin modificar la entidad ni la base de datos.
- Se reducen problemas de serialización, como relaciones cíclicas o respuestas demasiado grandes.

`notas` y `accionId` pueden ser nulos si esos campos son opcionales. El proyecto utiliza Java 21, por lo que puede usar `record` para declarar DTOs de forma concisa. Sus componentes no tienen setters; se accede a ellos con métodos como `dto.fecha()` y `dto.usuarioId()`.