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
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.v2.rest.model.SubscriptionForm;
import io.gravitee.rest.api.management.v2.rest.model.UpdateSubscriptionForm;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SubscriptionFormResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "my-env";
    private static final String UNKNOWN_ID = "550e8400-e29b-41d4-a716-446655440000";

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

    private io.gravitee.apim.core.subscription_form.model.SubscriptionForm givenAForm(boolean enabled, boolean defaultForm) {
        var form = SubscriptionFormFixtures.aSubscriptionFormBuilder()
            .environmentId(ENVIRONMENT)
            .enabled(enabled)
            .defaultForm(defaultForm)
            .build();
        subscriptionFormQueryService.initWith(List.of(form));
        subscriptionFormCrudService.initWith(List.of(form));
        return form;
    }

    @Nested
    class GetSubscriptionForm {

        @Test
        void should_get_the_form() {
            var form = givenAForm(false, true);

            var response = rootTarget.path(form.getId().toString()).request().get();

            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(SubscriptionForm.class)
                .satisfies(result -> {
                    assertThat(result.getId()).isEqualTo(UUID.fromString(SubscriptionFormFixtures.FORM_ID));
                    assertThat(result.getName()).isEqualTo(SubscriptionFormFixtures.FORM_NAME);
                    assertThat(result.getGmdContent()).isEqualTo(SubscriptionFormFixtures.GMD_CONTENT);
                    assertThat(result.getEnabled()).isFalse();
                    assertThat(result.getDefaultForm()).isTrue();
                });
        }

        @Test
        void should_return_404_when_form_not_found() {
            var response = rootTarget.path(UNKNOWN_ID).request().get();

            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            var form = givenAForm(false, true);

            shouldReturn403(RolePermission.ENVIRONMENT_METADATA, ENVIRONMENT, RolePermissionAction.READ, () ->
                rootTarget.path(form.getId().toString()).request().get()
            );
        }
    }

    @Nested
    class Update {

        @Test
        void should_update_name_and_content() {
            var existingForm = givenAForm(false, true);
            UpdateSubscriptionForm request = new UpdateSubscriptionForm()
                .name("Renamed")
                .gmdContent("<gmd-card>Updated Content</gmd-card>")
                .apiIds(List.of());

            var response = rootTarget.path(existingForm.getId().toString()).request().put(Entity.json(request));

            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(SubscriptionForm.class)
                .satisfies(result -> {
                    assertThat(result.getName()).isEqualTo("Renamed");
                    assertThat(result.getGmdContent()).isEqualTo("<gmd-card>Updated Content</gmd-card>");
                    assertThat(result.getDefaultForm()).isTrue();
                });
        }

        @Test
        void should_return_404_when_form_not_exists() {
            UpdateSubscriptionForm request = new UpdateSubscriptionForm()
                .name("Any")
                .gmdContent("<gmd-card>Content</gmd-card>")
                .apiIds(List.of());

            var response = rootTarget.path(UNKNOWN_ID).request().put(Entity.json(request));

            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_409_when_renaming_to_the_name_of_another_form() {
            var existingForm = givenAForm(false, true);
            var otherForm = SubscriptionFormFixtures.aSubscriptionFormBuilder()
                .id(SubscriptionFormId.random())
                .environmentId(ENVIRONMENT)
                .name("Partners")
                .defaultForm(false)
                .build();
            subscriptionFormQueryService.initWith(List.of(otherForm));
            UpdateSubscriptionForm request = new UpdateSubscriptionForm()
                .name("Partners")
                .gmdContent("<gmd-card>Content</gmd-card>")
                .apiIds(List.of());

            var response = rootTarget.path(existingForm.getId().toString()).request().put(Entity.json(request));

            assertThat(response).hasStatus(HttpStatusCode.CONFLICT_409);
        }

        @Test
        void should_return_400_when_gmd_content_is_missing() {
            var existingForm = givenAForm(false, true);
            UpdateSubscriptionForm request = new UpdateSubscriptionForm().name("Any");

            var response = rootTarget.path(existingForm.getId().toString()).request().put(Entity.json(request));

            assertThat(response).hasStatus(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            var existingForm = givenAForm(false, true);
            UpdateSubscriptionForm request = new UpdateSubscriptionForm()
                .name("Any")
                .gmdContent("<gmd-card>Content</gmd-card>")
                .apiIds(List.of());

            shouldReturn403(RolePermission.ENVIRONMENT_METADATA, ENVIRONMENT, RolePermissionAction.UPDATE, () ->
                rootTarget.path(existingForm.getId().toString()).request().put(Entity.json(request))
            );
        }
    }

    @Nested
    class EnableSubscriptionForm {

        @Test
        void should_enable_disabled_form() {
            var disabledForm = givenAForm(false, true);

            var response = rootTarget.path(disabledForm.getId().toString()).path("_enable").request().post(Entity.json(""));

            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(SubscriptionForm.class)
                .satisfies(result -> assertThat(result.getEnabled()).isTrue());
        }

        @Test
        void should_be_idempotent_when_already_enabled() {
            var enabledForm = givenAForm(true, true);

            var response = rootTarget.path(enabledForm.getId().toString()).path("_enable").request().post(Entity.json(""));

            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(SubscriptionForm.class)
                .satisfies(result -> assertThat(result.getEnabled()).isTrue());
        }

        @Test
        void should_return_404_when_form_not_found() {
            var response = rootTarget.path(UNKNOWN_ID).path("_enable").request().post(Entity.json(""));

            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }
    }

    @Nested
    class DisableSubscriptionForm {

        @Test
        void should_disable_enabled_form() {
            var enabledForm = givenAForm(true, true);

            var response = rootTarget.path(enabledForm.getId().toString()).path("_disable").request().post(Entity.json(""));

            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(SubscriptionForm.class)
                .satisfies(result -> assertThat(result.getEnabled()).isFalse());
        }

        @Test
        void should_be_idempotent_when_already_disabled() {
            var disabledForm = givenAForm(false, true);

            var response = rootTarget.path(disabledForm.getId().toString()).path("_disable").request().post(Entity.json(""));

            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(SubscriptionForm.class)
                .satisfies(result -> assertThat(result.getEnabled()).isFalse());
        }

        @Test
        void should_return_404_when_form_not_found() {
            var response = rootTarget.path(UNKNOWN_ID).path("_disable").request().post(Entity.json(""));

            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }
    }

    @Nested
    class SetDefaultSubscriptionForm {

        @Test
        void should_promote_the_form_and_demote_the_previous_default() {
            var previousDefault = SubscriptionFormFixtures.aSubscriptionFormBuilder().environmentId(ENVIRONMENT).build();
            var partnerForm = SubscriptionFormFixtures.aSubscriptionFormBuilder()
                .id(SubscriptionFormId.random())
                .environmentId(ENVIRONMENT)
                .name("Partners")
                .defaultForm(false)
                .build();
            subscriptionFormQueryService.initWith(List.of(previousDefault, partnerForm));
            subscriptionFormCrudService.initWith(List.of(previousDefault, partnerForm));

            var response = rootTarget.path(partnerForm.getId().toString()).path("_default").request().post(Entity.json(""));

            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(SubscriptionForm.class)
                .satisfies(result -> assertThat(result.getDefaultForm()).isTrue());
            assertThat(previousDefault.isDefaultForm()).isFalse();
        }

        @Test
        void should_return_404_when_form_not_found() {
            var response = rootTarget.path(UNKNOWN_ID).path("_default").request().post(Entity.json(""));

            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            var form = givenAForm(false, false);

            shouldReturn403(RolePermission.ENVIRONMENT_METADATA, ENVIRONMENT, RolePermissionAction.UPDATE, () ->
                rootTarget.path(form.getId().toString()).path("_default").request().post(Entity.json(""))
            );
        }
    }
}
