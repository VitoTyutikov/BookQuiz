package org.vito.server.auth.dto;

public record UserDto(
        String username,
        String firstName,
        String lastName,
        String email,
        String password

) {
}