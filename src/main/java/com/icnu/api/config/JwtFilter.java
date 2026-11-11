package com.icnu.api.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.filter.GenericFilterBean;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JwtFilter extends GenericFilterBean {
    public static String signature = "¡4qui-3st4-l4-d3finic1on-d3-l4-Clav3-S3cr3t4-PArA-35t4-ap1!";

    public static final String secret = Base64
            .getEncoder()
            .encodeToString(signature.getBytes(StandardCharsets.UTF_8));

    public static SecretKey getSignInKey(){
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }// getSignInKey

    /*
     * ServletRequest: Se encarga de interceptar la solicitud del usuario y recuperar datos sobre la misma, ejemplo:
     * encabezados, ruta destino de la solicitud (endpoint).
     *
     * ServletResponse: Salida. Proporciona la respuesta a la solicitud previamente mencionada. Valida si el usuario tiene permisos para
     * continuar con la solicitud (token de acceso).
     *
     * FilterChain: Secuencia ordenada de filtros por los que pasa la solicitud antes de llegar al controlador.
     * */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        if( requiereAutenticacion(httpRequest)){//intercepta la solicitud, lee metodo y ruta de destino y valida si necesita token o no
            String token = obtenerToken(httpRequest);
            validarToken(token);
        }//if

        filterChain.doFilter(servletRequest, servletResponse);
    }//doFilter

    private boolean requiereAutenticacion(HttpServletRequest httpRequest) {
        String metodo = httpRequest.getMethod();
        String URI = httpRequest.getRequestURI();

        // Las rutas PÚBLICAS (No requieren token)
        // El login es público porque el usuario aún no tiene token
        if (metodo.equals("POST") && URI.contains("/api/auth/login")) {
            return false;
        }//if
        // para que cualquiera pueda registrarse
        if (metodo.equals("POST") && URI.contains("/api/usuarios/registro")) {
            return false;
        }//if

        // lo demás requiere autenticación (GET, POST, PUT, DELETE)
        return true;
    }// requiere autenticación

    private String obtenerToken(HttpServletRequest httpRequest) throws ServletException{
        String authHeader = httpRequest.getHeader("Authorization");
        if(authHeader == null || !authHeader.contains("Bearer ")){
            throw new ServletException("1. Token inválido. Encabezado Authorization nulo o no contiene Bearer token");
        }//if
        return authHeader.substring(7); //recorta la palabra bearer + espacio
    }// obtenerToken

    /*
     * Jwts: Clase de jjwt que permite crear y leer tokens jwt
     *
     * parser(): inicializa los componentes que permiten leer y desencriptar la info de cada token
     * verifyWith(): indica que se validará con la firma secreta la autenticidad del token
     * build(): construye el parser con la configuración anterior
     * parseSignedClaims(token): descifra y verifica la firma del token, si es válido, continúa
     * getPayload(): recupera el cuerpo del token, que tiene la info del usuario, momento en que se creó el token
     * para validar si no ha expirado, etc.
     * */

    private void validarToken(String token) throws ServletException {
        try{
            Claims claims = Jwts
                    .parser() //toma el jwt y lo descifra
                    .verifyWith(getSignInKey()) //verifica que los datos sean correctos coincida con el secret
                    .build() //construye el lector de jwt
                    .parseSignedClaims(token)//toma el token y lo descifra
                    .getPayload(); //obtiene la informacion del token
            System.out.println(claims.getSubject());
        } catch(SignatureException | MalformedJwtException | ExpiredJwtException e){
            throw new ServletException("2. Token inválido. Verifica que contenga la firma correcta, que no haya sido manipulado o que no este expirado");
        }//catch
    }// validarToken

}//class JwtFilter
