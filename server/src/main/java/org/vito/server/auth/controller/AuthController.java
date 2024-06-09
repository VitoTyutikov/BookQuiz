package org.vito.server.auth.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.vito.server.auth.dto.AuthenticationRequest;
import org.vito.server.auth.dto.AuthenticationResponse;
import org.vito.server.auth.dto.TokenRequest;
import org.vito.server.auth.entity.BlackListToken;
import org.vito.server.auth.service.BlackListTokenService;
import org.vito.server.auth.service.UserService;
import org.vito.server.auth.utils.JwtUtil;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtTokenUtil;
    private final BlackListTokenService blackListTokenService;


    AuthController(AuthenticationManager authenticationManager,
                   UserService userDetailsService, JwtUtil jwtTokenUtil,
                   BlackListTokenService blackListTokenService) {
        this.authenticationManager = authenticationManager;
        this.userService = userDetailsService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.blackListTokenService = blackListTokenService;

    }


    @RequestMapping(value = "/refresh", method = {RequestMethod.POST})
    public ResponseEntity<?> refreshTokens(@RequestBody TokenRequest tokenRequest) throws Exception {
        final String refreshToken = tokenRequest.refreshToken();
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Refresh token is missing");
        }
        final String username = jwtTokenUtil.extractUsername(refreshToken);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Refresh token is missing username");
        }
        final var userDetails = userService.loadUserByUsername(username);
        if (blackListTokenService.isBlackListed(refreshToken)
                || !jwtTokenUtil.validateToken(refreshToken, userDetails, "REFRESH")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token is invalid");
        }
        var blackListToken = new BlackListToken();
        blackListToken.setToken(refreshToken);
        blackListToken.setAddedAt(LocalDateTime.now());
        blackListTokenService.save(blackListToken);
        final var tokens = jwtTokenUtil.generateTokens(userDetails);
        var response = new AuthenticationResponse(
                tokens.get("accessToken"),
                tokens.get("refreshToken"),
                userDetails.getAuthorities().toString(),
                jwtTokenUtil.extractExpiration(tokens.get("accessToken")).getTime());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @RequestMapping(value = "/authenticate", method = {RequestMethod.POST})
    public ResponseEntity<AuthenticationResponse> createAuthenticationTokens(
            @RequestBody AuthenticationRequest authenticationRequest)
            throws Exception {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    authenticationRequest.username(), authenticationRequest.password()));
        } catch (Exception e) {
            throw new Exception("Incorrect username or password", e);
        }

        final UserDetails userDetails = userService.loadUserByUsername(authenticationRequest.username());
        final Map<String, String> tokens = jwtTokenUtil.generateTokens(userDetails);

        final AuthenticationResponse response = new AuthenticationResponse(tokens.get("accessToken"),
                tokens.get("refreshToken"), userDetails.getAuthorities().toString(),
                jwtTokenUtil.extractExpiration(tokens.get("accessToken")).getTime());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @RequestMapping(value = "/afterLogout", method = {RequestMethod.POST})
    public ResponseEntity<String> logout(@RequestBody TokenRequest tokenRequest) {
        userService.logout(tokenRequest.refreshToken());
        return ResponseEntity.status(HttpStatus.OK).body("Logout successful");

    }


}
