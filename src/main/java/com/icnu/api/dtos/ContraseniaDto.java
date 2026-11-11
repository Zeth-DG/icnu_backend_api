package com.icnu.api.dtos;

import lombok.Data;
import lombok.RequiredArgsConstructor;

public class ContraseniaDto {
    private String contraseniaActual;
    private String contraseniaNueva;

    public ContraseniaDto(String contraseniaActual, String contraseniaNueva) {
        this.contraseniaActual = contraseniaActual;
        this.contraseniaNueva = contraseniaNueva;
    }//constructor

    public String getContraseniaActual() {
        return contraseniaActual;
    }//getContraseniaActual

    public String getContraseniaNueva() {
        return contraseniaNueva;
    }//getContraseniaNueva

}//ContraseniaDto
