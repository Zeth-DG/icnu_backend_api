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