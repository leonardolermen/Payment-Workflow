package com.payflow.coreservice.services;

import com.payflow.coreservice.model.User;
import com.payflow.coreservice.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    //CREATE
    public User createUser(User user){
        return userRepository.save(user);
    }

    // READ (por id)
    public User readUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    // UPDATE
    public User updateUser(Long id, User userUpdated) {
        User user = readUserById(id);

        user.setEmail(userUpdated.getEmail());
        user.setName(userUpdated.getName());
        user.setPassword(userUpdated.getPassword());
        user.setDocument(userUpdated.getDocument());

        return userRepository.save(user);
    }

    // DELETE
    public void deletar(Long id) {
        User user = readUserById(id);
        userRepository.delete(user);
    }


}
