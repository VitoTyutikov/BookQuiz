package org.vito.server.auth.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.vito.server.auth.dto.UserDto;
import org.vito.server.auth.entity.User;
import org.vito.server.auth.service.UserService;

import java.util.List;
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public String getCurrentUserLogin() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @RequestMapping(value = "/user/me", method = RequestMethod.GET)
    public ResponseEntity<User> getCurrentUser() {
        String login = getCurrentUserLogin();
        return ResponseEntity.status(HttpStatus.OK).body(userService.findByUsername(login));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/all", method = RequestMethod.GET)
    public ResponseEntity<List<User>> findAll() {
        return ResponseEntity.status(HttpStatus.OK).body(userService.findAll());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/user/{id}", method = RequestMethod.GET)
    public ResponseEntity<User> findById(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.findById(id));
    }

    @RequestMapping(value = "/user/register", method = RequestMethod.POST)
    public ResponseEntity<User> registerNewUser(@RequestBody UserDto userDto) {
        User registeredUser = userService.registerNewUser(userDto);
        if (registeredUser == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerNewUser(userDto));
    }

    @RequestMapping(value = "/user/update", method = RequestMethod.PUT)
    public ResponseEntity<User> update(@RequestBody UserDto user) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.update(user));
    }

/*    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/updateNonLocked", method = RequestMethod.PUT)
    public ResponseEntity<User> updateNonBlocked(@RequestBody ChangeUserNonBlockDTO user) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.updateNonLocked(user));
    }*/

/*    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/updateRole", method = RequestMethod.PUT)
    public ResponseEntity<User> updateRole(@RequestBody ChangeRoleDTO user) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.updateRole(user));
    }*/

}
