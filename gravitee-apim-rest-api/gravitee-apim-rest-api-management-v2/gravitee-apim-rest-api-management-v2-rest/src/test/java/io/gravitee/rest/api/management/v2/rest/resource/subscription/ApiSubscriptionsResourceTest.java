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
package io.gravitee.rest.api.management.v2.rest.resource.subscription;

import static io.gravitee.apim.core.subscription.model.SubscriptionEntity.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import inmemory.PlanCrudServiceInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
class ApiSubscriptionsResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";
    private static final String API_ID = "api-id";
    private static final String PLAN_ID = "plan-id";
    private static final String APPLICATION_ID = "application-id";
    private static final String SUBSCRIPTION_ID = "subscription-id";

    private static final String SPEC_FILE = "/crd/subscription/jwt-subscription.json";

    @Autowired
    SubscriptionService subscriptionService;

    @Autowired
    SubscriptionCrudServiceInMemory subscriptionCrudService;

    @Autowired
    PlanCrudServiceInMemory planCrudService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API_ID + "/subscriptions";
    }

    @BeforeEach
    void setup() {
        GraviteeContext.cleanContext();
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT);
        environment.setOrganizationId(ORGANIZATION);

        when(environmentService.findById(ENVIRONMENT)).thenReturn(environment);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environment);
    }

    @Nested
    class ImportSpec {

        WebTarget target;

        @BeforeEach
        void setUp() {
            target = rootTarget().path("/spec/_import");

            when(subscriptionService.create(any(ExecutionContext.class), any(), isNull(), eq("subscription-id")))
                .thenReturn(
                    io.gravitee.rest.api.model.SubscriptionEntity
                        .builder()
                        .id(SUBSCRIPTION_ID)
                        .api(API_ID)
                        .plan(PLAN_ID)
                        .application(APPLICATION_ID)
                        .build()
                );

            subscriptionCrudService.initWith(
                List.of(
                    builder().id(SUBSCRIPTION_ID).apiId(API_ID).planId(PLAN_ID).applicationId(APPLICATION_ID).status(Status.PENDING).build()
                )
            );

            planCrudService.initWith(
                List.of(
                    Plan
                        .builder()
                        .id(PLAN_ID)
                        .definitionVersion(DefinitionVersion.V4)
                        .planDefinitionHttpV4(
                            io.gravitee.definition.model.v4.plan.Plan
                                .builder()
                                .id(PLAN_ID)
                                .security(PlanSecurity.builder().type("JWT").build())
                                .build()
                        )
                        .build()
                )
            );
        }

        @Test
        void should_create_and_auto_accept_subscription() {
            var crdStatus = doImport();

            assertSoftly(soft -> {
                soft.assertThat(crdStatus.getStatus()).isEqualTo(Status.ACCEPTED);
                soft.assertThat(crdStatus.getStartingAt()).isNotNull();
                soft.assertThat(crdStatus.getEndingAt()).isNotNull();
            });
        }

        @Test
        void should_update_end_date() {
            when(subscriptionService.findById(SUBSCRIPTION_ID))
                .thenReturn(
                    io.gravitee.rest.api.model.SubscriptionEntity
                        .builder()
                        .id(SUBSCRIPTION_ID)
                        .api(API_ID)
                        .plan(PLAN_ID)
                        .application(APPLICATION_ID)
                        .status(SubscriptionStatus.ACCEPTED)
                        .startingAt(Date.from(ZonedDateTime.parse("2025-01-01T00:00:00Z").toInstant()))
                        .endingAt(Date.from(ZonedDateTime.parse("2025-08-01T00:00:00Z").toInstant()))
                        .build()
                );

            var crdStatus = doImport();

            assertSoftly(soft -> {
                soft.assertThat(crdStatus.getStatus()).isEqualTo(Status.ACCEPTED);
                soft.assertThat(crdStatus.getStartingAt()).isEqualTo(ZonedDateTime.parse("2025-01-01T00:00:00Z"));
                soft.assertThat(crdStatus.getEndingAt()).isEqualTo(ZonedDateTime.parse("2025-08-06T00:00:00Z"));
            });
        }

        private SubscriptionEntity doImport() {
            try (var response = target.request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(readJSON()))) {
                assertThat(response.getStatus()).isEqualTo(200);
                return response.readEntity(SubscriptionEntity.class);
            }
        }
    }

    @Nested
    class DeleteSpec {

        WebTarget target;

        @BeforeEach
        void setUp() {
            target = rootTarget().path("/spec/" + SUBSCRIPTION_ID);

            when(subscriptionService.create(any(ExecutionContext.class), any(), isNull(), eq("subscription-id")))
                .thenReturn(
                    io.gravitee.rest.api.model.SubscriptionEntity
                        .builder()
                        .id(SUBSCRIPTION_ID)
                        .api(API_ID)
                        .plan(PLAN_ID)
                        .application(APPLICATION_ID)
                        .build()
                );

            subscriptionCrudService.initWith(
                List.of(
                    builder().id(SUBSCRIPTION_ID).apiId(API_ID).planId(PLAN_ID).applicationId(APPLICATION_ID).status(Status.PENDING).build()
                )
            );

            planCrudService.initWith(
                List.of(
                    Plan
                        .builder()
                        .id(PLAN_ID)
                        .definitionVersion(DefinitionVersion.V4)
                        .planDefinitionHttpV4(
                            io.gravitee.definition.model.v4.plan.Plan
                                .builder()
                                .id(PLAN_ID)
                                .security(PlanSecurity.builder().type("JWT").build())
                                .build()
                        )
                        .build()
                )
            );

            apiCrudService.initWith(List.of(Api.builder().id(API_ID).build()));
        }

        @Test
        void should_close_subscription() {
            try (var response = target.request().delete()) {
                assertSoftly(soft -> {
                    soft.assertThat(response.getStatus()).isEqualTo(204);
                    var subscription = subscriptionCrudService.get(SUBSCRIPTION_ID);
                    soft.assertThat(subscription).isNotNull();
                    soft.assertThat(subscription.getStatus()).isEqualTo(Status.REJECTED);
                });
            }
        }
    }

    private String readJSON() {
        try (var reader = this.getClass().getResourceAsStream(SPEC_FILE)) {
            return IOUtils.toString(reader, Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
