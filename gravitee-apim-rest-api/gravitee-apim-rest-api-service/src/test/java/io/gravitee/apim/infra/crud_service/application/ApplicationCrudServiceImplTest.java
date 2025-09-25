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
package io.gravitee.apim.infra.crud_service.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.definition.model.Origin;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ApplicationCrudServiceImplTest {

    private static final String ENVIRONMENT_ID = "environment-id";

    ApplicationRepository applicationRepository;

    ApplicationCrudService service;

    @BeforeEach
    void setUp() {
        applicationRepository = mock(ApplicationRepository.class);

        service = new ApplicationCrudServiceImpl(applicationRepository);
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Nested
    class FindById {

        @Test
        void should_return_application_entity_when_found() throws TechnicalException {
            // Given
            String applicationId = "appId";
            when(applicationRepository.findById(any())).thenAnswer(invocation ->
                Optional.of(anApplication().id(invocation.getArgument(0)).build())
            );

            // When
            var result = service.findById(applicationId, ENVIRONMENT_ID);

            // Then
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result.getApiKeyMode()).isEqualTo(io.gravitee.rest.api.model.ApiKeyMode.EXCLUSIVE);
                soft.assertThat(result.getBackground()).isEqualTo("app-background");
                soft.assertThat(result.getCreatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")));
                soft.assertThat(result.getDescription()).isEqualTo("app-description");
                soft.assertThat(result.isDisableMembershipNotifications()).isTrue();
                soft.assertThat(result.getDomain()).isEqualTo("app-domain");
                soft.assertThat(result.getGroups()).containsExactly("group1");
                soft.assertThat(result.getId()).isEqualTo(applicationId);
                soft.assertThat(result.getName()).isEqualTo("app-name");
                soft.assertThat(result.getOrigin()).isEqualTo(Origin.MANAGEMENT);
                soft.assertThat(result.getPicture()).isEqualTo("app-picture");
                soft.assertThat(result.getStatus()).isEqualTo("ACTIVE");
                soft.assertThat(result.getType()).isEqualTo("SIMPLE");
                soft.assertThat(result.getUpdatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")));
            });
        }

        @Test
        void should_throw_when_environment_does_not_match_with_current_environment() throws TechnicalException {
            // Given
            String applicationId = "appId";
            when(applicationRepository.findById(any())).thenAnswer(invocation ->
                Optional.of(anApplication().id(invocation.getArgument(0)).build())
            );

            // When
            Throwable throwable = catchThrowable(() -> service.findById(applicationId, "OTHER"));

            // Then
            assertThat(throwable)
                .isInstanceOf(ApplicationNotFoundException.class)
                .hasMessage("Application [" + applicationId + "] cannot be found.");
        }

        @Test
        void should_throw_when_application_not_found() throws TechnicalException {
            // Given
            String applicationId = "unknown";
            when(applicationRepository.findById(any())).thenReturn(Optional.empty());

            // When
            Throwable throwable = catchThrowable(() -> service.findById(applicationId, ENVIRONMENT_ID));

            // Then
            assertThat(throwable)
                .isInstanceOf(ApplicationNotFoundException.class)
                .hasMessage("Application [" + applicationId + "] cannot be found.");
        }

        @Test
        void should_throw_when_find_fails() throws TechnicalException {
            // Given
            String applicationId = "unknown";
            when(applicationRepository.findById(any())).thenThrow(new TechnicalException());

            // When
            Throwable throwable = catchThrowable(() -> service.findById(applicationId, ENVIRONMENT_ID));

            // Then
            assertThat(throwable).isInstanceOf(TechnicalManagementException.class);
        }
    }

    private Application.ApplicationBuilder anApplication() {
        return Application.builder()
            .apiKeyMode(io.gravitee.repository.management.model.ApiKeyMode.EXCLUSIVE)
            .background("app-background")
            .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
            .description("app-description")
            .disableMembershipNotifications(true)
            .domain("app-domain")
            .environmentId(ENVIRONMENT_ID)
            .groups(Set.of("group1"))
            .metadata(Map.of("key1", "value1"))
            .name("app-name")
            .origin(Origin.MANAGEMENT)
            .picture("app-picture")
            .status(ApplicationStatus.ACTIVE)
            .type(ApplicationType.SIMPLE)
            .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")));
    }
}
