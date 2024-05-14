package org.vito.server.auth.dto;

public record TokenRequest(
        String refreshToken
) {
}
