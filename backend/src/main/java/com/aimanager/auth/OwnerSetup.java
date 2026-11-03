package com.aimanager.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!smoke")
class OwnerSetup implements ApplicationRunner {
    private final OwnerAuthService auth;

    OwnerSetup(OwnerAuthService auth) {
        this.auth = auth;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = System.getenv("AI_MANAGER_SETUP_USERNAME");
        String password = System.getenv("AI_MANAGER_SETUP_PASSWORD");
        if (username == null && password == null) {
            return;
        }
        if (username == null || password == null) {
            throw new IllegalArgumentException("Set both owner setup environment variables");
        }
        auth.initialize(username, password);
    }
}
