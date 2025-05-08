package com.bitzomax.controller;

import com.bitzomax.dto.AuthRequest;
import com.bitzomax.dto.AuthResponse;
import com.bitzomax.dto.SignupRequest;
import com.bitzomax.entity.User;
import com.bitzomax.security.JwtTokenProvider;
import com.bitzomax.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody AuthRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        
        // Find the user to get additional details
        User user = userService.findByUsername(loginRequest.getUsernameOrEmail())
                .orElseGet(() -> userService.findByEmail(loginRequest.getUsernameOrEmail()).orElse(null));
        
        if (user != null) {
            return ResponseEntity.ok(new AuthResponse(
                jwt, 
                user.getId(), 
                user.getUsername(), 
                user.getEmail(), 
                user.getRole().name(),
                user.isSubscribed()
            ));
        } else {
            return ResponseEntity.badRequest().body("User not found");
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        // Check if username is already taken
        if (userService.existsByUsername(signUpRequest.getUsername())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Username is already taken");
            return ResponseEntity.badRequest().body(error);
        }

        // Check if email is already in use
        if (userService.existsByEmail(signUpRequest.getEmail())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Email is already in use");
            return ResponseEntity.badRequest().body(error);
        }

        // Create user
        User user = userService.createUser(
                signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                signUpRequest.getPassword(),
                signUpRequest.getFullName()
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/api/users/{username}")
                .buildAndExpand(user.getUsername()).toUri();

        Map<String, String> response = new HashMap<>();
        response.put("success", "User registered successfully");
        response.put("username", user.getUsername());

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/check-auth")
    public ResponseEntity<?> checkAuthentication() {
        return ResponseEntity.ok().build();
    }
}