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
package io.gravitee.apim.core.audit.domain_service;

import inmemory.AuditCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.ApiProductAuditLogEntity;
import io.gravitee.apim.core.audit.model.ApplicationAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.apim.core.audit.model.event.ApiProductAuditEvent;
import io.gravitee.apim.core.audit.model.event.SubscriptionAuditEvent;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupAuditEvent;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class AuditDomainServiceTest {

    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();

    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();

    AuditDomainService service = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "audit-id");
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
    }

    @AfterEach
    void tearDown() {
        Stream.of(auditCrudService, userCrudService).forEach(InMemoryAlternative::reset);
    }

    @Nested
    class CreateApiAuditLog {

        @Test
        void should_create_an_api_audit_log() {
            var audit = ApiAuditLogEntity.builder()
                .apiId("api-id")
                .organizationId("organization-id")
                .environmentId("environment-id")
                .actor(AuditActor.builder().userId("system").build())
                .event(SubscriptionAuditEvent.SUBSCRIPTION_CLOSED)
                .properties(Map.of(AuditProperties.API, "api-id"))
                .oldValue(null)
                .newValue(null)
                .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.of("UTC")))
                .build();

            service.createApiAuditLog(audit);

            var expectedAudit = AuditEntity.builder()
                .id("audit-id")
                .organizationId("organization-id")
                .environmentId("environment-id")
                .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.of("UTC")))
                .user("system")
                .properties(Map.of("API", "api-id"))
                .referenceType(AuditEntity.AuditReferenceType.API)
                .referenceId("api-id")
                .event(SubscriptionAuditEvent.SUBSCRIPTION_CLOSED.name())
                .patch("[]")
                .build();

            Assertions.assertThat(auditCrudService.storage()).containsOnly(expectedAudit);
        }

        @Test
        void should_calculate_json_patch() {
            SubscriptionEntity originalSubscription = SubscriptionEntity.builder()
                .id("sub-id")
                .apiId("api-id")
                .status(SubscriptionEntity.Status.PENDING)
                .requestMessage("request-message")
                .build();
            var audit = ApiAuditLogEntity.builder()
                .apiId("api-id")
                .organizationId("organization-id")
                .environmentId("environment-id")
                .actor(AuditActor.builder().userId("system").build())
                .event(SubscriptionAuditEvent.SUBSCRIPTION_UPDATED)
                .properties(Map.of(AuditProperties.API, "api-id"))
                .oldValue(originalSubscription)
                .newValue(originalSubscription.toBuilder().status(SubscriptionEntity.Status.ACCEPTED).reasonMessage("accepted").build())
                .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.of("UTC")))
                .build();

            service.createApiAuditLog(audit);

            var expectedAudit = AuditEntity.builder()
                .id("audit-id")
                .organizationId("organization-id")
                .environmentId("environment-id")
                .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.of("UTC")))
                .user("system")
                .properties(Map.of("API", "api-id"))
                .referenceType(AuditEntity.AuditReferenceType.API)
                .referenceId("api-id")
                .event(SubscriptionAuditEvent.SUBSCRIPTION_UPDATED.name())
                .patch(
                    "[{\"op\":\"add\",\"path\":\"/reasonMessage\",\"value\":\"accepted\"},{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"ACCEPTED\"}]"
                )
                .build();

            Assertions.assertThat(auditCrudService.storage()).containsOnly(expectedAudit);
        }

        @Test
        void should_fetch_user_display_name_when_using_a_token() {
            userCrudService.initWith(List.of(BaseUserEntity.builder().id("user-id").firstname("Jane").lastname("Doe").build()));

            var audit = ApiAuditLogEntity.builder()
                .apiId("api-id")
                .organizationId("organization-id")
                .environmentId("environment-id")
                .actor(AuditActor.builder().userId("user-id").userSource("token").userSourceId("source-id").build())
                .event(SubscriptionAuditEvent.SUBSCRIPTION_UPDATED)
                .properties(Map.of(AuditProperties.API, "api-id"))
                .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.of("UTC")))
                .build();

            service.createApiAuditLog(audit);

            var expectedAudit = AuditEntity.builder()
                .id("audit-id")
                .organizationId("organization-id")
                .environmentId("environment-id")
                .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.of("UTC")))
                .user("Jane Doe - (using token \"source-id\")")
                .properties(Map.of("API", "api-id"))
                .referenceType(AuditEntity.AuditReferenceType.API)
                .referenceId("api-id")
                .event(SubscriptionAuditEvent.SUBSCRIPTION_UPDATED.name())
                .patch("[]")
                .build();

            Assertions.assertThat(auditCrudService.storage()).containsOnly(expectedAudit);
        }
    }

    @Nested
    class CreateApplicationAuditLog {

        @Test
        void should_create_an_application_audit_log() {
            var audit = ApplicationAuditLogEntity.builder()
                .applicationId("application-id")
                .organizationId("organization-id")
                .environmentId("environment-id")
                .actor(AuditActor.builder().userId("system").build())
                .event(SubscriptionAuditEvent.SUBSCRIPTION_CLOSED)
                .properties(Map.of(AuditProperties.API, "api-id"))
                .oldValue(null)
                .newValue(null)
                .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.of("UTC")))
                .build();

            service.createApplicationAuditLog(audit);

            var expectedAudit = AuditEntity.builder()
                .id("audit-id")
                .organizationId("organization-id")
                .environmentId("environment-id")
                .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.of("UTC")))
                .user("system")
                .properties(Map.of("API", "api-id"))
                .referenceType(AuditEntity.AuditReferenceType.APPLICATION)
                .referenceId("application-id")
                .event(SubscriptionAuditEvent.SUBSCRIPTION_CLOSED.name())
                .patch("[]")
                .build();

            Assertions.assertThat(auditCrudService.storage()).hasSize(1).containsOnly(expectedAudit);
        }

        @Test
        void should_calculate_json_patch() {
            SubscriptionEntity originalSubscription = SubscriptionEntity.builder()
                .id("sub-id")
                .apiId("api-id")
                .status(SubscriptionEntity.Status.PENDING)
                .requestMessage("request-message")
                .build();
            var audit = ApplicationAuditLogEntity.builder()
                .applicationId("application-id")
                .organizationId("organization-id")
                .environmentId("environment-id")
                .actor(AuditActor.builder().userId("system").build())
                .event(SubscriptionAuditEvent.SUBSCRIPTION_UPDATED)
                .properties(Map.of(AuditProperties.API, "api-id"))
                .oldValue(originalSubscription)
                .newValue(originalSubscription.toBuilder().status(SubscriptionEntity.Status.ACCEPTED).reasonMessage("accepted").build())
                .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.of("UTC")))
                .build();

            service.createApplicationAuditLog(audit);

            var expectedAudit = AuditEntity.builder()
                .id("audit-id")
                .organizationId("organization-id")
                .environmentId("environment-id")
                .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.of("UTC")))
                .user("system")
                .properties(Map.of("API", "api-id"))
                .referenceType(AuditEntity.AuditReferenceType.APPLICATION)
                .referenceId("application-id")
                .event(SubscriptionAuditEvent.SUBSCRIPTION_UPDATED.name())
                .patch(
                    "[{\"op\":\"add\",\"path\":\"/reasonMessage\",\"value\":\"accepted\"},{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"ACCEPTED\"}]"
                )
                .build();

            Assertions.assertThat(auditCrudService.storage()).containsOnly(expectedAudit);
        }

        @Test
        void should_fetch_user_display_name_when_using_a_token() {
            userCrudService.initWith(List.of(BaseUserEntity.builder().id("user-id").firstname("Jane").lastname("Doe").build()));

            var audit = ApplicationAuditLogEntity.builder()
                .applicationId("application-id")
                .organizationId("organization-id")
                .environmentId("environment-id")
                .actor(AuditActor.builder().userId("user-id").userSource("token").userSourceId("source-id").build())
                .event(SubscriptionAuditEvent.SUBSCRIPTION_UPDATED)
                .properties(Map.of(AuditProperties.API, "api-id"))
                .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.of("UTC")))
                .build();

            service.createApplicationAuditLog(audit);

            var expectedAudit = AuditEntity.builder()
                .id("audit-id")
                .organizationId("organization-id")
                .environmentId("environment-id")
                .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.of("UTC")))
                .user("Jane Doe - (using token \"source-id\")")
                .properties(Map.of("API", "api-id"))
                .referenceType(AuditEntity.AuditReferenceType.APPLICATION)
                .referenceId("application-id")
                .event(SubscriptionAuditEvent.SUBSCRIPTION_UPDATED.name())
                .patch("[]")
                .build();

            Assertions.assertThat(auditCrudService.storage()).containsOnly(expectedAudit);
        }
    }

    @Nested
    class CreateApiProductAuditLog {

        @Test
        void should_create_an_api_product_audit_log() {
            var audit = ApiProductAuditLogEntity.builder()
                .apiProductId("api-product-id")
                .organizationId("organization-id")
                .environmentId("environment-id")
                .actor(AuditActor.builder().userId("system").build())
                .event(ApiProductAuditEvent.API_PRODUCT_CREATED)
                .properties(Map.of(AuditProperties.API_PRODUCT, "api-product-id"))
                .oldValue(null)
                .newValue(null)
                .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.of("UTC")))
                .build();

            service.createApiProductAuditLog(audit);

            var expectedAudit = AuditEntity.builder()
                .id("audit-id")
                .organizationId("organization-id")
                .environmentId("environment-id")
                .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.of("UTC")))
                .user("system")
                .properties(Map.of("API_PRODUCT", "api-product-id"))
                .referenceType(AuditEntity.AuditReferenceType.API_PRODUCT)
                .referenceId("api-product-id")
                .event(ApiProductAuditEvent.API_PRODUCT_CREATED.name())
                .patch("[]")
                .build();

            Assertions.assertThat(auditCrudService.storage()).containsOnly(expectedAudit);
        }

        @Test
        void should_calculate_json_patch() {
            ApiProduct originalApiProduct = ApiProduct.builder()
                .id("api-product-id")
                .environmentId("environment-id")
                .name("Original Product")
                .description("Original description")
                .version("1.0.0")
                .apiIds(Set.of("api-1"))
                .build();

            var audit = ApiProductAuditLogEntity.builder()
                .apiProductId("api-product-id")
                .organizationId("organization-id")
                .environmentId("environment-id")
                .actor(AuditActor.builder().userId("system").build())
                .event(ApiProductAuditEvent.API_PRODUCT_UPDATED)
                .properties(Map.of(AuditProperties.API_PRODUCT, "api-product-id"))
                .oldValue(originalApiProduct)
                .newValue(originalApiProduct.toBuilder().name("Updated Product").apiIds(Set.of("api-1", "api-2")).build())
                .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.of("UTC")))
                .build();

            service.createApiProductAuditLog(audit);

            var audits = auditCrudService.storage();
            Assertions.assertThat(audits).hasSize(1);

            var actualAudit = audits.iterator().next();
            Assertions.assertThat(actualAudit.getId()).isEqualTo("audit-id");
            Assertions.assertThat(actualAudit.getOrganizationId()).isEqualTo("organization-id");
            Assertions.assertThat(actualAudit.getEnvironmentId()).isEqualTo("environment-id");
            Assertions.assertThat(actualAudit.getUser()).isEqualTo("system");
            Assertions.assertThat(actualAudit.getProperties()).isEqualTo(Map.of("API_PRODUCT", "api-product-id"));
            Assertions.assertThat(actualAudit.getReferenceType()).isEqualTo(AuditEntity.AuditReferenceType.API_PRODUCT);
            Assertions.assertThat(actualAudit.getReferenceId()).isEqualTo("api-product-id");
            Assertions.assertThat(actualAudit.getEvent()).isEqualTo(ApiProductAuditEvent.API_PRODUCT_UPDATED.name());

            Assertions.assertThat(actualAudit.getPatch()).contains("\"path\":\"/name\"", "\"value\":\"Updated Product\"");
            Assertions.assertThat(actualAudit.getPatch()).contains("api-2");
        }

        @Test
        void should_fetch_user_display_name_when_using_a_token() {
            userCrudService.initWith(List.of(BaseUserEntity.builder().id("user-id").firstname("Jane").lastname("Doe").build()));

            var audit = ApiProductAuditLogEntity.builder()
                .apiProductId("api-product-id")
                .organizationId("organization-id")
                .environmentId("environment-id")
                .actor(AuditActor.builder().userId("user-id").userSource("token").userSourceId("source-id").build())
                .event(ApiProductAuditEvent.API_PRODUCT_UPDATED)
                .properties(Map.of(AuditProperties.API_PRODUCT, "api-product-id"))
                .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.of("UTC")))
                .build();

            service.createApiProductAuditLog(audit);

            var expectedAudit = AuditEntity.builder()
                .id("audit-id")
                .organizationId("organization-id")
                .environmentId("environment-id")
                .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.of("UTC")))
                .user("Jane Doe - (using token \"source-id\")")
                .properties(Map.of("API_PRODUCT", "api-product-id"))
                .referenceType(AuditEntity.AuditReferenceType.API_PRODUCT)
                .referenceId("api-product-id")
                .event(ApiProductAuditEvent.API_PRODUCT_UPDATED.name())
                .patch("[]")
                .build();

            Assertions.assertThat(auditCrudService.storage()).containsOnly(expectedAudit);
        }
    }

    @Nested
    class CreateEnvironmentAuditLog {

        @Test
        void should_create_an_environment_audit_log() {
            var audit = EnvironmentAuditLogEntity.builder()
                .environmentId("environment-id")
                .organizationId("organization-id")
                .actor(AuditActor.builder().userId("system").build())
                .event(SharedPolicyGroupAuditEvent.SHARED_POLICY_GROUP_CREATED)
                .properties(Map.of(AuditProperties.SHARED_POLICY_GROUP, "shared-policy-group-id"))
                .oldValue(null)
                .newValue(null)
                .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.of("UTC")))
                .build();

            service.createEnvironmentAuditLog(audit);

            var expectedAudit = AuditEntity.builder()
                .id("audit-id")
                .organizationId("organization-id")
                .environmentId("environment-id")
                .createdAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.of("UTC")))
                .user("system")
                .properties(Map.of("SHARED_POLICY_GROUP", "shared-policy-group-id"))
                .referenceType(AuditEntity.AuditReferenceType.ENVIRONMENT)
                .referenceId("environment-id")
                .event(SharedPolicyGroupAuditEvent.SHARED_POLICY_GROUP_CREATED.name())
                .patch("[]")
                .build();

            Assertions.assertThat(auditCrudService.storage()).containsOnly(expectedAudit);
        }
    }
}
