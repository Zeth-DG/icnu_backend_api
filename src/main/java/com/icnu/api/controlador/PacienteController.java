package com.icnu.api.controlador;

import com.icnu.api.entidad.Paciente;
import com.icnu.api.servicio.PacienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
public class PacienteController {
    //llamar al service
    private final PacienteService pacienteService;

    //endpoints de lectura
    @PreAuthorize("hasAnyAuthority('ADMIN', 'CUIDADOR')")
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Paciente>> obtenerPacientesDelUsuario(@PathVariable Long usuarioId) {
        return  ResponseEntity.ok(pacienteService.obtenerPacientesDeUsuario(usuarioId));
    }//obtener pacientes del usuario

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CUIDADOR')")
    @GetMapping("/{id}")
    public ResponseEntity<Paciente> obtenerPacientePorId(@PathVariable Long id) {
        return  ResponseEntity.ok(pacienteService.obtenerPacientePorId(id));
    }//obtener paciente por id

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CUIDADOR')")
    @PostMapping("/usuario/{usuarioId}")
    public ResponseEntity<Paciente> agregarPaciente(@RequestBody Paciente paciente, @PathVariable Long usuarioId) {
        Paciente nuevoPaciente = pacienteService.agregarPaciente(paciente, usuarioId);
        return ResponseEntity.ok(nuevoPaciente);
    }//agregar paciente

    @PreAuthorize("hasAuthority('ADMIN')") // Solo el Admin puede editar
    @PutMapping("/{id}")
    public ResponseEntity<Paciente> actualizarPaciente(@PathVariable Long id) {
        pacienteService.eliminarPaciente(id);;
        return  ResponseEntity.noContent().build();
    }//actualizarPaciente

}//class PacienteController
