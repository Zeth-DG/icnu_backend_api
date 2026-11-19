package com.icnu.api.dtos;

public record LoginResponse (
        String token, Long id, String nombre,String rol) {
}//record
