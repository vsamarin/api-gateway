package ru.otus.homework.api.gateway.config.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("config.keycloak")
public class AuthClientProperties {
    @Value("authServerUrl")
    private String authServerUrl;
    @Value("realm")
    private String realm;
    @Value("clientId")
    private String clientId;
    @Value("clientSecret")
    private String clientSecret;
}
