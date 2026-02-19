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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.access_point.model.AccessPointEvent;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.reactivex.rxjava3.annotations.NonNull;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AbstractGraviteeUrlBasedCorsConfigurationSourceTest {

    @Mock
    private EventManager eventManager;

    private MockEnvironment fakeEnvironment;
    private AbstractGraviteeUrlBasedCorsConfigurationSource cut;

    @BeforeEach
    void setUp() {
        fakeEnvironment = new MockEnvironment();
        cut = buildCut();
    }

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

    @Test
    void should_evict_from_cache_and_unregister_from_event_manager_when_entry_expires() {
        // Expires after 50ms.
        fakeEnvironment.setProperty("cors.cache.ttl", "50");

        cut = buildCut();

        var request = buildRequest("mapi.gravitee.dev", null, null);

        var corsConfiguration = cut.getCorsConfiguration(request);
        assertThat(cut.getCorsConfiguration(request)).isSameAs(corsConfiguration);
        assertThat(cut.getCorsConfiguration(buildRequest("other.gravitee.dev", null, null))).isNotSameAs(corsConfiguration);

        Awaitility.waitAtMost(2000, TimeUnit.MILLISECONDS)
            .pollDelay(50, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                // Eviction only occurs on next access after TTL, so we need to call getCorsConfiguration again to trigger eviction of the expired entry.
                cut.getCorsConfiguration(request);
                verify(eventManager, atLeastOnce()).unsubscribeForEvents(
                    any(GraviteeCorsConfiguration.ParameterKeyEventListener.class),
                    eq(Key.class)
                );
                verify(eventManager, atLeastOnce()).unsubscribeForEvents(
                    any(GraviteeCorsConfiguration.AccessPointEventListener.class),
                    eq(AccessPointEvent.class)
                );
            });
    }

    MockHttpServletRequest buildRequest(String serverName, String referer, String origin) {
        var request = new MockHttpServletRequest("GET", "/");
        request.setServerName(serverName);

        Optional.ofNullable(referer).ifPresent(r -> request.addHeader(HttpHeaderNames.REFERER, r));
        Optional.ofNullable(origin).ifPresent(o -> request.addHeader(HttpHeaderNames.ORIGIN, o));

        return request;
    }

    private @NonNull AbstractGraviteeUrlBasedCorsConfigurationSource buildCut() {
        return new AbstractGraviteeUrlBasedCorsConfigurationSource(
            fakeEnvironment,
            mock(ParameterService.class),
            mock(InstallationAccessQueryService.class),
            eventManager,
            ParameterReferenceType.ENVIRONMENT
        ) {
            @Override
            protected String getReferenceId() {
                return "env-id";
            }
        };
    }
}
