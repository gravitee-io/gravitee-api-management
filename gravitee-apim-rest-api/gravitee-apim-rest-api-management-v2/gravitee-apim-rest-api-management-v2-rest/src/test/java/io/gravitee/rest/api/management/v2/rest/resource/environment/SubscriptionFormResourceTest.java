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
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.v2.rest.model.SubscriptionForm;
import io.gravitee.rest.api.management.v2.rest.model.UpdateSubscriptionForm;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SubscriptionFormResourceTest extends AbstractResourceTest {

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
    class Update {

        @Test
        void should_update_form() {
            // Given
            var existingForm = SubscriptionFormFixtures.aSubscriptionFormBuilder().environmentId(ENVIRONMENT).build();
            subscriptionFormQueryService.initWith(List.of(existingForm));
            subscriptionFormCrudService.initWith(List.of(existingForm));

            UpdateSubscriptionForm request = new UpdateSubscriptionForm().gmdContent("<gmd-card>Updated Content</gmd-card>");

            // When
            var response = rootTarget.path(existingForm.getId().toString()).request().put(Entity.json(request));

            // Then
            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(SubscriptionForm.class)
                .satisfies(result -> assertThat(result.getGmdContent()).isEqualTo("<gmd-card>Updated Content</gmd-card>"));
        }

        @Test
        void should_return_404_when_form_not_exists() {
            // Given
            UpdateSubscriptionForm request = new UpdateSubscriptionForm().gmdContent("<gmd-card>Content</gmd-card>");

            // When
            var response = rootTarget.path("550e8400-e29b-41d4-a716-446655440000").request().put(Entity.json(request));

            // Then
            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_400_when_gmd_content_is_missing() {
            // Given
            var existingForm = SubscriptionFormFixtures.aSubscriptionFormBuilder().environmentId(ENVIRONMENT).build();
            subscriptionFormQueryService.initWith(List.of(existingForm));
            UpdateSubscriptionForm request = new UpdateSubscriptionForm();

            // When
            var response = rootTarget.path(existingForm.getId().toString()).request().put(Entity.json(request));

            // Then
            assertThat(response).hasStatus(HttpStatusCode.BAD_REQUEST_400);
        }
    }

    @Nested
    class EnableSubscriptionForm {

        @Test
        void should_enable_disabled_form() {
            // Given
            var disabledForm = SubscriptionFormFixtures.aSubscriptionFormBuilder().environmentId(ENVIRONMENT).enabled(false).build();
            subscriptionFormQueryService.initWith(List.of(disabledForm));
            subscriptionFormCrudService.initWith(List.of(disabledForm));

            // When
            var response = rootTarget.path(disabledForm.getId().toString()).path("_enable").request().post(Entity.json(""));

            // Then
            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(SubscriptionForm.class)
                .satisfies(result -> assertThat(result.getEnabled()).isTrue());
        }

        @Test
        void should_be_idempotent_when_already_enabled() {
            // Given
            var enabledForm = io.gravitee.apim.core.subscription_form.model.SubscriptionForm.builder()
                .id(io.gravitee.apim.core.subscription_form.model.SubscriptionFormId.of(SubscriptionFormFixtures.FORM_ID))
                .environmentId(ENVIRONMENT)
                .gmdContent(GraviteeMarkdown.of(SubscriptionFormFixtures.GMD_CONTENT))
                .enabled(true)
                .build();
            subscriptionFormQueryService.initWith(List.of(enabledForm));
            subscriptionFormCrudService.initWith(List.of(enabledForm));

            // When
            var response = rootTarget.path(enabledForm.getId().toString()).path("_enable").request().post(Entity.json(""));

            // Then
            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(SubscriptionForm.class)
                .satisfies(result -> assertThat(result.getEnabled()).isTrue());
        }

        @Test
        void should_return_404_when_form_not_found() {
            // Given - no form exists

            // When
            var response = rootTarget.path("550e8400-e29b-41d4-a716-446655440000").path("_enable").request().post(Entity.json(""));

            // Then
            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }
    }

    @Nested
    class DisableSubscriptionForm {

        @Test
        void should_disable_enabled_form() {
            // Given
            var enabledForm = io.gravitee.apim.core.subscription_form.model.SubscriptionForm.builder()
                .id(io.gravitee.apim.core.subscription_form.model.SubscriptionFormId.of(SubscriptionFormFixtures.FORM_ID))
                .environmentId(ENVIRONMENT)
                .gmdContent(GraviteeMarkdown.of(SubscriptionFormFixtures.GMD_CONTENT))
                .enabled(true)
                .build();
            subscriptionFormQueryService.initWith(List.of(enabledForm));
            subscriptionFormCrudService.initWith(List.of(enabledForm));

            // When
            var response = rootTarget.path(enabledForm.getId().toString()).path("_disable").request().post(Entity.json(""));

            // Then
            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(SubscriptionForm.class)
                .satisfies(result -> assertThat(result.getEnabled()).isFalse());
        }

        @Test
        void should_be_idempotent_when_already_disabled() {
            // Given
            var disabledForm = SubscriptionFormFixtures.aSubscriptionFormBuilder().environmentId(ENVIRONMENT).enabled(false).build();
            subscriptionFormQueryService.initWith(List.of(disabledForm));
            subscriptionFormCrudService.initWith(List.of(disabledForm));

            // When
            var response = rootTarget.path(disabledForm.getId().toString()).path("_disable").request().post(Entity.json(""));

            // Then
            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(SubscriptionForm.class)
                .satisfies(result -> assertThat(result.getEnabled()).isFalse());
        }

        @Test
        void should_return_404_when_form_not_found() {
            // Given - no form exists

            // When
            var response = rootTarget.path("550e8400-e29b-41d4-a716-446655440000").path("_disable").request().post(Entity.json(""));

            // Then
            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }
    }
}
