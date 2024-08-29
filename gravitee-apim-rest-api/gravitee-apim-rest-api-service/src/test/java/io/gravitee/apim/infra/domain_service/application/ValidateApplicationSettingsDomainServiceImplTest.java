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
package io.gravitee.apim.infra.domain_service.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import io.gravitee.apim.core.application.domain_service.ValidateApplicationSettingsDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import io.gravitee.rest.api.service.impl.configuration.application.ApplicationTypeServiceImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ValidateApplicationSettingsDomainServiceImplTest {

    private final ApplicationRepository applicationRepository = mock(ApplicationRepository.class);

    private final ParameterService parameterService = mock(ParameterService.class);

    private final ApplicationTypeService applicationTypeService = new ApplicationTypeServiceImpl();

    private ValidateApplicationSettingsDomainServiceImpl cut;

    @BeforeEach
    void setUp() {
        reset(applicationRepository, parameterService);
        cut = new ValidateApplicationSettingsDomainServiceImpl(applicationRepository, applicationTypeService, parameterService);
    }

    @Test
    void should_replace_null_redirect_uris_with_empty_list() {
        var givenOauthSettings = OAuthClientSettings
            .builder()
            .applicationType("BACKEND_TO_BACKEND")
            .redirectUris(null)
            .grantTypes(List.of("client_credentials"))
            .build();

        var expectedOauthSettings = givenOauthSettings.toBuilder().redirectUris(List.of()).responseTypes(List.of()).build();

        var givenSettings = ApplicationSettings.builder().oauth(givenOauthSettings).build();

        var expectedSettings = givenSettings.toBuilder().oauth(expectedOauthSettings).build();

        var input = new ValidateApplicationSettingsDomainService.Input(
            AuditInfo.builder().organizationId("test").environmentId("test").build(),
            "app-id",
            givenSettings
        );

        var result = cut.validateAndSanitize(input);

        assertThat(result.value()).isNotEmpty().hasValue(input.sanitized(expectedSettings));
    }

    @Test
    void should_set_response_types() {
        var givenOauthSettings = OAuthClientSettings
            .builder()
            .applicationType("BROWSER")
            .redirectUris(List.of("https://app.example.com"))
            .grantTypes(List.of("authorization_code"))
            .build();

        var expectedOauthSettings = givenOauthSettings.toBuilder().responseTypes(List.of("code")).build();

        var givenSettings = ApplicationSettings.builder().oauth(givenOauthSettings).build();

        var expectedSettings = givenSettings.toBuilder().oauth(expectedOauthSettings).build();

        var input = new ValidateApplicationSettingsDomainService.Input(
            AuditInfo.builder().organizationId("test").environmentId("test").build(),
            "app-id",
            givenSettings
        );

        var result = cut.validateAndSanitize(input);

        assertThat(result.value()).isNotEmpty().hasValue(input.sanitized(expectedSettings));
    }
}
