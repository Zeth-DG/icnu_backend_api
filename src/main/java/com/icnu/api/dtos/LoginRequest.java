package com.icnu.api.dtos;

public class LoginRequest {
    private String email;
    private String contrasenia;

    public LoginRequest(String email, String contrasenia) {
        this.email = email;
        this.contrasenia = contrasenia;
    }//constructor

    public LoginRequest(){}//constructor vacío

    public String getEmail() {
        return email;
    }//getEmail

    public String getContrasenia() {
        return contrasenia;
    }//getContrasenia

    @Override
    public String toString() {
        return "LoginRequest{" +
                "email='" + email + '\'' +
                ", contrasenia='" + contrasenia + '\'' +
                '}';
    }//toString
}//class
