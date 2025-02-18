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
package io.gravitee.apim.infra.query_service.application;

import static assertions.CoreAssertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApplicationFixture;
import io.gravitee.apim.infra.adapter.ApplicationAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Set;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationQueryServiceImplTest {

    @Mock
    ApplicationRepository applicationRepository;

    @InjectMocks
    ApplicationQueryServiceImpl service;

    @Nested
    class FindByEnvironmentId {

        @Test
        @SneakyThrows
        void should_list_applications_matching_environment_id() {
            //Given
            var env = "my-env";
            var expectedApplication = ApplicationFixture.anApplication();
            when(applicationRepository.findAllByEnvironment("my-env", ApplicationStatus.values())).thenReturn(Set.of(expectedApplication));

            //When
            Set<BaseApplicationEntity> result = service.findByEnvironment(env);

            //Then
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result.size()).isOne();
                soft.assertThat(result).contains(ApplicationAdapter.INSTANCE.toEntity(expectedApplication));
            });
        }

        @Test
        @SneakyThrows
        void should_throw_when_technical_exception_occurs() {
            // Given
            var envId = "different-env";
            when(applicationRepository.findAllByEnvironment("different-env", ApplicationStatus.values()))
                .thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findByEnvironment(envId));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurred while finding applications by environment id: different-env");
        }
    }
}
