package com.icnu.api.controlador;

import com.icnu.api.dtos.*;
import com.icnu.api.servicio.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponseDto> registrar(@Valid @RequestBody RegistroRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.registrar(dto));
    }//registrar

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping
    public ResponseEntity<PageResponse<UsuarioResponseDto>> listar(
            @PageableDefault(size=20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(usuarioService.listar(pageable)));
    }//listar

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDto> obtener(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(
                usuarioService.obtener(id, authentication.getName()));
    }//obtener

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public ResponseEntity<UsuarioResponseDto> crear(@Valid @RequestBody UsuarioRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.crearComoAdmin(dto));
    }//crear

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateDto dto,
            Authentication authentication) {

        return ResponseEntity.ok(
                usuarioService.actualizar(id, dto, authentication.getName()));
    }//actualizar

    @PutMapping("/{id}/contrasenia")
    public ResponseEntity<Void> cambiarContrasenia(
            @PathVariable Long id,
            @Valid @RequestBody ContraseniaDto dto,
            Authentication authentication) {

        usuarioService.cambiarContrasenia(id, dto, authentication.getName());
        return ResponseEntity.noContent().build();
    }//cambiar contraseña

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }//eliminar

}//class
