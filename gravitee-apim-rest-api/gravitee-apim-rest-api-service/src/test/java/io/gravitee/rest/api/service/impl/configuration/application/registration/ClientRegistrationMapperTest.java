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
package io.gravitee.rest.api.service.impl.configuration.application.registration;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.service.impl.configuration.application.registration.client.register.ClientRegistrationRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClientRegistrationMapperTest {

    @ParameterizedTest
    @CsvSource({ "WEB,web", "NATIVE,native", "BROWSER,browser", "BACKEND_TO_BACKEND,backend_to_backend", "web,web" })
    void should_send_lowercase_application_type_when_registering(String applicationType, String expected) {
        NewApplicationEntity application = new NewApplicationEntity();
        application.setName("Test Web App");
        application.setSettings(settings(applicationType));

        ClientRegistrationRequest request = ClientRegistrationMapper.INSTANCE.toClientRegistrationRequest(
            new ClientRegistrationRequest(),
            application
        );

        assertThat(request.getApplicationType()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({ "WEB,web", "NATIVE,native", "BROWSER,browser", "BACKEND_TO_BACKEND,backend_to_backend" })
    void should_send_lowercase_application_type_when_updating(String applicationType, String expected) {
        UpdateApplicationEntity application = UpdateApplicationEntity.builder()
            .name("Test Web App")
            .description("desc")
            .settings(settings(applicationType))
            .build();

        ClientRegistrationRequest request = new ClientRegistrationRequest();
        ClientRegistrationMapper.INSTANCE.toClientRegistrationRequest(request, application);

        assertThat(request.getApplicationType()).isEqualTo(expected);
    }

    private static ApplicationSettings settings(String applicationType) {
        return ApplicationSettings.builder().oauth(OAuthClientSettings.builder().applicationType(applicationType).build()).build();
    }
}
