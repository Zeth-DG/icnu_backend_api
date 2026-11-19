package com.icnu.api.excepciones;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos.");
    }//mensaje
}//excepcion
