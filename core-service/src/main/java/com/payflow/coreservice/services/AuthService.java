package com.payflow.coreservice.services;

import com.payflow.coreservice.dto.*;
import com.payflow.coreservice.enums.Enum_User;
import com.payflow.coreservice.exception.DocumentAlreadyExistsException;
import com.payflow.coreservice.exception.EmailAlreadyExistsException;
import com.payflow.coreservice.exception.UserNotFoundException;
import com.payflow.coreservice.model.User;
import com.payflow.coreservice.model.factory.UserFactory;
import com.payflow.coreservice.repository.UserRepository;
import com.payflow.coreservice.security.JwtService;
import com.payflow.coreservice.security.UserDetailsServiceImpl;
import com.payflow.coreservice.dto.factory.AuthResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service class for authentication-related operations.
 */
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

    /**
     * Registers a new user.
     *
     * @param request the registration request
     * @return the authentication response
     */
    public AuthResponseDTO register(RegisterRequestDTO request){
        if(userRepository.findByEmail(request.getEmail()).isPresent()){
            throw new EmailAlreadyExistsException("Email já cadastrado");
        }

        if (userRepository.findByDocument(request.getDocument()).isPresent()) {
            throw new DocumentAlreadyExistsException("Documento já cadastrado");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = UserFactory.fromRegisterRequest(request, encodedPassword);

        User savedUser = userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtService.generateToken(userDetails);

        return AuthResponseFactory.fromUserAndToken(savedUser, token, 86400);
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

        return AuthResponseFactory.fromUserAndToken(user, token, 86400);
    }
}
