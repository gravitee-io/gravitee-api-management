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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.common.event.EventManager;
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.api.search.AccessPointCriteria;
import io.gravitee.repository.management.model.AccessPointReferenceType;
import io.gravitee.repository.management.model.AccessPointStatus;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Arrays;
import java.util.Collections;
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

    @ParameterizedTest
    @EnumSource(AccessPoint.ReferenceType.class)
    @SneakyThrows
    void should_delete_existing_access_points_of_the_reference(AccessPoint.ReferenceType referenceType) {
        // When
        service.deleteAccessPoints(referenceType, "ref-id");

        AccessPointCriteria expectedAccessPointCriteria = AccessPointCriteria
            .builder()
            .referenceType(AccessPointReferenceType.valueOf(referenceType.name()))
            .referenceIds(Collections.singletonList("ref-id"))
            .status(AccessPointStatus.CREATED)
            .to(-1)
            .build();

        // Then
        ArgumentCaptor<AccessPointCriteria> criteriaCaptor = ArgumentCaptor.forClass(AccessPointCriteria.class);
        verify(accessPointRepository).updateStatusByCriteria(criteriaCaptor.capture(), eq(AccessPointStatus.DELETED));
        AccessPointCriteria capturedCriteria = criteriaCaptor.getValue();
        assertThat(capturedCriteria).isEqualToComparingFieldByField(expectedAccessPointCriteria);
    }

    @Nested
    class UpdateAccessPoints {

        @ParameterizedTest
        @EnumSource(AccessPoint.ReferenceType.class)
        @SneakyThrows
        void should_update_existing_access_points_of_the_reference(AccessPoint.ReferenceType referenceType) {
            // Given
            var accessPoints = List.<AccessPoint>of();

            // When
            service.updateAccessPoints(referenceType, "ref-id", accessPoints);

            // Then
            ArgumentCaptor<AccessPointCriteria> criteriaCaptor = ArgumentCaptor.forClass(AccessPointCriteria.class);
            verify(accessPointRepository).updateStatusByCriteria(criteriaCaptor.capture(), eq(AccessPointStatus.DELETED));

            AccessPointCriteria capturedCriteria = criteriaCaptor.getValue();
            assertEquals(AccessPointReferenceType.valueOf(referenceType.name()), capturedCriteria.getReferenceType());
            assertEquals(Collections.singletonList("ref-id"), capturedCriteria.getReferenceIds());
            assertEquals(AccessPointStatus.CREATED, capturedCriteria.getStatus());
        }

        @ParameterizedTest
        @EnumSource(AccessPoint.ReferenceType.class)
        @SneakyThrows
        void should_create_access_points_provided(AccessPoint.ReferenceType referenceType) {
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
                .allSatisfy(ap -> {
                    Assertions.assertThat(ap.getId()).isNotNull();
                    Assertions.assertThat(ap.getReferenceType()).isEqualTo(AccessPointReferenceType.valueOf(referenceType.name()));
                    Assertions.assertThat(ap.getReferenceId()).isEqualTo("my-ref");
                    Assertions.assertThat(ap.getHost()).isEqualTo("my-host");
                    Assertions.assertThat(ap.isSecured()).isTrue();
                    Assertions.assertThat(ap.isOverriding()).isTrue();
                    Assertions.assertThat(ap.getStatus()).isEqualTo(AccessPointStatus.CREATED);
                });
        }

        @ParameterizedTest
        @EnumSource(AccessPoint.ReferenceType.class)
        @SneakyThrows
        void should_create_multiple_access_points(AccessPoint.ReferenceType referenceType) {
            // Given
            AccessPoint accessPoint1 = new AccessPoint();
            accessPoint1.setReferenceType(referenceType);
            accessPoint1.setReferenceId("ref-id");
            AccessPoint accessPoint2 = new AccessPoint();
            accessPoint2.setReferenceType(referenceType);
            accessPoint2.setReferenceId("ref-id");
            List<AccessPoint> accessPoints = Arrays.asList(accessPoint1, accessPoint2);

            // When
            service.updateAccessPoints(referenceType, "ref-id", accessPoints);

            // Then
            verify(accessPointRepository, times(2)).create(any(io.gravitee.repository.management.model.AccessPoint.class));
        }

        @ParameterizedTest
        @EnumSource(AccessPoint.ReferenceType.class)
        @SneakyThrows
        void testUpdateAccessPointsWithNullId(AccessPoint.ReferenceType referenceType) {
            // Given
            AccessPoint accessPoint = new AccessPoint();
            accessPoint.setReferenceType(referenceType);
            accessPoint.setReferenceId("ref-id");
            List<AccessPoint> accessPoints = Collections.singletonList(accessPoint);

            // When
            service.updateAccessPoints(referenceType, "ref-id", accessPoints);

            // Then
            ArgumentCaptor<io.gravitee.repository.management.model.AccessPoint> captor = ArgumentCaptor.forClass(
                io.gravitee.repository.management.model.AccessPoint.class
            );
            verify(accessPointRepository, times(1)).create(captor.capture());
            io.gravitee.repository.management.model.AccessPoint createdAccessPoint = captor.getValue();
            assertThat(createdAccessPoint.getId()).isEqualTo("random-id");
        }
    }
}
