package ru.otus.homework.api.gateway.config;

import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.springframework.context.annotation.Bean;
import ru.otus.homework.api.gateway.config.property.AuthClientProperties;

import java.util.Collections;

@org.springframework.context.annotation.Configuration
public class AuthClientConfig {

    @Bean
    public AuthzClient authzClient(AuthClientProperties properties) {
        Configuration config = new org.keycloak.authorization.client.Configuration();
        config.setAuthServerUrl(properties.getAuthServerUrl());
        config.setRealm(properties.getRealm());
        config.setResource(properties.getClientId());
        config.setCredentials(Collections.singletonMap("secret", properties.getClientSecret()));
        return AuthzClient.create(config);
    }

}
