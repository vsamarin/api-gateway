package ru.otus.homework.api.gateway.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.homework.api.gateway.service.ProxyService;

/**
 * Прокси контроллер, для проксирования всех запросов, кроме служебных
 */
@RestController
@RequiredArgsConstructor
public class ProxyController {

    private final ProxyService proxyService;

    @RequestMapping("/**")
    public ResponseEntity<String> proxyRequest(
            @RequestBody(required = false) String body,
            HttpMethod method,
            HttpServletRequest request
    ) throws JsonProcessingException {
        return proxyService.proxyRequest(body, method, request);
    }

}
