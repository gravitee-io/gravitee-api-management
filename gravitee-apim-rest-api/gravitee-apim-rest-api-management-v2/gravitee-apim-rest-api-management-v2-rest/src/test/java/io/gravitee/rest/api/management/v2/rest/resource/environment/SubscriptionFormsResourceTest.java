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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import static assertions.MAPIAssertions.assertThat;
import static org.mockito.Mockito.when;

import fixtures.core.model.SubscriptionFormFixtures;
import inmemory.InMemoryAlternative;
import inmemory.SubscriptionFormCrudServiceInMemory;
import inmemory.SubscriptionFormQueryServiceInMemory;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.v2.rest.model.SubscriptionForm;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SubscriptionFormsResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "my-env";

    WebTarget rootTarget;

    @Inject
    SubscriptionFormCrudServiceInMemory subscriptionFormCrudService;

    @Inject
    SubscriptionFormQueryServiceInMemory subscriptionFormQueryService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/subscription-forms";
    }

    @BeforeEach
    void setup() {
        rootTarget = rootTarget();

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    @Override
    public void tearDown() {
        super.tearDown();
        UuidString.reset();
        GraviteeContext.cleanContext();

        Stream.of(subscriptionFormCrudService, subscriptionFormQueryService).forEach(InMemoryAlternative::reset);
    }

    @Nested
    class GetSubscriptionForm {

        @Test
        void should_get_subscription_form() {
            // Given
            var form = SubscriptionFormFixtures.aSubscriptionFormBuilder().environmentId(ENVIRONMENT).build();
            subscriptionFormQueryService.initWith(List.of(form));

            // When
            var response = rootTarget.request().get();

            // Then
            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(SubscriptionForm.class)
                .satisfies(result -> {
                    assertThat(result.getId()).isEqualTo(UUID.fromString(SubscriptionFormFixtures.FORM_ID));
                    assertThat(result.getGmdContent()).isEqualTo(SubscriptionFormFixtures.GMD_CONTENT);
                    assertThat(result.getEnabled()).isFalse();
                });
        }

        @Test
        void should_return_404_when_subscription_form_not_found() {
            // Given - no form exists

            // When
            var response = rootTarget.request().get();

            // Then
            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }
    }
}
