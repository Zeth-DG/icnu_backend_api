package com.icnu.api.controlador;

import com.icnu.api.dtos.RolRequestDto;
import com.icnu.api.dtos.RolResponseDto;
import com.icnu.api.servicio.RolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolController {
    private final RolService rolService;

    @GetMapping
    public ResponseEntity<List<RolResponseDto>> listar() {
        return ResponseEntity.ok(rolService.listar());
    }//listar

    @GetMapping("/{id}")
    public ResponseEntity<RolResponseDto> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(rolService.obtener(id));
    }//obtener

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public ResponseEntity<RolResponseDto> crear(@Valid @RequestBody RolRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rolService.crear(dto));
    }//crear

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        rolService.eliminar(id);
        return ResponseEntity.noContent().build();
    }//eliminar

}//class
