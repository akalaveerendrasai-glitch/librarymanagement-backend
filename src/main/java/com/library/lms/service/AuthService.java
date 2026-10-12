package com.library.lms.service;

import com.library.lms.dto.request.LoginRequestDTO;
import com.library.lms.dto.request.RegisterRequestDTO;
import com.library.lms.dto.response.AuthResponseDTO;
import com.library.lms.entity.Role;
import com.library.lms.entity.User;
import com.library.lms.exception.DuplicateResourceException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.repository.UserRepository;
import com.library.lms.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Register a new member account.
     * Enforces Role.USER automatically to prevent privilege escalation.
     */
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();

        // 1. Check if email already registered
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new DuplicateResourceException("An account with email '" + cleanEmail + "' already exists");
        }

        // 2. Hash password with BCrypt
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3. Create user entity strictly with Role.USER
        User user = new User(
                request.getName().trim(),
                cleanEmail,
                encodedPassword,
                Role.USER
        );

        User savedUser = userRepository.save(user);

        // 4. Generate JWT token
        String token = tokenProvider.generateToken(
                savedUser.getEmail(),
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getRole().name()
        );

        return new AuthResponseDTO(
                token,
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }

    /**
     * Authenticate user credentials and return a signed JWT token.
     */
    public AuthResponseDTO login(LoginRequestDTO request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();

        // 1. Authenticate with Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(cleanEmail, request.getPassword())
        );

        // 2. Fetch user record
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + cleanEmail));

        // 3. Generate signed JWT token
        String token = tokenProvider.generateToken(
                user.getEmail(),
                user.getId(),
                user.getName(),
                user.getRole().name()
        );

        return new AuthResponseDTO(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    /**
     * Get details of currently authenticated user.
     */
    @Transactional(readOnly = true)
    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
