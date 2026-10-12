package com.library.lms.service;

import com.library.lms.dto.request.LoginRequestDTO;
import com.library.lms.dto.request.RegisterRequestDTO;
import com.library.lms.dto.response.AuthResponseDTO;
import com.library.lms.entity.Role;
import com.library.lms.entity.User;
import com.library.lms.exception.DuplicateResourceException;
import com.library.lms.repository.UserRepository;
import com.library.lms.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(10L);
        sampleUser.setName("Alice Cooper");
        sampleUser.setEmail("alice@example.com");
        sampleUser.setPassword("encodedHashedSecret");
        sampleUser.setRole(Role.USER);
    }

    @Test
    @DisplayName("Registration: Should successfully register a user with hashed password and Role.USER")
    void testRegister_Success() {
        RegisterRequestDTO request = new RegisterRequestDTO("Alice Cooper", "alice@example.com", "Secret@123");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret@123")).thenReturn("encodedHashedSecret");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(tokenProvider.generateToken(anyString(), anyLong(), anyString(), anyString())).thenReturn("mocked.jwt.token");

        AuthResponseDTO response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mocked.jwt.token");
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");

        verify(passwordEncoder, times(1)).encode("Secret@123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Registration: Should throw DuplicateResourceException when email already registered")
    void testRegister_DuplicateEmail_ThrowsConflict() {
        RegisterRequestDTO request = new RegisterRequestDTO("Alice Cooper", "alice@example.com", "Secret@123");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Login: Should successfully authenticate and return signed JWT token")
    void testLogin_Success() {
        LoginRequestDTO request = new LoginRequestDTO("alice@example.com", "Secret@123");
        Authentication authMock = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authMock);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(tokenProvider.generateToken("alice@example.com", 10L, "Alice Cooper", "USER")).thenReturn("valid.jwt.token");

        AuthResponseDTO response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("valid.jwt.token");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("Login: Should fail when bad credentials are provided")
    void testLogin_BadCredentials_ThrowsException() {
        LoginRequestDTO request = new LoginRequestDTO("alice@example.com", "WrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Bad credentials");

        verify(userRepository, never()).findByEmail(anyString());
    }
}
