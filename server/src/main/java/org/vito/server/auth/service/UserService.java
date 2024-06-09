package org.vito.server.auth.service;


import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.vito.server.auth.dto.UserDto;
import org.vito.server.auth.entity.BlackListToken;
import org.vito.server.auth.entity.User;
import org.vito.server.auth.repo.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final BlackListTokenService blackListTokenService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       BlackListTokenService blackListTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.blackListTokenService = blackListTokenService;
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Transactional
    public User registerNewUser(@RequestBody UserDto userDto) {

        try {
            if (userRepository.findByEmail(userDto.email()).isPresent()
                    || userRepository.findByUsername(userDto.username()).isPresent()) {
                throw new RuntimeException("Email or username already registered");
            }
        } catch (Exception e) {
            return null;
        }

        User user = new User();
        user.setUsername(userDto.username());
        user.setFirstName(userDto.firstName());
        user.setLastName(userDto.lastName());
        user.setEmail(userDto.email());
        user.setPassword(passwordEncoder.encode(userDto.password()));
        user.setDateJoined(LocalDate.now());

        userRepository.save(user);
        return user;
    }

    @Transactional
    public void delete(User user) {

        userRepository.delete(user);
    }

    @Transactional
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public void deleteAll() {
        userRepository.deleteAll();
    }

    @Transactional
    public void deleteByEmail(String email) {
        userRepository.deleteByEmail(email);
    }

    @Transactional
    public void deleteByUsername(String username) {
        userRepository.deleteByUsername(username);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    @Transactional
    public User update(UserDto user) {
        try {
            User userEntity = userRepository.findByUsername(user.username())
                    .orElseThrow(
                            () -> new UsernameNotFoundException("User not found with username: " + user.username()));
            userEntity.setFirstName(user.firstName());
            userEntity.setLastName(user.lastName());
            userEntity.setEmail(user.email());
            save(userEntity);
            return userEntity;
        } catch (Exception e) {
            return null;
        }

    }

    public void logout(String refreshToken) {
        BlackListToken blackListToken = new BlackListToken();
        blackListToken.setToken(refreshToken);
        blackListToken.setAddedAt(LocalDateTime.now());
        blackListTokenService.save(blackListToken);

    }


}
