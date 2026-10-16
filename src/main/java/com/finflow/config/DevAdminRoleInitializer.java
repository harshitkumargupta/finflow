package com.finflow.config;

import com.finflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Profile("dev")
public class DevAdminRoleInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final String configuredEmails;

    public DevAdminRoleInitializer(UserRepository userRepository,
                                   @Value("${finflow.admin.emails:}") String configuredEmails) {
        this.userRepository = userRepository;
        this.configuredEmails = configuredEmails;
    }

    @Override
    public void run(String... args) {
        Arrays.stream(configuredEmails.split(","))
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .map(userRepository::findByEmail)
                .flatMap(java.util.Optional::stream)
                .forEach(user -> {
                    user.setRole("ADMIN");
                    userRepository.save(user);
                });
    }
}
