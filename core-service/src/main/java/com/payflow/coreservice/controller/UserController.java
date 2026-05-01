package com.payflow.coreservice.controller;

import com.payflow.commons.dto.user.UserRequest;
import com.payflow.commons.dto.user.UserResponse;
import com.payflow.coreservice.services.UserService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {
    /*
     * ATENÇÃO: Este controller é para operações administrativas em usuários.
     * Para registro/login de usuários finais, use AuthController (/auth).
     * 
     * AuthController: POST /auth/register (com criptografia + JWT)
     * UserController: POST /users (administrativo, sem criptografia)
     */

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // =========================
    // GET /users/{id}
    // =========================
    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable UUID id) {
        return userService.readUserById(id);
    }

    // =========================
    // PUT /users/{id}/balance
    // =========================
    @PutMapping("/{id}/balance")
    public UserResponse updateBalance(@PathVariable UUID id,
                                      @RequestParam BigDecimal amount) {
        return userService.updateBalance(id, amount);
    }
}