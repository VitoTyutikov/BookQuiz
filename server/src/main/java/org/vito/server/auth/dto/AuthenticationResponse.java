package org.vito.server.auth.dto;

public record AuthenticationResponse(
        String access,
        String refresh,
        String roles,
        Long expiration
) {
}
