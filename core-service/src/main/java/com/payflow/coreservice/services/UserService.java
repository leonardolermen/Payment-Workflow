package com.payflow.coreservice.services;

import com.payflow.coreservice.dto.UserRequestDTO;
import com.payflow.coreservice.dto.UserResponseDTO;
import com.payflow.coreservice.model.User;
import com.payflow.coreservice.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    //CREATE
    public UserResponseDTO createUser(UserRequestDTO dto) {
        User user = toEntity(dto);
        User savedUser = userRepository.save(user);
        return toResponseDTO(savedUser);
    }

    // READ (por id)
    public UserResponseDTO readUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        return toResponseDTO(user);
    }

    // UPDATE
    public UserResponseDTO updateUser(UUID id, UserRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setDocument(dto.getDocument());

        User updatedUser = userRepository.save(user);

        return toResponseDTO(updatedUser);
    }

    // DELETE
    public void delete(UUID id) {
        User user = findUserEntityById(id);
        userRepository.delete(user);
    }

    private User findUserEntityById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    private User toEntity(UserRequestDTO dto) {
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setDocument(dto.getDocument());
        return user;
    }

    private UserResponseDTO toResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setBalance(user.getBalance());
        dto.setStatus(user.getStatus());
        return dto;
    }

    public UserResponseDTO updateBalance(UUID id, BigDecimal amount) {

        User user = findUserEntityById(id);

        user.setBalance(user.getBalance().add(amount));

        return toResponseDTO(userRepository.save(user));
    }


}
