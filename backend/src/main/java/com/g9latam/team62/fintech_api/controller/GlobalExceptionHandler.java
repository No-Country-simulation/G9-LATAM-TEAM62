package com.g9latam.team62.fintech_api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Maneja argumentos inválidos en la lógica de negocio (400 Bad Request)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidReference(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    // 2. Maneja estados inválidos o conflictos de negocio (409 Conflict)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }

    // 3. Maneja errores de validación de campos (@Valid en DTOs) devolviendo un mapa limpio {"campo": "error"}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage != null ? errorMessage : "Campo inválido");
        });
        return ResponseEntity.badRequest().body(errors);
    }

    // 4. Maneja JSONs malformados enviados por el cliente (400 Bad Request)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMalformedJson(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", "El cuerpo de la petición JSON está malformado o es ilegible"));
    }

    // 5. Maneja tipos de datos incorrectos en parámetros/path (ej: mandar texto en lugar de un ID numérico)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        Class<?> requiredType = exception.getRequiredType();
        String typeName = (requiredType != null) ? requiredType.getSimpleName() : "desconocido";
        String message = String.format("El parámetro '%s' debe ser de tipo '%s'", 
                exception.getName(), 
                typeName);            
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    // 6. Maneja métodos HTTP no soportados (ej: intentar un POST en un endpoint GET)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        String message = String.format("El método HTTP '%s' no está soportado para este endpoint", exception.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Map.of("error", message));
    }

    // 7. Capturador genérico para cualquier otro error no controlado (evita filtraciones y fugas de información)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAllExceptions(Exception exception) {
        // Registramos el error completo en el servidor para que los desarrolladores puedan depurar
        log.error("Excepción no controlada en el servidor: ", exception);

        // Devolvemos un mensaje genérico y seguro al cliente
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ha ocurrido un error inesperado en el servidor. Intenta más tarde."));
    }
}
