package com.incidentreplay.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test-target")
public class TestTargetController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @GetMapping("/delay")
    public String delay() throws InterruptedException {
        Thread.sleep(2000);
        return "Delayed";
    }

    @RequestMapping(value = "/echo", method = {
            RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.PATCH, RequestMethod.DELETE
    })
    public Map<String, Object> echo(HttpServletRequest request, @RequestBody(required = false) String body) {
        Map<String, Object> response = new HashMap<>();
        response.put("method", request.getMethod());
        response.put("path", request.getRequestURI());

        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        
        response.put("headers", headers);
        response.put("body", body);

        return response;
    }
}
