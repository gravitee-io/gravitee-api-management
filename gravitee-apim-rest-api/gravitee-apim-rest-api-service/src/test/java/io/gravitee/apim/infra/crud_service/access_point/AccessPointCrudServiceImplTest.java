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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.apim.core.access_point.model.AccessPointEvent;
import io.gravitee.apim.infra.adapter.AccessPointAdapter;
import io.gravitee.common.event.EventManager;
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.api.search.AccessPointCriteria;
import io.gravitee.repository.management.model.AccessPointReferenceType;
import io.gravitee.repository.management.model.AccessPointStatus;
import io.gravitee.repository.management.model.AccessPointTarget;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.*;
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
    class Delete {

        @ParameterizedTest
        @EnumSource(AccessPoint.ReferenceType.class)
        void should_delete_existing_access_points_for_a_referenceType(AccessPoint.ReferenceType referenceType) throws Exception {
            var fetchedAccessPoints = List.of(
                io.gravitee.repository.management.model.AccessPoint.builder().id("ap1").build(),
                io.gravitee.repository.management.model.AccessPoint.builder().id("ap2").build()
            );
            var accessPointCriteria = AccessPointCriteria
                .builder()
                .referenceType(AccessPointReferenceType.valueOf(referenceType.name()))
                .referenceIds(Set.of("ref-id"))
                .status(AccessPointStatus.CREATED)
                .build();
            when(accessPointRepository.findByCriteria(accessPointCriteria, null, null)).thenReturn(fetchedAccessPoints);
            when(accessPointRepository.update(any(io.gravitee.repository.management.model.AccessPoint.class)))
                .thenReturn(io.gravitee.repository.management.model.AccessPoint.builder().build());

            var dateBeforeDeletion = Instant.now().minusSeconds(1);
            service.deleteAccessPoints(referenceType, "ref-id");
            var dateAfterDeletion = Instant.now().plusSeconds(1);

            var accessPointCaptor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.AccessPoint.class);
            verify(accessPointRepository, times(2)).update(accessPointCaptor.capture());
            accessPointCaptor
                .getAllValues()
                .forEach(ap -> {
                    assertThat(ap.getStatus()).isEqualTo(AccessPointStatus.DELETED);
                    assertThat(ap.getUpdatedAt()).isAfter(dateBeforeDeletion).isBefore(dateAfterDeletion);
                });
            verify(eventManager, times(2)).publishEvent(eq(AccessPointEvent.DELETED), any(AccessPoint.class));
        }
    }

    @Nested
    class UpdateAccessPoints {

        @ParameterizedTest
        @EnumSource(AccessPoint.ReferenceType.class)
        void should_do_nothing_if_no_access_points(AccessPoint.ReferenceType referenceType) throws Exception {
            // Given
            var accessPoints = List.<AccessPoint>of();

            // When
            service.updateAccessPoints(referenceType, "ref-id", accessPoints);

            // Then
            verify(accessPointRepository).findByCriteria(any(AccessPointCriteria.class), any(), any());
            verify(accessPointRepository, never()).update(any());
            verifyNoInteractions(eventManager);
        }

        @ParameterizedTest
        @EnumSource(AccessPoint.ReferenceType.class)
        void should_update_existing_access_points_of_the_reference(AccessPoint.ReferenceType referenceType) throws Exception {
            // Given
            var unmodifiedAccessPoint = io.gravitee.repository.management.model.AccessPoint
                .builder()
                .id("unmodified-id")
                .referenceType(AccessPointReferenceType.valueOf(referenceType.name()))
                .referenceId("ref-id")
                .target(AccessPointTarget.CONSOLE)
                .host("host-1")
                .overriding(true)
                .secured(true)
                .status(AccessPointStatus.CREATED)
                .build();
            var modifiedAccessPoint = io.gravitee.repository.management.model.AccessPoint
                .builder()
                .id("modified-id")
                .referenceType(AccessPointReferenceType.valueOf(referenceType.name()))
                .referenceId("ref-id")
                .target(AccessPointTarget.GATEWAY)
                .host("host-1")
                .overriding(true)
                .secured(true)
                .status(AccessPointStatus.CREATED)
                .build();
            var accessPoints = List.of(
                // update the host to make sure the upper case is considered as equals too
                AccessPointAdapter.INSTANCE.toEntity(unmodifiedAccessPoint).toBuilder().host("host-1".toUpperCase()).build(),
                AccessPointAdapter.INSTANCE.toEntity(modifiedAccessPoint).toBuilder().host("modified-host").build()
            );
            when(accessPointRepository.findByCriteria(any(AccessPointCriteria.class), any(), any()))
                .thenReturn(List.of(unmodifiedAccessPoint, modifiedAccessPoint));

            // When
            var dateBeforeDeletion = Instant.now().minusSeconds(1);
            service.updateAccessPoints(referenceType, "ref-id", accessPoints);
            var dateAfterDeletion = Instant.now().plusSeconds(1);

            // Then

            var createdAPsCaptor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.AccessPoint.class);
            verify(accessPointRepository).create(createdAPsCaptor.capture());
            var createdAP = createdAPsCaptor.getValue();
            assertThat(createdAP.getUpdatedAt()).isBefore(dateAfterDeletion).isAfter(dateBeforeDeletion);
            assertThat(createdAP)
                .extracting(
                    io.gravitee.repository.management.model.AccessPoint::getStatus,
                    io.gravitee.repository.management.model.AccessPoint::getHost,
                    io.gravitee.repository.management.model.AccessPoint::getTarget
                )
                .containsExactly(AccessPointStatus.CREATED, "modified-host", AccessPointTarget.GATEWAY);

            var updatedAPsCaptor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.AccessPoint.class);
            verify(accessPointRepository).update(updatedAPsCaptor.capture());
            var updatedAP = updatedAPsCaptor.getValue();
            assertThat(updatedAP.getUpdatedAt()).isBefore(dateAfterDeletion).isAfter(dateBeforeDeletion);
            assertThat(updatedAP)
                .extracting(
                    io.gravitee.repository.management.model.AccessPoint::getStatus,
                    io.gravitee.repository.management.model.AccessPoint::getHost,
                    io.gravitee.repository.management.model.AccessPoint::getTarget
                )
                .containsExactly(AccessPointStatus.DELETED, "host-1", AccessPointTarget.GATEWAY);

            verify(eventManager).publishEvent(eq(AccessPointEvent.CREATED), any());
            verify(eventManager).publishEvent(eq(AccessPointEvent.DELETED), any());
        }

        @Test
        void should_update_multiple_gateway_access_points() throws Exception {
            // Given
            var gwOne = io.gravitee.repository.management.model.AccessPoint
                .builder()
                .id("gateway-one")
                .referenceType(AccessPointReferenceType.valueOf(AccessPoint.ReferenceType.ENVIRONMENT.name()))
                .referenceId("ref-id")
                .target(AccessPointTarget.GATEWAY)
                .host("host-gateway-one")
                .status(AccessPointStatus.CREATED)
                .build();
            var gwTwo = io.gravitee.repository.management.model.AccessPoint
                .builder()
                .id("gateway-two")
                .referenceType(AccessPointReferenceType.valueOf(AccessPoint.ReferenceType.ENVIRONMENT.name()))
                .referenceId("ref-id")
                .target(AccessPointTarget.GATEWAY)
                .host("host-gateway-two")
                .status(AccessPointStatus.CREATED)
                .build();
            var accessPoints = List.of(
                AccessPointAdapter.INSTANCE.toEntity(gwOne),
                AccessPointAdapter.INSTANCE.toEntity(gwTwo).toBuilder().host("modified-gateway-two").build()
            );
            when(accessPointRepository.findByCriteria(any(AccessPointCriteria.class), any(), any())).thenReturn(List.of(gwOne, gwTwo));

            // When
            var dateBeforeDeletion = Instant.now().minusSeconds(1);
            service.updateAccessPoints(AccessPoint.ReferenceType.ENVIRONMENT, "ref-id", accessPoints);
            var dateAfterDeletion = Instant.now().plusSeconds(1);

            // Then
            var createdAPsCaptor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.AccessPoint.class);
            verify(accessPointRepository).create(createdAPsCaptor.capture());
            var createdAP = createdAPsCaptor.getValue();
            assertThat(createdAP.getUpdatedAt()).isBefore(dateAfterDeletion).isAfter(dateBeforeDeletion);
            assertThat(createdAP)
                .extracting(
                    io.gravitee.repository.management.model.AccessPoint::getStatus,
                    io.gravitee.repository.management.model.AccessPoint::getHost,
                    io.gravitee.repository.management.model.AccessPoint::getTarget
                )
                .containsExactly(AccessPointStatus.CREATED, "modified-gateway-two", AccessPointTarget.GATEWAY);

            var updatedAPsCaptor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.AccessPoint.class);
            verify(accessPointRepository).update(updatedAPsCaptor.capture());
            var updatedAP = updatedAPsCaptor.getValue();
            assertThat(updatedAP.getUpdatedAt()).isBefore(dateAfterDeletion).isAfter(dateBeforeDeletion);
            assertThat(updatedAP)
                .extracting(
                    io.gravitee.repository.management.model.AccessPoint::getStatus,
                    io.gravitee.repository.management.model.AccessPoint::getHost,
                    io.gravitee.repository.management.model.AccessPoint::getTarget
                )
                .containsExactly(AccessPointStatus.DELETED, "host-gateway-two", AccessPointTarget.GATEWAY);

            verify(eventManager).publishEvent(eq(AccessPointEvent.CREATED), any());
            verify(eventManager).publishEvent(eq(AccessPointEvent.DELETED), any());
        }

        @ParameterizedTest
        @EnumSource(AccessPoint.ReferenceType.class)
        void should_create_access_points_for_first_time(AccessPoint.ReferenceType referenceType) throws Exception {
            // Given
            when(accessPointRepository.findByCriteria(any(AccessPointCriteria.class), any(), any())).thenReturn(List.of());

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
            var dateBeforeDeletion = Instant.now().minusSeconds(1);
            service.updateAccessPoints(referenceType, "ref-id", accessPoints);
            var dateAfterDeletion = Instant.now().plusSeconds(1);

            // Then
            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.AccessPoint.class);
            verify(accessPointRepository, times(accessPoints.size())).create(captor.capture());

            assertThat(captor.getAllValues())
                .hasSameSizeAs(accessPoints)
                .allSatisfy(ap -> {
                    assertThat(ap.getId()).isNotNull();
                    assertThat(ap.getReferenceType()).isEqualTo(AccessPointReferenceType.valueOf(referenceType.name()));
                    assertThat(ap.getReferenceId()).isEqualTo("my-ref");
                    assertThat(ap.getHost()).isEqualTo("my-host");
                    assertThat(ap.isSecured()).isTrue();
                    assertThat(ap.isOverriding()).isTrue();
                    assertThat(ap.getStatus()).isEqualTo(AccessPointStatus.CREATED);
                    assertThat(ap.getUpdatedAt()).isAfter(dateBeforeDeletion).isBefore(dateAfterDeletion);
                });

            verify(eventManager, times(5)).publishEvent(eq(AccessPointEvent.CREATED), any());
        }

        @ParameterizedTest
        @EnumSource(AccessPoint.ReferenceType.class)
        void should_not_update_access_points_if_nothing_changed(AccessPoint.ReferenceType referenceType) throws Exception {
            // Given
            var ap1 = AccessPoint
                .builder()
                .referenceType(referenceType)
                .referenceId("ref-id")
                .target(AccessPoint.Target.CONSOLE)
                .host("host-console")
                .build();
            var ap2 = AccessPoint
                .builder()
                .referenceType(referenceType)
                .referenceId("ref-id")
                .target(AccessPoint.Target.GATEWAY)
                .host("host-gateway")
                .build();
            when(accessPointRepository.findByCriteria(any(AccessPointCriteria.class), any(), any()))
                .thenReturn(List.of(AccessPointAdapter.INSTANCE.fromEntity(ap1), AccessPointAdapter.INSTANCE.fromEntity(ap2)));

            var accessPoints = List.of(ap1, ap2);

            // When
            service.updateAccessPoints(referenceType, "ref-id", accessPoints);

            // Then
            verify(accessPointRepository, never()).create(any(io.gravitee.repository.management.model.AccessPoint.class));
            verifyNoInteractions(eventManager);
        }

        @ParameterizedTest
        @EnumSource(AccessPoint.ReferenceType.class)
        void should_update_access_points_with_null_id(AccessPoint.ReferenceType referenceType) throws Exception {
            // Given
            var accessPoints = List.of(AccessPoint.builder().referenceType(referenceType).referenceId("ref-id").build());

            // When
            service.updateAccessPoints(referenceType, "ref-id", accessPoints);

            // Then
            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.AccessPoint.class);
            verify(accessPointRepository).create(captor.capture());
            var createdAccessPoint = captor.getValue();
            assertThat(createdAccessPoint.getId()).isEqualTo("random-id");
        }
    }
}
