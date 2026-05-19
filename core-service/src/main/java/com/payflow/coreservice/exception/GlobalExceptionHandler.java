package com.payflow.coreservice.exception;

import com.payflow.coreservice.logging.RequestLoggingFilter;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String traceId = getTraceId();
        errors.put(RequestLoggingFilter.MDC_TRACE_ID_KEY, traceId);

        log.warn("validation.error traceId={} errors={}", traceId, errors);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Dados inválidos. Verifique os campos e tente novamente.",
                errors,
                LocalDateTime.now()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(InvalidFormatException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFormatException(InvalidFormatException ex) {
        String traceId = getTraceId();

        Map<String, String> errors = new HashMap<>();
        errors.put(RequestLoggingFilter.MDC_TRACE_ID_KEY, traceId);
        fillInvalidFormatErrors(errors, ex);

        log.warn("request.parse.error traceId={} message={}", traceId, ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Dados inválidos. Verifique o payload e tente novamente.",
                errors,
                LocalDateTime.now()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    private static void fillInvalidFormatErrors(Map<String, String> errors, InvalidFormatException ex) {
        String fieldPath = ex.getPath() != null && !ex.getPath().isEmpty()
                ? ex.getPath().stream().map(ref -> ref.getFieldName()).collect(java.util.stream.Collectors.joining("."))
                : "payload";

        Class<?> targetType = ex.getTargetType();
        Object invalidValue = ex.getValue();

        if (targetType != null && targetType.isEnum()) {
            Object[] accepted = targetType.getEnumConstants();
            String acceptedValues = java.util.Arrays.stream(accepted)
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(", "));

            errors.put(fieldPath, "Valor inválido '" + invalidValue + "'. Valores aceitos: [" + acceptedValues + "]");
        } else {
            errors.put(fieldPath, "Valor inválido '" + invalidValue + "' para o tipo " + (targetType != null ? targetType.getSimpleName() : "esperado"));
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String traceId = getTraceId();

        Map<String, String> errors = new HashMap<>();
        errors.put(RequestLoggingFilter.MDC_TRACE_ID_KEY, traceId);

        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException) {
            InvalidFormatException ife = (InvalidFormatException) cause;
            fillInvalidFormatErrors(errors, ife);
        } else {
            errors.put("payload", "JSON inválido ou mal formatado");
        }

        log.warn("request.parse.error traceId={} message={}", traceId, ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Dados inválidos. Verifique o payload e tente novamente.",
                errors,
                LocalDateTime.now()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        String traceId = getTraceId();
        log.warn("business.error traceId={} type={} message={}", traceId, ex.getClass().getSimpleName(), ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                withTraceId(traceId),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(DocumentAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleDocumentAlreadyExistsException(DocumentAlreadyExistsException ex) {
        String traceId = getTraceId();
        log.warn("business.error traceId={} type={} message={}", traceId, ex.getClass().getSimpleName(), ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                withTraceId(traceId),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex) {
        String traceId = getTraceId();
        log.warn("business.error traceId={} type={} message={}", traceId, ex.getClass().getSimpleName(), ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                withTraceId(traceId),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        String traceId = getTraceId();

        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        HttpStatus safeStatus = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
        String message = ex.getReason() != null ? ex.getReason() : "Requisição inválida";

        if (safeStatus.is5xxServerError()) {
            log.error("http.error traceId={} status={} reason={}", traceId, safeStatus.value(), message, ex);
            message = "Ocorreu um erro ao processar sua solicitação. Tente novamente mais tarde.";
        } else {
            log.warn("http.error traceId={} status={} reason={}", traceId, safeStatus.value(), message);
        }

        ErrorResponse errorResponse = new ErrorResponse(
                safeStatus.value(),
                message,
                withTraceId(traceId),
                LocalDateTime.now()
        );

        return ResponseEntity.status(safeStatus).body(errorResponse);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
        String traceId = getTraceId();
        String headerName = ex.getHeaderName();
        String message = String.format("Cabeçalho obrigatório '%s' não fornecido", headerName);

        log.warn("missing.header traceId={} header={}", traceId, headerName);

        Map<String, String> errors = new HashMap<>();
        errors.put("header", headerName);
        errors.put(RequestLoggingFilter.MDC_TRACE_ID_KEY, traceId);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                errors,
                LocalDateTime.now()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        String traceId = getTraceId();
        String paramName = ex.getParameterName();
        String paramType = ex.getParameterType();
        String message = String.format("Parâmetro obrigatório '%s' (tipo: %s) não fornecido", paramName, paramType);

        log.warn("missing.parameter traceId={} parameter={} type={}", traceId, paramName, paramType);

        Map<String, String> errors = new HashMap<>();
        errors.put("parameter", paramName);
        errors.put("type", paramType);
        errors.put(RequestLoggingFilter.MDC_TRACE_ID_KEY, traceId);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                errors,
                LocalDateTime.now()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwtException(ExpiredJwtException ex) {
        String traceId = getTraceId();
        String message = "Token de autenticação expirado. Faça login novamente.";

        log.warn("jwt.expired traceId={}", traceId);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                message,
                withTraceId(traceId),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ErrorResponse> handleSignatureException(SignatureException ex) {
        String traceId = getTraceId();
        String message = "Token de autenticação inválido.";

        log.warn("jwt.invalid signature traceId={}", traceId);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                message,
                withTraceId(traceId),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        String traceId = getTraceId();
        String message = "Credenciais inválidas.";

        log.warn("auth.failed traceId={}", traceId);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                message,
                withTraceId(traceId),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        String traceId = getTraceId();
        String message = ex.getMessage() != null ? ex.getMessage() : "Argumento inválido fornecido";

        log.warn("illegal.argument traceId={} message={}", traceId, message);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                withTraceId(traceId),
                LocalDateTime.now()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String traceId = getTraceId();
        String message = "Violação de integridade de dados.";
        String rootMessage = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();

        Map<String, String> errors = new HashMap<>();
        errors.put(RequestLoggingFilter.MDC_TRACE_ID_KEY, traceId);

        if (rootMessage != null) {
            if (rootMessage.contains("uc_payments_idempotencykey")) {
                message = "Já existe um pagamento com esta chave de idempotência.";
                errors.put("field", "idempotencyKey");
            } else if (rootMessage.contains("uc_users_email")) {
                message = "Já existe um usuário com este email.";
                errors.put("field", "email");
            } else if (rootMessage.contains("uc_users_document")) {
                message = "Já existe um usuário com este documento.";
                errors.put("field", "document");
            } else {
                errors.put("constraint", rootMessage);
            }
        }

        log.warn("data.integrity.violation traceId={} message={}", traceId, rootMessage);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                message,
                errors,
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        String traceId = getTraceId();
        log.error("runtime.error traceId={} type={} message={}", traceId, ex.getClass().getSimpleName(), ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Ocorreu um erro ao processar sua solicitação. Tente novamente mais tarde.",
                withTraceId(traceId),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        String traceId = getTraceId();
        log.error("unhandled.error traceId={} type={} message={}", traceId, ex.getClass().getSimpleName(), ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Ocorreu um erro inesperado. Tente novamente mais tarde.",
                withTraceId(traceId),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private static String getTraceId() {
        return MDC.get(RequestLoggingFilter.MDC_TRACE_ID_KEY);
    }

    private static Map<String, String> withTraceId(String traceId) {
        Map<String, String> meta = new HashMap<>();
        meta.put(RequestLoggingFilter.MDC_TRACE_ID_KEY, traceId);
        return meta;
    }

    public static record ErrorResponse(
            int status,
            String message,
            Map<String, String> errors,
            LocalDateTime timestamp
    ) {}
}
