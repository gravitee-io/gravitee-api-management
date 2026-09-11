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
package io.gravitee.apim.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.SubscriptionFormFixtures;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.subscription_form.model.Constraint;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormFieldConstraints;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.repository.management.model.SubscriptionForm;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionFormAdapterTest {

    private static final String FORM_ID = "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d";
    private static final String CONTENT_ID = "0f8fad5b-d9cb-469f-a165-70867728950e";
    private static final String CONSTRAINTS_JSON = "{\"company\":[{\"type\":\"required\"}]}";

    private final SubscriptionFormAdapter adapter = SubscriptionFormAdapter.INSTANCE;

    @Nested
    class ToEntity {

        @Test
        void should_map_the_row_and_take_the_definition_from_the_given_content() {
            var row = SubscriptionForm.builder()
                .id(FORM_ID)
                .environmentId("environment-id")
                .name("Partners")
                .defaultForm(true)
                .portalPageContentId(CONTENT_ID)
                .gmdContent("<gmd-input name=\"stale\"/>")
                .enabled(true)
                .validationConstraints(CONSTRAINTS_JSON)
                .build();

            var entity = adapter.toEntity(row, GraviteeMarkdown.of(SubscriptionFormFixtures.GMD_CONTENT));

            assertThat(entity.getId()).isEqualTo(SubscriptionFormId.of(FORM_ID));
            assertThat(entity.getEnvironmentId()).isEqualTo("environment-id");
            assertThat(entity.getName()).isEqualTo("Partners");
            assertThat(entity.isDefaultForm()).isTrue();
            assertThat(entity.getPortalPageContentId()).isEqualTo(PortalPageContentId.of(CONTENT_ID));
            assertThat(entity.getGmdContent()).isEqualTo(GraviteeMarkdown.of(SubscriptionFormFixtures.GMD_CONTENT));
            assertThat(entity.isEnabled()).isTrue();
            assertThat(entity.getValidationConstraints().byFieldKey()).containsOnlyKeys("company");
            assertThat(entity.getValidationConstraints().byFieldKey().get("company")).containsExactly(new Constraint.Required());
        }

        @Test
        void should_leave_page_content_id_null_on_a_legacy_row() {
            var row = SubscriptionForm.builder().id(FORM_ID).environmentId("environment-id").validationConstraints("{}").build();

            var entity = adapter.toEntity(row, GraviteeMarkdown.of(SubscriptionFormFixtures.GMD_CONTENT));

            assertThat(entity.getPortalPageContentId()).isNull();
            assertThat(entity.getValidationConstraints()).isEqualTo(SubscriptionFormFieldConstraints.empty());
        }
    }

    @Nested
    class ToRepository {

        @Test
        void should_map_the_form_without_writing_its_definition_inline() {
            var form = SubscriptionFormFixtures.aSubscriptionFormBuilder()
                .enabled(true)
                .validationConstraints(new SubscriptionFormFieldConstraints(Map.of("company", List.of(new Constraint.Required()))))
                .build();

            var row = adapter.toRepository(form);

            assertThat(row.getId()).isEqualTo(SubscriptionFormFixtures.FORM_ID);
            assertThat(row.getEnvironmentId()).isEqualTo(SubscriptionFormFixtures.ENVIRONMENT_ID);
            assertThat(row.getName()).isEqualTo(SubscriptionFormFixtures.FORM_NAME);
            assertThat(row.isDefaultForm()).isTrue();
            assertThat(row.getPortalPageContentId()).isEqualTo(SubscriptionFormFixtures.PORTAL_PAGE_CONTENT_ID.toString());
            assertThat(row.getGmdContent()).isNull();
            assertThat(row.isEnabled()).isTrue();
            assertThat(row.getValidationConstraints()).isEqualTo(CONSTRAINTS_JSON);
        }

        @Test
        void should_write_empty_constraints_as_an_empty_json_object() {
            var row = adapter.toRepository(SubscriptionFormFixtures.aSubscriptionFormWithNullId());

            assertThat(row.getId()).isNull();
            assertThat(row.getPortalPageContentId()).isNull();
            assertThat(row.getValidationConstraints()).isEqualTo("{}");
        }
    }
}
