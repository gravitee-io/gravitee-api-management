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
import io.gravitee.rest.api.management.v2.rest.model.CreateSubscriptionForm;
import io.gravitee.rest.api.management.v2.rest.model.SubscriptionForm;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
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
    class ListSubscriptionForms {

        @Test
        void should_list_the_forms_of_the_environment() {
            var defaultForm = SubscriptionFormFixtures.aSubscriptionFormBuilder().environmentId(ENVIRONMENT).build();
            var partnerForm = SubscriptionFormFixtures.aSubscriptionFormBuilder()
                .id(SubscriptionFormId.random())
                .environmentId(ENVIRONMENT)
                .name("Partners")
                .defaultForm(false)
                .build();
            var otherEnvironmentForm = SubscriptionFormFixtures.aSubscriptionFormBuilder()
                .id(SubscriptionFormId.random())
                .environmentId("other-env")
                .build();
            subscriptionFormQueryService.initWith(List.of(defaultForm, partnerForm, otherEnvironmentForm));

            var response = rootTarget.request().get();

            assertThat(response).hasStatus(HttpStatusCode.OK_200);
            var forms = response.readEntity(new GenericType<List<SubscriptionForm>>() {});
            assertThat(forms)
                .extracting(SubscriptionForm::getId)
                .containsExactly(UUID.fromString(SubscriptionFormFixtures.FORM_ID), UUID.fromString(partnerForm.getId().toString()));
            assertThat(forms.get(0).getName()).isEqualTo(SubscriptionFormFixtures.FORM_NAME);
            assertThat(forms.get(0).getDefaultForm()).isTrue();
            assertThat(forms.get(0).getGmdContent()).isEqualTo(SubscriptionFormFixtures.GMD_CONTENT);
            assertThat(forms.get(1).getName()).isEqualTo("Partners");
            assertThat(forms.get(1).getDefaultForm()).isFalse();
        }

        @Test
        void should_return_an_empty_list_when_the_environment_has_no_form() {
            var response = rootTarget.request().get();

            assertThat(response).hasStatus(HttpStatusCode.OK_200);
            assertThat(response.readEntity(new GenericType<List<SubscriptionForm>>() {})).isEmpty();
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.ENVIRONMENT_METADATA, ENVIRONMENT, RolePermissionAction.READ, () -> rootTarget.request().get());
        }
    }

    @Nested
    class CreateSubscriptionForms {

        @Test
        void should_create_a_form() {
            var request = new CreateSubscriptionForm().name("Partners").gmdContent(SubscriptionFormFixtures.GMD_CONTENT);

            var response = rootTarget.request().post(Entity.json(request));

            assertThat(response)
                .hasStatus(HttpStatusCode.CREATED_201)
                .asEntity(SubscriptionForm.class)
                .satisfies(result -> {
                    assertThat(result.getId()).isNotNull();
                    assertThat(result.getName()).isEqualTo("Partners");
                    assertThat(result.getGmdContent()).isEqualTo(SubscriptionFormFixtures.GMD_CONTENT);
                    assertThat(result.getEnabled()).isFalse();
                    assertThat(result.getDefaultForm()).isFalse();
                });
            assertThat(subscriptionFormCrudService.storage()).hasSize(1);
        }

        @Test
        void should_return_409_when_the_name_is_already_used() {
            subscriptionFormQueryService.initWith(
                List.of(SubscriptionFormFixtures.aSubscriptionFormBuilder().environmentId(ENVIRONMENT).name("Partners").build())
            );
            var request = new CreateSubscriptionForm().name("partners").gmdContent(SubscriptionFormFixtures.GMD_CONTENT);

            var response = rootTarget.request().post(Entity.json(request));

            assertThat(response).hasStatus(HttpStatusCode.CONFLICT_409);
        }

        @Test
        void should_return_400_when_the_name_is_missing() {
            var request = new CreateSubscriptionForm().gmdContent(SubscriptionFormFixtures.GMD_CONTENT);

            var response = rootTarget.request().post(Entity.json(request));

            assertThat(response).hasStatus(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            var request = new CreateSubscriptionForm().name("Partners").gmdContent(SubscriptionFormFixtures.GMD_CONTENT);

            shouldReturn403(RolePermission.ENVIRONMENT_METADATA, ENVIRONMENT, RolePermissionAction.CREATE, () ->
                rootTarget.request().post(Entity.json(request))
            );
        }
    }
}
