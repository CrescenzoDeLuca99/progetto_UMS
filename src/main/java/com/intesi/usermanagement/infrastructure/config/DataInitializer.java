package com.intesi.usermanagement.infrastructure.config;

import com.intesi.usermanagement.domain.enums.RoleName;
import com.intesi.usermanagement.domain.model.Role;
import com.intesi.usermanagement.infrastructure.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(ApplicationArguments args) {
        Arrays.stream(RoleName.values())
                .filter(roleName -> roleRepository.findByName(roleName).isEmpty())
                .forEach(roleName -> {
                    roleRepository.save(new Role(roleName));
                    log.info("Ruolo inizializzato: {}", roleName);
                });
    }
}
