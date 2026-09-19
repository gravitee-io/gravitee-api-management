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
package inmemory;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.federation.FederatedAgent;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.context.OriginContext;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiQueryServiceInMemoryTest {

    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String INTEGRATION_ID = "integration-id";

    private ApiQueryServiceInMemory cut;

    @BeforeEach
    void setUp() {
        cut = new ApiQueryServiceInMemory();
    }

    private static Api anApi(String id) {
        return Api.builder().id(id).environmentId(ENVIRONMENT_ID).originContext(new OriginContext.Integration(INTEGRATION_ID)).build();
    }

    private static Api withProviderOrganization(Api api, String organization) {
        return api
            .toBuilder()
            .apiDefinitionValue(FederatedAgent.builder().provider(new FederatedAgent.Provider(organization, "https://example.net")).build())
            .build();
    }

    @Nested
    class SearchByIntegrationId {

        @Test
        void should_match_the_query_case_insensitively_against_the_name() {
            cut.initWith(
                List.of(
                    anApi("api-1").toBuilder().name("Alpha Billing Agent").build(),
                    anApi("api-2").toBuilder().name("Beta Runner").build()
                )
            );

            var page = cut.searchByIntegrationId(ENVIRONMENT_ID, INTEGRATION_ID, null, "ALPHA", new PageableImpl(1, 10));

            assertThat(page.getContent()).extracting(Api::getId).containsExactly("api-1");
        }

        @Test
        void should_match_the_query_as_a_substring_of_the_description() {
            cut.initWith(
                List.of(
                    anApi("api-1").toBuilder().description("Handles invoice reconciliation").build(),
                    anApi("api-2").toBuilder().description("Handles nothing related").build()
                )
            );

            var page = cut.searchByIntegrationId(ENVIRONMENT_ID, INTEGRATION_ID, null, "invoice", new PageableImpl(1, 10));

            assertThat(page.getContent()).extracting(Api::getId).containsExactly("api-1");
        }

        @Test
        void should_match_the_query_against_the_agent_provider_organization() {
            cut.initWith(
                List.of(withProviderOrganization(anApi("api-1"), "Acme Robotics"), withProviderOrganization(anApi("api-2"), "Initech"))
            );

            var page = cut.searchByIntegrationId(ENVIRONMENT_ID, INTEGRATION_ID, null, "acme", new PageableImpl(1, 10));

            assertThat(page.getContent()).extracting(Api::getId).containsExactly("api-1");
        }

        @Test
        void should_treat_a_null_query_as_no_narrowing() {
            cut.initWith(List.of(anApi("api-1"), anApi("api-2")));

            var page = cut.searchByIntegrationId(ENVIRONMENT_ID, INTEGRATION_ID, null, null, new PageableImpl(1, 10));

            assertThat(page.getContent()).extracting(Api::getId).containsExactlyInAnyOrder("api-1", "api-2");
        }

        @Test
        void should_treat_a_legacy_api_with_no_stored_definition_version_as_v2() {
            cut.initWith(
                List.of(
                    anApi("api-legacy"), // definitionVersion left null
                    anApi("api-v4").toBuilder().definitionVersion(DefinitionVersion.V4).build()
                )
            );

            var page = cut.searchByIntegrationId(
                ENVIRONMENT_ID,
                INTEGRATION_ID,
                List.of(DefinitionVersion.V2),
                null,
                new PageableImpl(1, 10)
            );

            assertThat(page.getContent()).extracting(Api::getId).containsExactly("api-legacy");
        }

        @Test
        void should_narrow_to_the_requested_definition_versions() {
            cut.initWith(
                List.of(
                    anApi("api-v2").toBuilder().definitionVersion(DefinitionVersion.V2).build(),
                    anApi("api-v4").toBuilder().definitionVersion(DefinitionVersion.V4).build()
                )
            );

            var page = cut.searchByIntegrationId(
                ENVIRONMENT_ID,
                INTEGRATION_ID,
                List.of(DefinitionVersion.V4),
                null,
                new PageableImpl(1, 10)
            );

            assertThat(page.getContent()).extracting(Api::getId).containsExactly("api-v4");
        }

        @Test
        void should_never_return_an_api_of_another_integration() {
            cut.initWith(List.of(anApi("api-1").toBuilder().originContext(new OriginContext.Integration("another-integration")).build()));

            var page = cut.searchByIntegrationId(ENVIRONMENT_ID, INTEGRATION_ID, null, null, new PageableImpl(1, 10));

            assertThat(page.getContent()).isEmpty();
        }

        @Test
        void should_never_return_an_api_of_another_environment() {
            cut.initWith(List.of(anApi("api-1").toBuilder().environmentId("another-environment").build()));

            var page = cut.searchByIntegrationId(ENVIRONMENT_ID, INTEGRATION_ID, null, null, new PageableImpl(1, 10));

            assertThat(page.getContent()).isEmpty();
        }

        @Test
        void should_order_apis_most_recently_updated_first_and_keep_a_null_updated_at_deterministically_last() {
            var now = ZonedDateTime.now();
            cut.initWith(
                List.of(
                    anApi("api-no-date"), // updatedAt left null
                    anApi("api-old").toBuilder().updatedAt(now.minusDays(1)).build(),
                    anApi("api-recent").toBuilder().updatedAt(now).build()
                )
            );

            var page = cut.searchByIntegrationId(ENVIRONMENT_ID, INTEGRATION_ID, null, null, new PageableImpl(1, 10));

            assertThat(page.getContent()).extracting(Api::getId).containsExactly("api-recent", "api-old", "api-no-date");
        }

        @Test
        void should_break_ties_by_id_when_apis_share_the_same_updated_at() {
            var now = ZonedDateTime.now();
            cut.initWith(List.of(anApi("api-b").toBuilder().updatedAt(now).build(), anApi("api-a").toBuilder().updatedAt(now).build()));

            var page = cut.searchByIntegrationId(ENVIRONMENT_ID, INTEGRATION_ID, null, null, new PageableImpl(1, 10));

            assertThat(page.getContent()).extracting(Api::getId).containsExactly("api-a", "api-b");
        }
    }
}
