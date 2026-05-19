package com.meusetup.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class BaseExeProvider {

    private static final Logger log = LoggerFactory.getLogger(BaseExeProvider.class);

    private byte[] baseBytes;

    @PostConstruct
    void load() throws IOException {
        ClassPathResource resource = new ClassPathResource("base.exe");
        if (!resource.exists()) {
            throw new IllegalStateException(
                "base.exe nao encontrado no classpath. Compile o go-base primeiro (go-base/build.ps1)."
            );
        }
        try (InputStream is = resource.getInputStream()) {
            this.baseBytes = is.readAllBytes();
        }
        log.info("base.exe loaded ({} bytes)", baseBytes.length);
    }

    public byte[] get() {
        return baseBytes;
    }
}
