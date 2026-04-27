package com.payflow.coreservice.services;

import com.payflow.coreservice.dto.*;
import com.payflow.coreservice.enums.Enum_User;
import com.payflow.coreservice.exception.EmailAlreadyExistsException;
import com.payflow.coreservice.exception.UserNotFoundException;
import com.payflow.coreservice.model.User;
import com.payflow.coreservice.repository.UserRepository;
import com.payflow.coreservice.security.JwtService;
import com.payflow.coreservice.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    public AuthResponseDTO register(RegisterRequestDTO request){
        if(userRepository.findByEmail(request.getEmail()).isPresent()){
            throw new EmailAlreadyExistsException("Email já cadastrado");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDocument(request.getDocument());
        user.setDocumentType(request.getDocumentType().name());
        user.setBalance(request.getBalance() != null ? request.getBalance() : BigDecimal.ZERO);
        user.setStatus(Enum_User.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtService.generateToken(userDetails);

        return new AuthResponseDTO(
                token,
                "Bearer",
                savedUser.getUuid(),
                savedUser.getName(),
                savedUser.getEmail(),
                LocalDateTime.now().plusSeconds(86400)
        );
    }

    public AuthResponseDTO login(AuthRequestDTO request){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return new AuthResponseDTO(
                token,
                "Bearer",
                user.getUuid(),
                user.getName(),
                user.getEmail(),
                LocalDateTime.now().plusSeconds(86400)
        );
    }
}
