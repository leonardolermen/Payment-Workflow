package com.payflow.coreservice.services;

import com.payflow.coreservice.model.User;
import com.payflow.coreservice.repository.UserRepository;
import org.springframework.stereotype.Service;

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
}
