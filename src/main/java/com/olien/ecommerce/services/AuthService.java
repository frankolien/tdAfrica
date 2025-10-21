package com.olien.ecommerce.services;

import com.olien.ecommerce.dtos.AuthResponse;
import com.olien.ecommerce.dtos.LoginRequest;
import com.olien.ecommerce.dtos.RegisterRequest;
import com.olien.ecommerce.entities.Role;
import com.olien.ecommerce.entities.User;
import com.olien.ecommerce.repositories.RoleRepository;
import com.olien.ecommerce.repositories.UserRepository;
import com.olien.ecommerce.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthService {
    
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       RoleRepository roleRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }
    
    public AuthResponse register(RegisterRequest registerRequest) {
        log.info("Registering new user with email: {}", registerRequest.getEmail());
        
        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }
        
        // Check if phone number already exists (if provided)
        if (registerRequest.getPhoneNumber() != null && 
            userRepository.existsByPhoneNumber(registerRequest.getPhoneNumber())) {
            throw new RuntimeException("Phone number is already in use!");
        }
        
        // Create new user
        User user = new User();
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setEmail(registerRequest.getEmail());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEnabled(true);
        user.setRoles(new HashSet<>());
        
        // Assign default role
        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        user.addRole(userRole);
        
        // Save user
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());
        
        // Generate JWT token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        registerRequest.getEmail(),
                        registerRequest.getPassword()
                )
        );
        
        String jwt = jwtUtil.generateToken(authentication);
        
        // Build response
        Set<String> roles = savedUser.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
        
        return new AuthResponse(jwt, savedUser.getId(), savedUser.getFirstName(), 
                               savedUser.getLastName(), savedUser.getEmail(), 
                               savedUser.getPhoneNumber(), roles, savedUser.getCreatedAt());
    }
    
    public AuthResponse login(LoginRequest loginRequest) {
        log.info("User attempting to login with email: {}", loginRequest.getEmail());
        
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Generate JWT token
        String jwt = jwtUtil.generateToken(authentication);
        
        // Get user details
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Build response
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
        
        log.info("User logged in successfully with email: {}", loginRequest.getEmail());
        
        return new AuthResponse(jwt, user.getId(), user.getFirstName(), 
                               user.getLastName(), user.getEmail(), 
                               user.getPhoneNumber(), roles, user.getCreatedAt());
    }
    
    @Transactional
    public void initializeRoles() {
        log.info("Initializing default roles...");
        
        if (!roleRepository.existsByName(Role.RoleName.ROLE_USER)) {
            Role userRole = new Role(Role.RoleName.ROLE_USER, "Default role for all users");
            roleRepository.save(userRole);
            log.info("Created ROLE_USER");
        }
        
        if (!roleRepository.existsByName(Role.RoleName.ROLE_ADMIN)) {
            Role adminRole = new Role(Role.RoleName.ROLE_ADMIN, "Administrator role with full access");
            roleRepository.save(adminRole);
            log.info("Created ROLE_ADMIN");
        }
        
        if (!roleRepository.existsByName(Role.RoleName.ROLE_MODERATOR)) {
            Role moderatorRole = new Role(Role.RoleName.ROLE_MODERATOR, "Moderator role with limited admin access");
            roleRepository.save(moderatorRole);
            log.info("Created ROLE_MODERATOR");
        }
        
        log.info("Role initialization completed");
    }
}