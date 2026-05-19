package com.meusetup.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class AppIdValidator {

    private static final Logger log = LoggerFactory.getLogger(AppIdValidator.class);

    public static final int MAX_APPS = 50;

    private static final Pattern ID_PATTERN =
        Pattern.compile("^[A-Za-z0-9][A-Za-z0-9+.\\-]{0,62}[A-Za-z0-9+]$");

    private Set<String> allowlist;

    @PostConstruct
    void loadAllowlist() throws IOException {
        ClassPathResource resource = new ClassPathResource("winget-allowlist.txt");
        if (!resource.exists()) {
            throw new IllegalStateException("winget-allowlist.txt nao encontrado no classpath.");
        }

        Set<String> ids = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (!isSyntacticallyValid(line)) {
                    throw new IllegalStateException(
                        "ID invalido no allowlist (falha regex): " + line
                    );
                }
                ids.add(line);
            }
        }
        this.allowlist = Set.copyOf(ids);

        log.info("Loaded {} IDs from allowlist", allowlist.size());
    }

    public boolean isAllowed(String id) {
        if (id == null) return false;
        if (!isSyntacticallyValid(id)) return false;
        return allowlist.contains(id);
    }

    private static boolean isSyntacticallyValid(String id) {
        if (id.contains("..")) return false;
        return ID_PATTERN.matcher(id).matches();
    }
}
