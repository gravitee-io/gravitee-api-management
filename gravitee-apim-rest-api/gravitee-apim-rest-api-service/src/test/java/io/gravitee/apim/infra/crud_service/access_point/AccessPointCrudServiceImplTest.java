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
package io.gravitee.apim.infra.crud_service.access_point;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.apim.infra.adapter.AccessPointAdapter;
import io.gravitee.common.event.EventManager;
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.model.AccessPointTarget;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Arrays;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccessPointCrudServiceImplTest {

    @Mock
    AccessPointRepository accessPointRepository;

    @Mock
    EventManager eventManager;

    AccessPointCrudServiceImpl service;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "random-id");
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
    }

    @BeforeEach
    void setUp() {
        accessPointRepository = mock(AccessPointRepository.class);
        service = new AccessPointCrudServiceImpl(accessPointRepository, eventManager);
    }

    @Nested
    class UpdateAccessPoints {

        @ParameterizedTest
        @EnumSource(AccessPoint.ReferenceType.class)
        @SneakyThrows
        void should_delete_existing_access_points_of_the_reference(AccessPoint.ReferenceType referenceType) {
            // Given
            var accessPoints = List.<AccessPoint>of();

            // When
            service.updateAccessPoints(referenceType, "ref-id", accessPoints);

            // Then
            verify(accessPointRepository).deleteByReference(AccessPointAdapter.INSTANCE.fromEntity(referenceType), "ref-id");
        }

        @ParameterizedTest
        @EnumSource(AccessPoint.ReferenceType.class)
        @SneakyThrows
        void should_create_all_access_points_provided(AccessPoint.ReferenceType referenceType) {
            // Given

            var accessPoints = Arrays
                .stream(AccessPoint.Target.values())
                .map(target ->
                    AccessPoint
                        .builder()
                        .referenceType(referenceType)
                        .referenceId("my-ref")
                        .host("my-host")
                        .secured(true)
                        .overriding(true)
                        .target(target)
                        .build()
                )
                .toList();

            // When
            service.updateAccessPoints(referenceType, "ref-id", accessPoints);

            // Then
            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.AccessPoint.class);
            verify(accessPointRepository, times(accessPoints.size())).create(captor.capture());

            Assertions
                .assertThat(captor.getAllValues())
                .hasSameSizeAs(accessPoints)
                .containsOnly(
                    io.gravitee.repository.management.model.AccessPoint
                        .builder()
                        .id("random-id")
                        .referenceType(AccessPointAdapter.INSTANCE.fromEntity(referenceType))
                        .referenceId("my-ref")
                        .host("my-host")
                        .secured(true)
                        .overriding(true)
                        .target(AccessPointTarget.GATEWAY)
                        .build(),
                    io.gravitee.repository.management.model.AccessPoint
                        .builder()
                        .id("random-id")
                        .referenceType(AccessPointAdapter.INSTANCE.fromEntity(referenceType))
                        .referenceId("my-ref")
                        .host("my-host")
                        .secured(true)
                        .overriding(true)
                        .target(AccessPointTarget.CONSOLE_API)
                        .build(),
                    io.gravitee.repository.management.model.AccessPoint
                        .builder()
                        .id("random-id")
                        .referenceType(AccessPointAdapter.INSTANCE.fromEntity(referenceType))
                        .referenceId("my-ref")
                        .host("my-host")
                        .secured(true)
                        .overriding(true)
                        .target(AccessPointTarget.PORTAL_API)
                        .build(),
                    io.gravitee.repository.management.model.AccessPoint
                        .builder()
                        .id("random-id")
                        .referenceType(AccessPointAdapter.INSTANCE.fromEntity(referenceType))
                        .referenceId("my-ref")
                        .host("my-host")
                        .secured(true)
                        .overriding(true)
                        .target(AccessPointTarget.CONSOLE)
                        .build(),
                    io.gravitee.repository.management.model.AccessPoint
                        .builder()
                        .id("random-id")
                        .referenceType(AccessPointAdapter.INSTANCE.fromEntity(referenceType))
                        .referenceId("my-ref")
                        .host("my-host")
                        .secured(true)
                        .overriding(true)
                        .target(AccessPointTarget.PORTAL)
                        .build()
                );
        }
    }
}
