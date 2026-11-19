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
