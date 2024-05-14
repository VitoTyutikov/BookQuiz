package org.vito.server.auth.config;


import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.vito.server.auth.service.UserService;

import java.util.Arrays;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration {
    @Autowired
    @Lazy
    private UserService userService;
    @Autowired
    private Filter jwtRequestFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf((csrf) -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests((authz) -> authz
                        .requestMatchers("/api/v1/auth/authenticate", "/api/v1/auth/refresh", "/api/v1/users/user/register", "/logout", "/api/v1/auth/afterLogout")
                        .permitAll()
//                        .requestMatchers("/api/v1/books/**")
//                        .permitAll()
                        .anyRequest().authenticated())
                .logout((logout) -> logout
                        .logoutSuccessHandler((httpServletRequest, httpServletResponse, authentication) -> {
                            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                        }))
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        AuthenticationManagerBuilder authManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authManagerBuilder.userDetailsService(userService)
                .passwordEncoder(passwordEncoder());

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(Arrays.asList("*"));
        corsConfiguration.setAllowedMethods(Arrays.asList("*"));
        corsConfiguration.setAllowedHeaders(Arrays.asList("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}
