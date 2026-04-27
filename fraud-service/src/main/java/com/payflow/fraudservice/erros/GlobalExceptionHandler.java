package com.payflow.fraudservice.erros;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponseDTO> handlerFeignException(FeignException e){

        log.error("Erro de comunicação com serviço externo: {}", e.getMessage());

        return ResponseEntity.status(503)
                .body(new ErrorResponseDTO(
                        "SERVIÇO INDISPONIVEL",
                        "Serviço core temporariamente indisponível")
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handlerValidationException(MethodArgumentNotValidException e){
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.error("Erro de validação: {}", errors);

        return ResponseEntity.badRequest()
                .body(new ErrorResponseDTO("ERRO DE VALIDAÇÃO", errors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handlerIllegalArgument(IllegalArgumentException e){
        log.error("Argumento inválido: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponseDTO("ARGUMENTO INVÁLIDO", e.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponseDTO> handlerDataAccessException(DataAccessException e){

        log.error("Error de acesso ao banco: {}", e.getMessage());

        return ResponseEntity.status(500)
                .body(new ErrorResponseDTO(
                        "ERRO DE ACESSO AO BANCO",
                        "Erro de acesso ao banco de dados"));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handlerEntityNotFound(EntityNotFoundException e){

        log.warn("Recurso não encontrado: {}", e.getMessage());

        return ResponseEntity.status(400)
                .body(new ErrorResponseDTO("RECURSO NÃO ENCONTRADO", "Recurso não encontrado"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> hanlderGenereicException(Exception e){
        log.error("Erro não tratado" , e);

        return ResponseEntity.status(500)
                .body(new ErrorResponseDTO("ERRO INTERNO", "Erro interno no servidor"));
    }
}
