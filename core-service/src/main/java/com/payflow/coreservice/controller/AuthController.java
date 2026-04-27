package com.payflow.coreservice.controller;

import com.payflow.coreservice.dto.AuthRequestDTO;
import com.payflow.coreservice.dto.AuthResponseDTO;
import com.payflow.coreservice.dto.RegisterRequestDTO;
import com.payflow.coreservice.services.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;


    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request){
        logger.info(
                "Registering user name={} email={} document={} documentType={} balance={}",
                request.getName(),
                request.getEmail(),
                request.getDocument(),
                request.getDocumentType(),
                request.getBalance()
        );
        AuthResponseDTO response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request){
        AuthResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
