package com.payflow.coreservice.services;

import com.payflow.commons.dto.user.UserRequest;
import com.payflow.commons.dto.user.UserResponse;
import com.payflow.coreservice.model.User;
import com.payflow.coreservice.model.factory.UserFactory;
import com.payflow.coreservice.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {
    /*
     * ATENÇÃO: Este serviço é para operações administrativas em usuários.
     * Para registro/login de usuários finais, use AuthService.
     * AuthService: Registro com criptografia de senha + geração de JWT
     * UserService: Operações CRUD administrativas (sem criptografia)
     */

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // READ (por id)
    public UserResponse readUserById(UUID id) {
        User user = userRepository.findByUuid(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        return toResponse(user);
    }

    // UPDATE
    public UserResponse updateUser(UUID id, UserRequest dto) {
        User user = userRepository.findByUuid(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        String encodedPassword = passwordEncoder.encode(dto.password());

        user = UserFactory.fromUpdateRequest(user, dto, encodedPassword );

        User updatedUser = userRepository.save(user);

        return toResponse(updatedUser);
    }

    // DELETE
    public void delete(UUID id) {
        User user = findUserEntityById(id);
        userRepository.delete(user);
    }

    private User findUserEntityById(UUID id) {
        return userRepository.findByUuid(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getUuid())
                .name(user.getName())
                .email(user.getEmail())
                .balance(user.getBalance())
                .status(user.getStatus())
                .document(user.getDocument())
                .documentType(user.getDocumentType())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public UserResponse updateBalance(UUID id, BigDecimal amount) {

        User user = findUserEntityById(id);

        user.setBalance(user.getBalance().add(amount));

        return toResponse(userRepository.save(user));
    }

}
