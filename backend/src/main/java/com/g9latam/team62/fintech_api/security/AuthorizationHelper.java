package com.g9latam.team62.fintech_api.security;

import com.g9latam.team62.fintech_api.model.User;
import com.g9latam.team62.fintech_api.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class AuthorizationHelper {

    private final UserService userService;

    public AuthorizationHelper(UserService userService) {
        this.userService = userService;
    }

    public User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Usuario no autenticado");
        }
        String email = principal.getName();
        if (email == null) {
            throw new AccessDeniedException("Nombre de usuario inválido");
        }
        return userService.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Usuario no encontrado en el sistema"));
    }

    public void verifyUserOwnership(Principal principal, Long userId) {
        User user = getAuthenticatedUser(principal);
        if (!user.getId().equals(userId)) {
            throw new AccessDeniedException("No tienes permiso para acceder o modificar los recursos de otro usuario");
        }
    }
}
