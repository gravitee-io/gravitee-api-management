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

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import fixtures.SubscriptionFixtures;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.VerifySubscription;
import io.gravitee.rest.api.management.v2.rest.model.VerifySubscriptionResponse;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiSubscriptionsResource_VerifyCreateApiSubscriptionTest extends AbstractApiSubscriptionsResourceTest {

    @Autowired
    private ApiKeyService apiKeyService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/subscriptions/_verify";
    }

    @BeforeEach
    public void before() {
        reset(apiKeyService);
    }

    @Test
    public void should_return_403_if_incorrect_permissions() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.CREATE)
            )
        ).thenReturn(false);

        final Response response = rootTarget().request().post(Entity.json(SubscriptionFixtures.aVerifySubscription()));
        assertEquals(FORBIDDEN_403, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(FORBIDDEN_403, (int) error.getHttpStatus());
        assertEquals("You do not have sufficient rights to access this resource", error.getMessage());
    }

    @Test
    public void should_return_400_when_invalid_api_key_pattern() {
        final Response response = rootTarget().request().post(Entity.json(SubscriptionFixtures.aVerifySubscription().apiKey("###")));
        assertEquals(BAD_REQUEST_400, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(BAD_REQUEST_400, (int) error.getHttpStatus());
        assertEquals("Validation error", error.getMessage());
    }

    @Test
    public void should_return_400_when_missing_api_key() {
        final Response response = rootTarget().request().post(Entity.json(SubscriptionFixtures.aVerifySubscription().apiKey(null)));
        assertEquals(BAD_REQUEST_400, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(BAD_REQUEST_400, (int) error.getHttpStatus());
        assertEquals("Validation error", error.getMessage());
    }

    @Test
    public void should_return_400_when_missing_application() {
        final Response response = rootTarget().request().post(Entity.json(SubscriptionFixtures.aVerifySubscription().applicationId(null)));
        assertEquals(BAD_REQUEST_400, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(BAD_REQUEST_400, (int) error.getHttpStatus());
        assertEquals("Validation error", error.getMessage());
    }

    @Test
    public void should_verify_subscription() {
        when(
            apiKeyService.canCreate(
                GraviteeContext.getExecutionContext(),
                "apiKey",
                API,
                io.gravitee.apim.core.subscription.model.SubscriptionReferenceType.API.name(),
                APPLICATION
            )
        ).thenReturn(true);

        final VerifySubscription verifySubscription = SubscriptionFixtures.aVerifySubscription()
            .applicationId(APPLICATION)
            .apiKey("apiKey");
        final Response response = rootTarget().request().post(Entity.json(verifySubscription));
        assertEquals(OK_200, response.getStatus());

        final VerifySubscriptionResponse verifyResponse = response.readEntity(VerifySubscriptionResponse.class);
        assertTrue(verifyResponse.getOk());
    }

    @Test
    public void should_verify_subscription_false_response() {
        when(
            apiKeyService.canCreate(
                GraviteeContext.getExecutionContext(),
                "apiKey",
                API,
                io.gravitee.apim.core.subscription.model.SubscriptionReferenceType.API.name(),
                APPLICATION
            )
        ).thenReturn(false);

        final VerifySubscription verifySubscription = SubscriptionFixtures.aVerifySubscription()
            .applicationId(APPLICATION)
            .apiKey("apiKey");
        final Response response = rootTarget().request().post(Entity.json(verifySubscription));
        assertEquals(OK_200, response.getStatus());

        final VerifySubscriptionResponse verifyResponse = response.readEntity(VerifySubscriptionResponse.class);
        assertFalse(verifyResponse.getOk());
    }
}
