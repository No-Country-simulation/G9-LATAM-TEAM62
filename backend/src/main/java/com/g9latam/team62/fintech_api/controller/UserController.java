package com.g9latam.team62.fintech_api.controller;

import com.g9latam.team62.fintech_api.dto.ChangePasswordRequest;
import com.g9latam.team62.fintech_api.dto.ProfileUpdateRequest;
import com.g9latam.team62.fintech_api.dto.UserResponseDTO;
import com.g9latam.team62.fintech_api.model.User;
import com.g9latam.team62.fintech_api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.lang.NonNull;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import com.g9latam.team62.fintech_api.security.AuthorizationHelper;
import com.g9latam.team62.fintech_api.model.FinancialProfileHistory;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuarios", description = "Endpoints para la gestión de usuarios, perfiles y cambio de contraseñas")
public class UserController {

    private final UserService service;
    private final AuthorizationHelper authorizationHelper;

    public UserController(UserService service, AuthorizationHelper authorizationHelper) {
        this.service = service;
        this.authorizationHelper = authorizationHelper;
    }
    @GetMapping
    @Operation(summary = "Obtener todos los usuarios", description = "Retorna una colección que contiene únicamente al usuario autenticado actual.")
    public Collection<UserResponseDTO> findAll(Principal principal) {
        if (principal == null) {
            return java.util.List.of();
        }
        String email = principal.getName();
        if (email == null) {
            return java.util.List.of();
        }
        return service.findByEmail(email).stream()
                .map(UserResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID", description = "Retorna el usuario correspondiente al ID provisto, siempre que sea el propio usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<UserResponseDTO> findById(@PathVariable @NonNull Long id, Principal principal) {
        authorizationHelper.verifyUserOwnership(principal, id);
        return service.findById(id)
                .map(UserResponseDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario", description = "Actualiza toda la información de un usuario existente, validando la propiedad del recurso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado con éxito",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<UserResponseDTO> update(@PathVariable @NonNull Long id, @Valid @RequestBody User user, Principal principal) {
        authorizationHelper.verifyUserOwnership(principal, id);
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(UserResponseDTO.fromEntity(service.update(id, user)));
    }

    @PutMapping("/{id}/profile")
    @Operation(summary = "Actualizar perfil de usuario", description = "Actualiza campos específicos del perfil de un usuario, validando la propiedad del recurso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado con éxito",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<UserResponseDTO> updateProfile(@PathVariable @NonNull Long id,
                                               @Valid @RequestBody ProfileUpdateRequest request, Principal principal) {
        authorizationHelper.verifyUserOwnership(principal, id);
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(UserResponseDTO.fromEntity(service.updateProfile(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un usuario", description = "Elimina físicamente el usuario con el ID especificado, validando la propiedad del recurso.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario eliminado con éxito"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<Void> delete(@PathVariable @NonNull Long id, Principal principal) {
        authorizationHelper.verifyUserOwnership(principal, id);
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        service.delete(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{id}/profile-history")
    @Operation(summary = "Obtener historial de perfiles financieros del usuario",
               description = "Retorna la lista de perfiles financieros históricos del usuario, validando que pertenezca al usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial encontrado",
                    content = @Content(mediaType = "application/json", 
                    array = @ArraySchema(schema = @Schema(implementation = FinancialProfileHistory.class)))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<List<FinancialProfileHistory>> getProfileHistory(
            @PathVariable @NonNull Long id, Principal principal) {
        authorizationHelper.verifyUserOwnership(principal, id);
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<FinancialProfileHistory> history = service.findProfileHistory(id);
        return ResponseEntity.ok(history);
    }


    @PostMapping("/change-password")
    @Operation(summary = "Cambiar contraseña", description = "Permite a un usuario autenticado cambiar su contraseña actual por una nueva.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña cambiada con éxito"),
            @ApiResponse(responseCode = "400", description = "La contraseña anterior no coincide o la nueva contraseña es inválida"),
            @ApiResponse(responseCode = "401", description = "No autenticado o token inválido")
    })
    public ResponseEntity<?> changePassword(Principal principal, @Valid @RequestBody ChangePasswordRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        try {
            service.changePassword(principal.getName(), request.oldPassword(), request.newPassword());
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
