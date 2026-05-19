package com.meusetup.controller;

import com.meusetup.security.AppIdValidator;
import com.meusetup.security.TrailerWriter;
import com.meusetup.service.BaseExeProvider;
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

import java.util.List;

@RestController
@RequestMapping("/api")
public class InstallerController {

    private final BaseExeProvider baseExe;
    private final TrailerWriter trailerWriter;
    private final AppIdValidator validator;

    public InstallerController(BaseExeProvider baseExe,
                               TrailerWriter trailerWriter,
                               AppIdValidator validator) {
        this.baseExe = baseExe;
        this.trailerWriter = trailerWriter;
        this.validator = validator;
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateInstaller(@RequestBody List<String> appIds) {
        if (appIds == null || appIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione ao menos um aplicativo.");
        }
        if (appIds.size() > AppIdValidator.MAX_APPS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limite de aplicativos excedido.");
        }
        for (String id : appIds) {
            if (!validator.isAllowed(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID invalido.");
            }
        }

        byte[] base = baseExe.get();
        byte[] trailer = trailerWriter.build(appIds);

        byte[] result = new byte[base.length + trailer.length];
        System.arraycopy(base, 0, result, 0, base.length);
        System.arraycopy(trailer, 0, result, base.length, trailer.length);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(
            ContentDisposition.attachment().filename("Instalador.exe").build()
        );

        return ResponseEntity.ok().headers(headers).body(result);
    }
}
