package com.payflow.coreservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


@Slf4j
@Service
public class HmacSignatureService {

    @Value("${WEBHOOK_HMAC_SECRET}")
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
        String expectedSignature = generateSignature(payload);
        return expectedSignature.equals(signature);
    }

    public String generateSignature(Object payloadObject){
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            String payloadJson = objectMapper.writeValueAsString(payloadObject);
            return generateSignature(payloadJson);
        } catch (Exception e){
            log.error("Erro ao converter o objeto do JSON para a assinatura", e);
            throw new RuntimeException("Falha ao gerar a assinatura do objeto", e);
        }
    }
}




