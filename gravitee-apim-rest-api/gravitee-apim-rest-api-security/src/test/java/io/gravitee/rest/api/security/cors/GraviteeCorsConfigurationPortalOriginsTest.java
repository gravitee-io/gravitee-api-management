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

import static io.gravitee.rest.api.security.cors.GraviteeCorsConfiguration.UNDEFINED_REFERENCE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.event.EventManager;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GraviteeCorsConfigurationPortalOriginsTest {

    private static final String RESTRICTED_ORIGIN = "https://console.acme.io";

    @Mock
    private EventManager eventManager;

    @Mock
    private ParameterService parameterService;

    @Mock
    private InstallationAccessQueryService installationAccessQueryService;

    private final MockEnvironment environment = new MockEnvironment();

    @BeforeEach
    void setUp() {
        environment.setProperty(Key.PORTAL_HTTP_CORS_ALLOW_ORIGIN.key(), RESTRICTED_ORIGIN);
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    void should_allow_every_portal_access_point_before_the_environment_is_known() {
        when(installationAccessQueryService.getPortalUrls()).thenReturn(List.of("http://localhost:4100"));
        when(installationAccessQueryService.getPortalInstanceUrls()).thenReturn(List.of("http://partners.localhost:4100"));

        var cors = corsFor(UNDEFINED_REFERENCE_ID);

        assertThat(cors.checkOrigin("http://partners.localhost:4100")).isEqualTo("http://partners.localhost:4100");
        assertThat(cors.checkOrigin("http://localhost:4100")).isEqualTo("http://localhost:4100");
        assertThat(cors.checkOrigin("http://unknown.localhost:4100")).isNull();
    }

    @Test
    void should_allow_the_portal_access_points_of_the_environment() {
        when(
            parameterService.find(any(), eq(Key.PORTAL_HTTP_CORS_ALLOW_ORIGIN), eq("env-id"), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(RESTRICTED_ORIGIN);
        when(installationAccessQueryService.getPortalUrls("env-id")).thenReturn(List.of("http://localhost:4100"));
        when(installationAccessQueryService.getPortalInstanceUrls("env-id")).thenReturn(List.of("https://partners.acme.io"));

        var cors = corsFor("env-id");

        assertThat(cors.checkOrigin("https://partners.acme.io")).isEqualTo("https://partners.acme.io");
        assertThat(cors.checkOrigin(RESTRICTED_ORIGIN)).isEqualTo(RESTRICTED_ORIGIN);
        assertThat(cors.checkOrigin("https://docs.acme.io")).isNull();
    }

    private GraviteeCorsConfiguration corsFor(String referenceId) {
        return new GraviteeCorsConfiguration(
            environment,
            parameterService,
            installationAccessQueryService,
            eventManager,
            referenceId,
            ParameterReferenceType.ENVIRONMENT
        );
    }
}
