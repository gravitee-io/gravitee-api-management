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
package io.gravitee.apim.core.api.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.AuditInfoFixtures;
import inmemory.AuditCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.MetadataCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.impl.upgrade.initializer.DefaultMetadataInitializer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiMetadataDomainServiceTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String API_ID = "my-api";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    MetadataCrudServiceInMemory metadataCrudService = new MetadataCrudServiceInMemory();

    ApiMetadataDomainService service;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        service =
            new ApiMetadataDomainService(
                metadataCrudService,
                new AuditDomainService(auditCrudService, new UserCrudServiceInMemory(), new JacksonJsonDiffProcessor())
            );
    }

    @AfterEach
    void tearDown() {
        Stream.of(metadataCrudService).forEach(InMemoryAlternative::reset);
    }

    @Nested
    class CreateDefaultApiMetadata {

        @Test
        void should_create_email_support_metadata() {
            service.createDefaultApiMetadata(API_ID, AUDIT_INFO);

            Assertions
                .assertThat(metadataCrudService.storage())
                .contains(
                    Metadata
                        .builder()
                        .key("email-support")
                        .format(Metadata.MetadataFormat.MAIL)
                        .name(DefaultMetadataInitializer.METADATA_EMAIL_SUPPORT_KEY)
                        .value("${(api.primaryOwner.email)!''}")
                        .referenceType(Metadata.ReferenceType.API)
                        .referenceId(API_ID)
                        .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .build()
                );
        }

        @Test
        void should_create_an_audit() {
            service.createDefaultApiMetadata(API_ID, AUDIT_INFO);

            assertThat(auditCrudService.storage())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
                .containsExactly(
                    AuditEntity
                        .builder()
                        .id("generated-id")
                        .organizationId(ORGANIZATION_ID)
                        .environmentId(ENVIRONMENT_ID)
                        .referenceType(AuditEntity.AuditReferenceType.API)
                        .referenceId(API_ID)
                        .user(USER_ID)
                        .properties(Map.of("METADATA", "email-support"))
                        .event(ApiAuditEvent.METADATA_CREATED.name())
                        .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .build()
                );
        }
    }
}
