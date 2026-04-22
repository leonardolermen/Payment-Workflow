package com.payflow.coreservice.controller;

import com.payflow.coreservice.model.User;
import com.payflow.coreservice.services.UserService;
import org.springframework.web.bind.annotation.*;

@RestController //define que essa classe faz requisições https
@RequestMapping("/users") //define a rota
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // CREATE
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    // READ (por id)
    @GetMapping("/{id}")
    public User readUserById(@PathVariable Long id) {
        return userService.readUserById(id);
    }

    // UPDATE
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deletar(id);
    }
}

