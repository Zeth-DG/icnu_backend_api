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

