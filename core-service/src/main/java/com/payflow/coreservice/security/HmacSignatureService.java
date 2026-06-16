package com.payflow.coreservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;


@Slf4j
@Service
public class HmacSignatureService {

    @Value("${webhook.hmac.secret}")
    private String hmacSecret;

    private static final String HMAC_SHA256 = "HmacSHA256";

    public String generateSignature(String payload){
        try{
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    hmacSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e){
            log.error("Erro ao gerar a signature HMAC", e);
            throw new RuntimeException("Erro ao gerar a signature HMAC", e);
        }
    }

    public boolean verifySignature(String payload, String signature){
        try{
            String expectedSignature = generateSignature(payload);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] expectedBytes = digest.digest(expectedSignature.getBytes(StandardCharsets.UTF_8));
            byte[] actualBytes = digest.digest(signature.getBytes(StandardCharsets.UTF_8));

            return Arrays.equals(expectedBytes, actualBytes);
        } catch (Exception e){
            log.error("Erro ao verificar a signature HMAC", e);
            return false;
        }
    }

    public String generateSignature(Object payloadObject){
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            String payloadJson = objectMapper.writeValueAsString(payloadObject);
            return generateSignature(payloadJson);
        } catch (Exception e){
            log.error("Erro ao converter o objeto do JSON para a assinatura", e);
            throw new RuntimeException("Falha ao gerar a assinatura do objeto", e);
        }
    }
}




