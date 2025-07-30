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
package io.gravitee.rest.api.security.cors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

class AbstractGraviteeUrlBasedCorsConfigurationSourceTest {

    MockEnvironment fakeEnvironment = new MockEnvironment();

    AbstractGraviteeUrlBasedCorsConfigurationSource cut = new AbstractGraviteeUrlBasedCorsConfigurationSource(
        fakeEnvironment,
        mock(ParameterService.class),
        mock(InstallationAccessQueryService.class),
        mock(EventManager.class),
        ParameterReferenceType.ENVIRONMENT
    ) {
        @Override
        protected String getReferenceId() {
            return "env-id";
        }
    };

    @Test
    void should_cache_cors_configuration_using_referer_header_if_defined() {
        var request = buildRequest("mapi.gravitee.dev", "https://local.fr.gravitee.dev", null);

        var corsConfiguration = cut.getCorsConfiguration(request);
        assertThat(cut.getCorsConfiguration(request)).isSameAs(corsConfiguration);
        assertThat(cut.getCorsConfiguration(buildRequest("mapi.gravitee.dev", "other-referer", null))).isNotSameAs(corsConfiguration);
    }

    @Test
    void should_cache_cors_configuration_using_origin_header_if_defined() {
        var request = buildRequest("mapi.gravitee.dev", null, "https://local.fr.gravitee.dev");

        var corsConfiguration = cut.getCorsConfiguration(request);
        assertThat(cut.getCorsConfiguration(request)).isSameAs(corsConfiguration);
        assertThat(cut.getCorsConfiguration(buildRequest("mapi.gravitee.dev", null, "other-origin"))).isNotSameAs(corsConfiguration);
    }

    @Test
    void should_cache_cors_configuration_using_request_url_if_no_referer_or_origin_header_defined() {
        var request = buildRequest("mapi.gravitee.dev", null, null);

        var corsConfiguration = cut.getCorsConfiguration(request);
        assertThat(cut.getCorsConfiguration(request)).isSameAs(corsConfiguration);
        assertThat(cut.getCorsConfiguration(buildRequest("other.gravitee.dev", null, null))).isNotSameAs(corsConfiguration);
    }

    MockHttpServletRequest buildRequest(String serverName, String referer, String origin) {
        var request = new MockHttpServletRequest("GET", "/");
        request.setServerName(serverName);

        Optional.ofNullable(referer).ifPresent(r -> request.addHeader(HttpHeaderNames.REFERER, r));
        Optional.ofNullable(origin).ifPresent(o -> request.addHeader(HttpHeaderNames.ORIGIN, o));

        return request;
    }
}
