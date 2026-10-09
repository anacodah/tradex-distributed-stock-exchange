package com.tradex.gateway.service;

import com.tradex.common.entity.UserProfile;
import com.tradex.common.repository.UserProfileRepository;
import com.tradex.gateway.dto.AuthResponse;
import com.tradex.gateway.dto.LoginRequest;
import com.tradex.gateway.dto.RegisterRequest;
import com.tradex.gateway.dto.UserDto;
import com.tradex.gateway.entity.Role;
import com.tradex.gateway.entity.User;
import com.tradex.gateway.repository.RoleRepository;
import com.tradex.gateway.repository.UserRepository;
import com.tradex.gateway.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserProfileRepository userProfileRepository;
    private final WalletService walletService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserProfileRepository userProfileRepository,
                       WalletService walletService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userProfileRepository = userProfileRepository;
        this.walletService = walletService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Role not found"));
        user.getRoles().add(userRole);

        user = userRepository.save(user);

        // Create User Profile
        UserProfile profile = new UserProfile(user.getId(), request.getUsername(), "");
        userProfileRepository.save(profile);

        // Initialize Virtual Wallet with default $10,000.00
        walletService.getOrCreateWallet(user.getUsername());

        // Authenticate the user to generate token
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String jwt = jwtService.generateToken(new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(),
                user.getRoles().stream()
                        .map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority(r.getName()))
                        .collect(Collectors.toSet())
        ));

        return new AuthResponse(jwt, mapToDto(user));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        String jwt = jwtService.generateToken(new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(),
                user.getRoles().stream()
                        .map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority(r.getName()))
                        .collect(Collectors.toSet())
        ));
        return new AuthResponse(jwt, mapToDto(user));
    }

    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return mapToDto(user);
    }

    private UserDto mapToDto(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
        return new UserDto(user.getId(), user.getUsername(), user.getEmail(), roles);
    }
}
