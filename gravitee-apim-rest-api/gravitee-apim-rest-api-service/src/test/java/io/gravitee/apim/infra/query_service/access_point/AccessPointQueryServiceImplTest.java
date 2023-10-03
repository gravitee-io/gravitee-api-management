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
package io.gravitee.apim.infra.query_service.access_point;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.access_point.model.RestrictedDomainEntity;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.model.AccessPoint;
import io.gravitee.repository.management.model.AccessPointReferenceType;
import io.gravitee.repository.management.model.AccessPointTarget;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class AccessPointQueryServiceImplTest {

    AccessPointRepository accessPointRepository;
    AccessPointQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        accessPointRepository = mock(AccessPointRepository.class);
        service = new AccessPointQueryServiceImpl(accessPointRepository);
    }

    @Nested
    class GetReferenceContext {

        @Test
        void should_return_reference_context_when_access_point_match_the_host_provided() throws TechnicalException {
            // Given
            when(accessPointRepository.findByHost("my-host"))
                .thenReturn(
                    Optional.of(AccessPoint.builder().referenceId("ref-id").referenceType(AccessPointReferenceType.ORGANIZATION).build())
                );

            // When
            var result = service.getReferenceContext("my-host");

            // Then
            Assertions
                .assertThat(result)
                .contains(ReferenceContext.builder().referenceId("ref-id").referenceType(ReferenceContext.Type.ORGANIZATION).build());
        }

        @Test
        void should_return_empty_when_no_access_point_match_the_host_provided() throws TechnicalException {
            // Given
            when(accessPointRepository.findByHost("my-host")).thenReturn(Optional.empty());

            // When
            var result = service.getReferenceContext("my-host");

            // Then
            Assertions.assertThat(result).isEmpty();
        }

        @Test
        void should_return_empty_when_exception_rose() throws TechnicalException {
            // Given
            when(accessPointRepository.findByHost("my-host")).thenThrow(new TechnicalException("error"));

            // When
            var result = service.getReferenceContext("my-host");

            // Then
            Assertions.assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetConsoleUrl {

        @Test
        void should_return_http_url_matching_the_only_access_point_of_organization() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("org-id")
                        .referenceType(AccessPointReferenceType.ORGANIZATION)
                        .target(AccessPointTarget.CONSOLE)
                        .host("my-host")
                        .build()
                )
            );

            // When
            var result = service.getConsoleUrl("org-id");

            // Then
            Assertions.assertThat(result).isEqualTo("http://my-host");
        }

        @Test
        void should_return_https_url_when_access_point_is_secured() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("org-id")
                        .referenceType(AccessPointReferenceType.ORGANIZATION)
                        .target(AccessPointTarget.CONSOLE)
                        .host("my-host")
                        .secured(true)
                        .build()
                )
            );

            // When
            var result = service.getConsoleUrl("org-id");

            // Then
            Assertions.assertThat(result).isEqualTo("https://my-host");
        }

        @Test
        void should_return_http_url_matching_the_first_overridden_access_point_of_organization() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("org-id")
                        .referenceType(AccessPointReferenceType.ORGANIZATION)
                        .target(AccessPointTarget.CONSOLE)
                        .host("my-host")
                        .build(),
                    AccessPoint
                        .builder()
                        .referenceId("org-id")
                        .referenceType(AccessPointReferenceType.ORGANIZATION)
                        .target(AccessPointTarget.CONSOLE)
                        .host("override-1")
                        .overriding(true)
                        .build(),
                    AccessPoint
                        .builder()
                        .referenceId("org-id")
                        .referenceType(AccessPointReferenceType.ORGANIZATION)
                        .target(AccessPointTarget.CONSOLE)
                        .host("override-2")
                        .overriding(true)
                        .build()
                )
            );

            // When
            var result = service.getConsoleUrl("org-id");

            // Then
            Assertions.assertThat(result).isEqualTo("http://override-1");
        }

        @Test
        void should_return_null_when_no_access_point_found() {
            // Given
            givenAccessPoints(List.of());

            // When
            var result = service.getConsoleUrl("org-id");

            // Then
            Assertions.assertThat(result).isNull();
        }

        @Test
        void should_throw_when_exception_rose() throws TechnicalException {
            // Given
            when(accessPointRepository.findByReferenceAndTarget(any(), any(), any())).thenThrow(new TechnicalException("error"));

            // When
            var result = Assertions.catchThrowable(() -> service.getConsoleUrl("org-id"));

            // Then
            Assertions.assertThat(result).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("org-id");
        }
    }

    @Nested
    class GetConsoleUrls {

        @Test
        void should_return_http_urls_matching_the_only_access_point_of_organization() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("org-id")
                        .referenceType(AccessPointReferenceType.ORGANIZATION)
                        .target(AccessPointTarget.CONSOLE)
                        .host("my-host")
                        .build()
                )
            );

            // When
            var result = service.getConsoleUrls("org-id", true);

            // Then
            Assertions.assertThat(result).containsOnly("http://my-host");
        }

        @Test
        void should_return_https_urls_when_access_point_is_secured() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("org-id")
                        .referenceType(AccessPointReferenceType.ORGANIZATION)
                        .target(AccessPointTarget.CONSOLE)
                        .host("my-host")
                        .secured(true)
                        .build()
                )
            );

            // When
            var result = service.getConsoleUrls("org-id", true);

            // Then
            Assertions.assertThat(result).containsOnly("https://my-host");
        }

        @Test
        void should_return_http_urls_matching_access_point_of_organization() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("org-id")
                        .referenceType(AccessPointReferenceType.ORGANIZATION)
                        .target(AccessPointTarget.CONSOLE)
                        .host("my-host")
                        .build(),
                    AccessPoint
                        .builder()
                        .referenceId("org-id")
                        .referenceType(AccessPointReferenceType.ORGANIZATION)
                        .target(AccessPointTarget.CONSOLE)
                        .host("override-1")
                        .overriding(true)
                        .build(),
                    AccessPoint
                        .builder()
                        .referenceId("org-id")
                        .referenceType(AccessPointReferenceType.ORGANIZATION)
                        .target(AccessPointTarget.CONSOLE)
                        .host("override-2")
                        .overriding(true)
                        .build()
                )
            );

            // When
            var result = service.getConsoleUrls("org-id", true);

            // Then
            Assertions.assertThat(result).containsOnly("http://my-host", "http://override-1", "http://override-2");
        }

        @Test
        void should_return_only_http_urls_overriding_access_point_of_organization() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("org-id")
                        .referenceType(AccessPointReferenceType.ORGANIZATION)
                        .target(AccessPointTarget.CONSOLE)
                        .host("my-host")
                        .build(),
                    AccessPoint
                        .builder()
                        .referenceId("org-id")
                        .referenceType(AccessPointReferenceType.ORGANIZATION)
                        .target(AccessPointTarget.CONSOLE)
                        .host("override-1")
                        .overriding(true)
                        .build(),
                    AccessPoint
                        .builder()
                        .referenceId("org-id")
                        .referenceType(AccessPointReferenceType.ORGANIZATION)
                        .target(AccessPointTarget.CONSOLE)
                        .host("override-2")
                        .overriding(true)
                        .build()
                )
            );

            // When
            var result = service.getConsoleUrls("org-id", false);

            // Then
            Assertions.assertThat(result).containsOnly("http://override-1", "http://override-2");
        }

        @Test
        void should_return_null_when_no_access_point_found() {
            // Given
            givenAccessPoints(List.of());

            // When
            var result = service.getConsoleUrls("org-id", true);

            // Then
            Assertions.assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_exception_rose() throws TechnicalException {
            // Given
            when(accessPointRepository.findByReferenceAndTarget(any(), any(), any())).thenThrow(new TechnicalException("error"));

            // When
            var result = Assertions.catchThrowable(() -> service.getConsoleUrls("org-id", true));

            // Then
            Assertions.assertThat(result).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("org-id");
        }
    }

    @Nested
    class GetConsoleApiUrl {

        @Test
        void should_return_http_url_matching_the_only_access_point_of_organization() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("org-id")
                        .referenceType(AccessPointReferenceType.ORGANIZATION)
                        .target(AccessPointTarget.CONSOLE_API)
                        .host("my-host")
                        .build()
                )
            );

            // When
            var result = service.getConsoleApiUrl("org-id");

            // Then
            Assertions.assertThat(result).isEqualTo("http://my-host");
        }

        @Test
        void should_return_https_url_when_access_point_is_secured() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("org-id")
                        .referenceType(AccessPointReferenceType.ORGANIZATION)
                        .target(AccessPointTarget.CONSOLE_API)
                        .host("my-host")
                        .secured(true)
                        .build()
                )
            );

            // When
            var result = service.getConsoleApiUrl("org-id");

            // Then
            Assertions.assertThat(result).isEqualTo("https://my-host");
        }

        @Test
        void should_return_http_url_matching_the_first_overridden_access_point_of_organization() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("org-id")
                        .referenceType(AccessPointReferenceType.ORGANIZATION)
                        .target(AccessPointTarget.CONSOLE_API)
                        .host("my-host")
                        .build(),
                    AccessPoint
                        .builder()
                        .referenceId("org-id")
                        .referenceType(AccessPointReferenceType.ORGANIZATION)
                        .target(AccessPointTarget.CONSOLE_API)
                        .host("override-1")
                        .overriding(true)
                        .build(),
                    AccessPoint
                        .builder()
                        .referenceId("org-id")
                        .referenceType(AccessPointReferenceType.ORGANIZATION)
                        .target(AccessPointTarget.CONSOLE_API)
                        .host("override-2")
                        .overriding(true)
                        .build()
                )
            );

            // When
            var result = service.getConsoleApiUrl("org-id");

            // Then
            Assertions.assertThat(result).isEqualTo("http://override-1");
        }

        @Test
        void should_return_null_when_no_access_point_found() {
            // Given
            givenAccessPoints(List.of());

            // When
            var result = service.getConsoleApiUrl("org-id");

            // Then
            Assertions.assertThat(result).isNull();
        }

        @Test
        void should_throw_when_exception_rose() throws TechnicalException {
            // Given
            when(accessPointRepository.findByReferenceAndTarget(any(), any(), any())).thenThrow(new TechnicalException("error"));

            // When
            var result = Assertions.catchThrowable(() -> service.getConsoleApiUrl("org-id"));

            // Then
            Assertions.assertThat(result).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("org-id");
        }
    }

    @Nested
    class GetPortalUrl {

        @Test
        void should_return_http_url_matching_the_only_access_point_of_environment() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.PORTAL)
                        .host("my-host")
                        .build()
                )
            );

            // When
            var result = service.getPortalUrl("env-id");

            // Then
            Assertions.assertThat(result).isEqualTo("http://my-host");
        }

        @Test
        void should_return_https_url_when_access_point_is_secured() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.PORTAL)
                        .host("my-host")
                        .secured(true)
                        .build()
                )
            );

            // When
            var result = service.getPortalUrl("env-id");

            // Then
            Assertions.assertThat(result).isEqualTo("https://my-host");
        }

        @Test
        void should_return_http_url_matching_the_first_overridden_access_point_of_environment() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.PORTAL)
                        .host("my-host")
                        .build(),
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.PORTAL)
                        .host("override-1")
                        .overriding(true)
                        .build(),
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.PORTAL)
                        .host("override-2")
                        .overriding(true)
                        .build()
                )
            );

            // When
            var result = service.getPortalUrl("env-id");

            // Then
            Assertions.assertThat(result).isEqualTo("http://override-1");
        }

        @Test
        void should_return_null_when_no_access_point_found() {
            // Given
            givenAccessPoints(List.of());

            // When
            var result = service.getPortalUrl("env-id");

            // Then
            Assertions.assertThat(result).isNull();
        }

        @Test
        void should_throw_when_exception_rose() throws TechnicalException {
            // Given
            when(accessPointRepository.findByReferenceAndTarget(any(), any(), any())).thenThrow(new TechnicalException("error"));

            // When
            var result = Assertions.catchThrowable(() -> service.getPortalUrl("env-id"));

            // Then
            Assertions.assertThat(result).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("env-id");
        }
    }

    @Nested
    class GetPortalUrls {

        @Test
        void should_return_http_urls_matching_the_only_access_point_of_environment() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.PORTAL)
                        .host("my-host")
                        .build()
                )
            );

            // When
            var result = service.getPortalUrls("env-id", true);

            // Then
            Assertions.assertThat(result).containsOnly("http://my-host");
        }

        @Test
        void should_return_https_urls_when_access_point_is_secured() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.PORTAL)
                        .host("my-host")
                        .secured(true)
                        .build()
                )
            );

            // When
            var result = service.getPortalUrls("env-id", true);

            // Then
            Assertions.assertThat(result).containsOnly("https://my-host");
        }

        @Test
        void should_return_http_urls_matching_access_point_of_environment() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.PORTAL)
                        .host("my-host")
                        .build(),
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.PORTAL)
                        .host("override-1")
                        .overriding(true)
                        .build(),
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.PORTAL)
                        .host("override-2")
                        .overriding(true)
                        .build()
                )
            );

            // When
            var result = service.getPortalUrls("env-id", true);

            // Then
            Assertions.assertThat(result).containsOnly("http://my-host", "http://override-1", "http://override-2");
        }

        @Test
        void should_return_only_http_urls_overriding_access_point_of_environment() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.PORTAL)
                        .host("my-host")
                        .build(),
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.PORTAL)
                        .host("override-1")
                        .overriding(true)
                        .build(),
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.PORTAL)
                        .host("override-2")
                        .overriding(true)
                        .build()
                )
            );

            // When
            var result = service.getPortalUrls("env-id", false);

            // Then
            Assertions.assertThat(result).containsOnly("http://override-1", "http://override-2");
        }

        @Test
        void should_return_null_when_no_access_point_found() {
            // Given
            givenAccessPoints(List.of());

            // When
            var result = service.getPortalUrls("env-id", true);

            // Then
            Assertions.assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_exception_rose() throws TechnicalException {
            // Given
            when(accessPointRepository.findByReferenceAndTarget(any(), any(), any())).thenThrow(new TechnicalException("error"));

            // When
            var result = Assertions.catchThrowable(() -> service.getPortalUrls("env-id", true));

            // Then
            Assertions.assertThat(result).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("env-id");
        }
    }

    @Nested
    class GetPortalApiUrl {

        @Test
        void should_return_http_url_matching_the_only_access_point_of_environment() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.PORTAL_API)
                        .host("my-host")
                        .build()
                )
            );

            // When
            var result = service.getPortalApiUrl("env-id");

            // Then
            Assertions.assertThat(result).isEqualTo("http://my-host");
        }

        @Test
        void should_return_https_url_when_access_point_is_secured() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.PORTAL_API)
                        .host("my-host")
                        .secured(true)
                        .build()
                )
            );

            // When
            var result = service.getPortalApiUrl("env-id");

            // Then
            Assertions.assertThat(result).isEqualTo("https://my-host");
        }

        @Test
        void should_return_http_url_matching_the_first_overridden_access_point_of_environment() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.PORTAL_API)
                        .host("my-host")
                        .build(),
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.PORTAL_API)
                        .host("override-1")
                        .overriding(true)
                        .build(),
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.PORTAL_API)
                        .host("override-2")
                        .overriding(true)
                        .build()
                )
            );

            // When
            var result = service.getPortalApiUrl("env-id");

            // Then
            Assertions.assertThat(result).isEqualTo("http://override-1");
        }

        @Test
        void should_return_null_when_no_access_point_found() {
            // Given
            givenAccessPoints(List.of());

            // When
            var result = service.getPortalApiUrl("env-id");

            // Then
            Assertions.assertThat(result).isNull();
        }

        @Test
        void should_throw_when_exception_rose() throws TechnicalException {
            // Given
            when(accessPointRepository.findByReferenceAndTarget(any(), any(), any())).thenThrow(new TechnicalException("error"));

            // When
            var result = Assertions.catchThrowable(() -> service.getPortalApiUrl("env-id"));

            // Then
            Assertions.assertThat(result).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("env-id");
        }
    }

    @Nested
    class GetGatewayRestrictedDomains {

        @Test
        void should_return_restricted_domain_matching_the_only_access_point_of_organization() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.GATEWAY)
                        .host("my-host")
                        .build()
                )
            );

            // When
            var result = service.getGatewayRestrictedDomains("env-id");

            // Then
            Assertions.assertThat(result).containsExactly(RestrictedDomainEntity.builder().domain("my-host").build());
        }

        @Test
        void should_return_all_restricted_domain_matching_overridden_access_points_of_environment() {
            // Given
            givenAccessPoints(
                List.of(
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.GATEWAY)
                        .host("my-host")
                        .build(),
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.GATEWAY)
                        .host("override-1")
                        .overriding(true)
                        .build(),
                    AccessPoint
                        .builder()
                        .referenceId("env-id")
                        .referenceType(AccessPointReferenceType.ENVIRONMENT)
                        .target(AccessPointTarget.GATEWAY)
                        .host("override-2")
                        .overriding(true)
                        .secured(true)
                        .build()
                )
            );

            // When
            var result = service.getGatewayRestrictedDomains("env-id");

            // Then
            Assertions
                .assertThat(result)
                .containsExactly(
                    RestrictedDomainEntity.builder().domain("override-1").build(),
                    RestrictedDomainEntity.builder().domain("override-2").secured(true).build()
                );
        }

        @Test
        void should_return_empty_list_when_no_access_point_found() {
            // Given
            givenAccessPoints(List.of());

            // When
            var result = service.getGatewayRestrictedDomains("env-id");

            // Then
            Assertions.assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_exception_rose() throws TechnicalException {
            // Given
            when(accessPointRepository.findByReferenceAndTarget(any(), any(), any())).thenThrow(new TechnicalException("error"));

            // When
            var result = Assertions.catchThrowable(() -> service.getGatewayRestrictedDomains("env-id"));

            // Then
            Assertions.assertThat(result).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("env-id");
        }
    }

    @SneakyThrows
    void givenAccessPoints(List<AccessPoint> accessPoints) {
        if (!accessPoints.isEmpty()) {
            when(
                accessPointRepository.findByReferenceAndTarget(
                    eq(accessPoints.get(0).getReferenceType()),
                    eq(accessPoints.get(0).getReferenceId()),
                    eq(accessPoints.get(0).getTarget())
                )
            )
                .thenReturn(accessPoints);
        }
    }
}
