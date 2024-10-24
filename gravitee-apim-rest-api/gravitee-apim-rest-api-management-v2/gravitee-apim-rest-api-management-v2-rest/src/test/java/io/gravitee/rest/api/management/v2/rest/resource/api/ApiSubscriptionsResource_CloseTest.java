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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.apim.core.api.model.Api.*;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import inmemory.ApplicationCrudServiceInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.use_case.CloseSubscriptionUseCase;
import io.gravitee.rest.api.management.v2.rest.model.Subscription;
import io.gravitee.rest.api.management.v2.rest.model.SubscriptionStatus;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiSubscriptionsResource_CloseTest extends AbstractResourceTest {

    protected static final String API = "my-api";
    protected static final String PLAN = "my-plan";
    protected static final String APPLICATION = "my-application";
    protected static final String SUBSCRIPTION = "my-subscription";
    protected static final String ENVIRONMENT = "my-env";

    @Autowired
    protected CloseSubscriptionUseCase closeSubscriptionUsecase;

    @Autowired
    private SubscriptionCrudServiceInMemory subscriptionCrudServiceInMemory;

    @Autowired
    private ApplicationCrudServiceInMemory applicationCrudServiceInMemory;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/subscriptions" + "/" + SUBSCRIPTION + "/_close";
    }

    @BeforeEach
    public void setUp() {
        super.setUp();

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        applicationCrudServiceInMemory.initWith(List.of(BaseApplicationEntity.builder().id(APPLICATION).build()));
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        subscriptionCrudServiceInMemory.reset();
    }

    @Test
    public void should_return_404_if_not_found() {
        final Response response = rootTarget().request().post(Entity.json(null));
        assertThat(response).hasStatus(NOT_FOUND_404).asError().hasMessage("Subscription [" + SUBSCRIPTION + "] cannot be found.");
    }

    @Test
    public void should_return_404_if_subscription_associated_to_another_api() {
        subscriptionCrudServiceInMemory.initWith(
            List.of(
                SubscriptionEntity
                    .builder()
                    .id(SUBSCRIPTION)
                    .apiId("ANOTHER_API")
                    .planId(PLAN)
                    .applicationId(APPLICATION)
                    .status(SubscriptionEntity.Status.ACCEPTED)
                    .build()
            )
        );

        final Response response = rootTarget().request().post(Entity.json(null));
        assertThat(response).hasStatus(NOT_FOUND_404).asError().hasMessage("Subscription [" + SUBSCRIPTION + "] cannot be found.");
    }

    @Test
    public void should_return_403_if_incorrect_permissions() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            )
        )
            .thenReturn(false);

        final Response response = rootTarget().request().post(Entity.json(null));
        assertThat(response).hasStatus(FORBIDDEN_403).asError().hasMessage("You do not have sufficient rights to access this resource");
    }

    @Test
    public void should_return_subscription_when_subscription_closed() {
        subscriptionCrudServiceInMemory.initWith(
            List.of(
                SubscriptionEntity
                    .builder()
                    .id(SUBSCRIPTION)
                    .apiId(API)
                    .planId(PLAN)
                    .applicationId(APPLICATION)
                    .status(SubscriptionEntity.Status.ACCEPTED)
                    .build()
            )
        );

        apiCrudService.initWith(List.of(builder().id(API).build()));

        final Response response = rootTarget().request().post(Entity.json(null));
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(Subscription.class)
            .extracting(Subscription::getStatus)
            .isEqualTo(SubscriptionStatus.CLOSED);
    }
}
