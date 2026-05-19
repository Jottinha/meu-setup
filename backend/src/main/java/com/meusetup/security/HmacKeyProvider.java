package com.meusetup.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class HmacKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(HmacKeyProvider.class);
    private static final int KEY_BYTES = 32;

    private byte[] key;

    @PostConstruct
    void load() throws IOException {
        ClassPathResource resource = new ClassPathResource("hmac.key");
        if (!resource.exists()) {
            throw new IllegalStateException(
                "hmac.key nao encontrado no classpath. Rode scripts/gen-hmac-key.ps1 e refaca o build."
            );
        }

        String hex;
        try (InputStream is = resource.getInputStream()) {
            hex = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
        }

        if (hex.length() != KEY_BYTES * 2 || !hex.matches("^[0-9a-fA-F]+$")) {
            throw new IllegalStateException(
                "hmac.key invalido: esperado " + (KEY_BYTES * 2) + " caracteres hex."
            );
        }

        byte[] bytes = new byte[KEY_BYTES];
        for (int i = 0; i < KEY_BYTES; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        this.key = bytes;

        log.info("HMAC key loaded ({} bytes)", KEY_BYTES);
    }

    public byte[] get() {
        return key;
    }
}
