package com.meusetup.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
public class InstallerController {

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateInstaller(@RequestBody List<String> appIds) throws IOException {
        if (appIds == null || appIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione ao menos um aplicativo.");
        }

        ClassPathResource resource = new ClassPathResource("base.exe");
        if (!resource.exists()) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Executável base não encontrado. Compile o go-base primeiro."
            );
        }

        byte[] baseBytes;
        try (InputStream is = resource.getInputStream()) {
            baseBytes = is.readAllBytes();
        }

        // Formato que o Go lê ao varrer o próprio binário de trás para frente
        String appList = "||APPS:" + String.join(",", appIds) + "||";
        byte[] suffix = appList.getBytes(StandardCharsets.UTF_8);

        byte[] result = new byte[baseBytes.length + suffix.length];
        System.arraycopy(baseBytes, 0, result, 0, baseBytes.length);
        System.arraycopy(suffix, 0, result, baseBytes.length, suffix.length);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(
            ContentDisposition.attachment().filename("Instalador.exe").build()
        );

        return ResponseEntity.ok().headers(headers).body(result);
    }
}
