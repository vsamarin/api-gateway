package ru.otus.homework.api.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.resource.ProtectedResource;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.otus.homework.api.gateway.config.property.AuthClientProperties;
import ru.otus.homework.api.gateway.dto.Error;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static org.springframework.http.HttpStatus.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProxyService {

    private final AuthzClient authzClient;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AuthClientProperties authProperties;

    @Value("${config.baseUrl}")
    private String baseUrl;

    public ResponseEntity<String> proxyRequest(
            String body,
            HttpMethod method,
            HttpServletRequest request
    ) throws JsonProcessingException {
        final String requestUrl = request.getRequestURI();

        final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization)) {
            return errorResponse(UNAUTHORIZED, "Bearer token not found");
        }

        String userId;
        try {
            final String accessToken = authorization.substring("Bearer ".length());
            //authzClient.authorization(accessToken).authorize();
            JWT jwt = JWTParser.parse(accessToken);
            validate(jwt);
            userId = jwt.getJWTClaimsSet().getStringClaim("sub");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return errorResponse(UNAUTHORIZED, "Bearer is invalid");
        }

        if (!HttpMethod.POST.name().equalsIgnoreCase(request.getMethod())) {
            ResourceRepresentation resource = new ResourceRepresentation();
            resource.setName(requestUrl);
            resource.setOwner(userId);
            ProtectedResource resourceClient = authzClient.protection().resource();
            ResourceRepresentation existingResource = resourceClient.findByName(resource.getName(), userId);
            if (existingResource == null) {
                return errorResponse(FORBIDDEN, "You don't have enough permissions");
            }
        }

        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(requestUrl)
                .query(request.getQueryString())
                .build(true)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(headerName)) {
                log.info(request.getHeader(headerName));
            } else {
                headers.set(headerName, request.getHeader(headerName));
            }
        }

        headers.remove(HttpHeaders.ACCEPT_ENCODING);
        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> serverResponse;
        try {
            serverResponse = restTemplate.exchange(uri, method, httpEntity, String.class);
        } catch (HttpStatusCodeException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(e.getMessage());
        }

        if (HttpMethod.POST.name().equalsIgnoreCase(request.getMethod())) {
            try {
                JsonNode node = objectMapper.readTree(serverResponse.getBody());
                String id = node.get("id").asText();
                String url = String.format("%s/%s", requestUrl, id);

                ResourceRepresentation resource = new ResourceRepresentation();
                resource.setName(url);
                resource.setType("urn:resources:users:user");
                resource.setUris(Collections.singleton(url));
                resource.setOwner(userId);

                ProtectedResource resourceClient = authzClient.protection().resource();
                resourceClient.create(resource);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return errorResponse(INTERNAL_SERVER_ERROR, objectMapper.writeValueAsString(e.getMessage()));
            }
        }

        if (HttpMethod.DELETE.name().equalsIgnoreCase(request.getMethod())) {
            try {
                ResourceRepresentation resource = new ResourceRepresentation();
                resource.setName(requestUrl);
                resource.setOwner(userId);
                ProtectedResource resourceClient = authzClient.protection().resource();
                ResourceRepresentation existingResource = resourceClient.findByName(resource.getName(), userId);
                if (existingResource != null) {
                    resourceClient.delete(existingResource.getId());
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return errorResponse(INTERNAL_SERVER_ERROR, objectMapper.writeValueAsString(e.getMessage()));
            }
        }

        HttpHeaders responseHeaders = new HttpHeaders();
        final List<String> responseType = serverResponse.getHeaders().get(HttpHeaders.CONTENT_TYPE);
        if (!CollectionUtils.isEmpty(responseType)) {
            responseHeaders.put(HttpHeaders.CONTENT_TYPE, responseType);
        }
        return new ResponseEntity<>(serverResponse.getBody(), responseHeaders, serverResponse.getStatusCode());
    }

    @SneakyThrows
    private ResponseEntity<String> errorResponse(HttpStatus status, String message) {
        Error error = new Error(status.value(), message);
        return new ResponseEntity<>(objectMapper.writeValueAsString(error), status);
    }

    private void validate(JWT jwt) throws MalformedURLException, BadJOSEException, JOSEException {
        ResourceRetriever resourceRetriever = new DefaultResourceRetriever();
        @SuppressWarnings("deprecation")
        JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(
                new URL(String.format("%s/realms/%s/protocol/openid-connect/certs",
                        authProperties.getAuthServerUrl(), authProperties.getRealm())),
                resourceRetriever
        );
        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(JOSEObjectType.JWT));
        jwtProcessor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource));
        jwtProcessor.process(jwt, null);
    }

}
