/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.rest.api.service.ReCaptchaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RecaptchaFilter extends GenericFilterBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecaptchaFilter.class);
    public static final String DEFAULT_RECAPTCHA_HEADER_NAME = "X-Recaptcha-Token";
    private static final Set<String> RESTRICTED_PATHS = new HashSet<>(Arrays.asList("/user/login", "/users/registration", "/users/registration/finalize"));

    private ReCaptchaService reCaptchaService;
    private ObjectMapper objectMapper;

    public RecaptchaFilter(ReCaptchaService reCaptchaService, ObjectMapper objectMapper) {
        this.reCaptchaService = reCaptchaService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if(RESTRICTED_PATHS.stream().anyMatch(path -> httpRequest.getPathInfo().contains(path))) {

            LOGGER.debug("Checking captcha");

            String reCaptchaToken = httpRequest.getHeader(DEFAULT_RECAPTCHA_HEADER_NAME);

            if(!reCaptchaService.isValid(reCaptchaToken)) {

                HashMap<String, Object> error = new HashMap<>();

                error.put("message", "Something goes wrong. Please try again.");
                error.put("http_status", SC_BAD_REQUEST);

                httpResponse.setStatus(SC_BAD_REQUEST);
                httpResponse.setContentType(MediaType.APPLICATION_JSON.toString());
                httpResponse.getWriter().write(objectMapper.writeValueAsString(error));
                httpResponse.getWriter().close();
            }else {
                chain.doFilter(request, response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}