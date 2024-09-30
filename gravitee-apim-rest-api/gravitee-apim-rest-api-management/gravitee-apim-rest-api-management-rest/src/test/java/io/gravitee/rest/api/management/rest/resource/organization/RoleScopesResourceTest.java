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
package io.gravitee.rest.api.management.rest.resource.organization;

import static org.junit.Assert.assertEquals;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class RoleScopesResourceTest extends AbstractResourceTest {

    private static final Map<String, List<String>> EXPECTED_ROLE_SCOPES = Map.of(
        "ORGANIZATION",
        List.of(
            "AUDIT",
            "CUSTOM_USER_FIELDS",
            "ENTRYPOINT",
            "ENVIRONMENT",
            "IDENTITY_PROVIDER",
            "IDENTITY_PROVIDER_ACTIVATION",
            "INSTALLATION",
            "LICENSE_MANAGEMENT",
            "NOTIFICATION_TEMPLATES",
            "POLICIES",
            "ROLE",
            "SETTINGS",
            "TAG",
            "TENANT",
            "USER",
            "USER_TOKEN"
        ),
        "ENVIRONMENT",
        List.of(
            "ALERT",
            "API",
            "API_HEADER",
            "APPLICATION",
            "AUDIT",
            "CATEGORY",
            "CLIENT_REGISTRATION_PROVIDER",
            "DASHBOARD",
            "DICTIONARY",
            "DOCUMENTATION",
            "ENTRYPOINT",
            "GROUP",
            "IDENTITY_PROVIDER_ACTIVATION",
            "INSTANCE",
            "INTEGRATION",
            "MESSAGE",
            "METADATA",
            "NOTIFICATION",
            "PLATFORM",
            "QUALITY_RULE",
            "SETTINGS",
            "SHARED_POLICY_GROUP",
            "TAG",
            "TENANT",
            "THEME",
            "TOP_APIS"
        ),
        "API",
        List.of(
            "ALERT",
            "ANALYTICS",
            "AUDIT",
            "DEFINITION",
            "DISCOVERY",
            "DOCUMENTATION",
            "EVENT",
            "GATEWAY_DEFINITION",
            "HEALTH",
            "LOG",
            "MEMBER",
            "MESSAGE",
            "METADATA",
            "NOTIFICATION",
            "PLAN",
            "QUALITY_RULE",
            "RATING",
            "RATING_ANSWER",
            "RESPONSE_TEMPLATES",
            "REVIEWS",
            "SUBSCRIPTION"
        ),
        "APPLICATION",
        List.of("ALERT", "ANALYTICS", "DEFINITION", "LOG", "MEMBER", "METADATA", "NOTIFICATION", "SUBSCRIPTION"),
        "INTEGRATION",
        List.of("DEFINITION", "MEMBER")
    );

    @Override
    protected String contextPath() {
        return "configuration/rolescopes/";
    }

    @Test
    public void should_return_role_scopes() {
        var response = envTarget().request().get();

        Map<?, ?> resultRoleScopes = response.readEntity(Map.class);

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertEquals(EXPECTED_ROLE_SCOPES, resultRoleScopes);
    }
}
