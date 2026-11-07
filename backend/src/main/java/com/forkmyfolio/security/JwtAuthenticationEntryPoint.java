package com.forkmyfolio.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forkmyfolio.advice.ApiResponseWrapper;
import com.forkmyfolio.dto.response.FieldErrorDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;


/**
 * Component that handles unauthorized (401) errors for JWT authentication.
 * It's invoked when an unauthenticated user tries to access a secured REST resource.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    /**
     * This method is called whenever an exception is thrown due to an unauthenticated user
     * trying to access a resource that requires authentication.
     *
     * @param request       that resulted in an <code>AuthenticationException</code>
     * @param response      so that the user agent can begin authentication
     * @param authException that caused the invocation
     * @throws IOException      if an input or output error is detected when the servlet handles the request
     * @throws ServletException if the request for the GET could not be handled
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        logger.error("Responding with unauthorized error. Message - {}", authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        FieldErrorDto errorDto = new FieldErrorDto("authentication", "Unauthorized: " + authException.getMessage());
        ApiResponseWrapper<Object> apiResponseWrapper = new ApiResponseWrapper<>(Collections.singletonList(errorDto), "unauthorized");

        OutputStream out = response.getOutputStream();
        ObjectMapper mapper = new ObjectMapper(); // Consider injecting this ObjectMapper if it's customized elsewhere
        mapper.writeValue(out, apiResponseWrapper);
        out.flush();
    }
}
