/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.event.EventManager;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;

class GraviteeUrlBasedCorsConfigurationSourceTest {

    private static final String NOT_ALLOWED_ORIGIN = "https://cockpit.example.com";

    private CorsFilter corsFilter;

    @BeforeEach
    void setUp() {
        var parameterService = mock(ParameterService.class);
        when(
            parameterService.find(
                any(ExecutionContext.class),
                eq(Key.CONSOLE_HTTP_CORS_ALLOW_ORIGIN),
                anyString(),
                eq(ParameterReferenceType.ORGANIZATION)
            )
        ).thenReturn("https://console.example.com");
        corsFilter = new CorsFilter(
            new GraviteeUrlBasedCorsConfigurationSource(
                new MockEnvironment(),
                parameterService,
                mock(InstallationAccessQueryService.class),
                mock(EventManager.class),
                ParameterReferenceType.ORGANIZATION
            )
        );
    }

    @Test
    void should_reject_a_post_from_an_origin_that_is_not_allowed() throws Exception {
        assertThat(postFromNotAllowedOrigin("/auth/external").getStatus()).isEqualTo(403);
    }

    @Test
    void should_not_apply_cors_to_the_cockpit_login_form() throws Exception {
        var response = postFromNotAllowedOrigin("/auth/cockpit");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    }

    private MockHttpServletResponse postFromNotAllowedOrigin(String path) throws Exception {
        var request = new MockHttpServletRequest("POST", path);
        request.setPathInfo(path);
        request.addHeader(HttpHeaders.ORIGIN, NOT_ALLOWED_ORIGIN);
        var response = new MockHttpServletResponse();
        corsFilter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
