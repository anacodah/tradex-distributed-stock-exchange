package com.tradex.gateway.service;

import com.tradex.gateway.dto.AuthResponse;
import com.tradex.gateway.dto.LoginRequest;
import com.tradex.gateway.dto.RegisterRequest;
import com.tradex.gateway.entity.Role;
import com.tradex.gateway.entity.User;
import com.tradex.gateway.repository.RoleRepository;
import com.tradex.gateway.repository.UserRepository;
import com.tradex.gateway.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@test.com");
        request.setPassword("password");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        
        Role role = new Role();
        role.setName("ROLE_USER");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        
        when(passwordEncoder.encode("password")).thenReturn("hashedpassword");
        when(jwtService.generateToken(any())).thenReturn("fake-jwt-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("fake-jwt-token", response.getToken());
        assertEquals("testuser", response.getUser().getUsername());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterDuplicateUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(request));
        assertEquals("Username is already taken", exception.getMessage());
    }

    @Test
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@test.com");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(request));
        assertEquals("Email is already in use", exception.getMessage());
    }

    @Test
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("hashedpassword");
        Role role = new Role();
        role.setName("ROLE_USER");
        user.getRoles().add(role);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("fake-jwt-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("fake-jwt-token", response.getToken());
        assertEquals("testuser", response.getUser().getUsername());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void testLoginInvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
            .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}
