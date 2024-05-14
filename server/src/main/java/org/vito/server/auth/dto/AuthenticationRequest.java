package org.vito.server.auth.dto;

public record AuthenticationRequest(
        String username,
        String password
) {
}
