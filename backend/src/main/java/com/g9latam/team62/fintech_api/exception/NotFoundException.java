package com.g9latam.team62.fintech_api.exception;

/**
 * El recurso identificado en la petición no existe.
 *
 * <p>Existe para separar "no encontré esto" de "lo que enviaste está mal": ambos casos
 * viajaban como {@link IllegalArgumentException} y {@link
 * com.g9latam.team62.fintech_api.controller.GlobalExceptionHandler} los traducía a 400, de
 * modo que pedir un usuario inexistente respondía "petición inválida" en vez de 404.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
