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
package io.gravitee.apim.infra.crud_service.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.audit.crud_service.AuditCrudService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.event.SubscriptionAuditEvent;
import io.gravitee.apim.infra.crud_service.user.UserCrudServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.User;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuditCrudServiceImplTest {

    @Mock
    AuditRepository auditRepository;

    @Captor
    ArgumentCaptor<Audit> auditCaptor;

    AuditCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuditCrudServiceImpl(auditRepository);
    }

    @Nested
    class Create {

        @Test
        void should_create_an_audit() throws TechnicalException {
            // Given

            // When
            service.create(
                AuditEntity.builder()
                    .id("audit-id")
                    .organizationId("organization-id")
                    .environmentId("environment-id")
                    .referenceId("reference-id")
                    .referenceType(AuditEntity.AuditReferenceType.API)
                    .user("system")
                    .properties(Map.of("key", "value"))
                    .event("event")
                    .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC))
                    .patch("[]")
                    .build()
            );

            // Then
            var expectedAudit = Audit.builder()
                .id("audit-id")
                .organizationId("organization-id")
                .environmentId("environment-id")
                .createdAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                .user("system")
                .properties(Map.of("key", "value"))
                .referenceType(Audit.AuditReferenceType.API)
                .referenceId("reference-id")
                .event("event")
                .patch("[]")
                .build();

            verify(auditRepository).create(auditCaptor.capture());
            Assertions.assertThat(auditCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedAudit);
        }

        @Test
        void should_throw_when_create_fails() throws TechnicalException {
            // Given
            when(auditRepository.create(any())).thenThrow(new TechnicalException("technical exception"));

            // When
            Throwable throwable = catchThrowable(() ->
                service.create(
                    AuditEntity.builder()
                        .referenceType(AuditEntity.AuditReferenceType.API)
                        .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC))
                        .build()
                )
            );

            // Then
            assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("technical exception");
        }
    }
}
