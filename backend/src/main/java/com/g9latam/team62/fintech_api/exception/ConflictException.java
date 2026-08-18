package com.g9latam.team62.fintech_api.exception;

/**
 * La petición es válida pero choca con el estado actual del sistema: un correo ya registrado,
 * un recurso duplicado. Se traduce a 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
