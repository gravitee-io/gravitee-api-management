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

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.federation.FederatedAgent;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.context.OriginContext;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiQueryServiceInMemoryTest {

    private ApiQueryServiceInMemory cut;

    @BeforeEach
    void setUp() {
        cut = new ApiQueryServiceInMemory();
    }

    @Nested
    class SearchByIntegrationId {

        private static final String INTEGRATION_ID = "int-a";
        private static final List<String> EVERY_SEEDED_API = List.of("api-v2", "api-v4", "api-fed", "api-agent", "api-null-version");
        private static final List<String> EVERY_FREE_TEXT_API = List.of("api-name", "api-desc", "api-org", "api-org-less", "api-bare");

        @ParameterizedTest(name = "{0}")
        @MethodSource("definitionVersionRequests")
        void should_narrow_the_result_to_the_requested_definition_versions(
            String caseName,
            List<DefinitionVersion> requestedVersions,
            List<String> expectedApiIds
        ) {
            cut.initWith(
                List.of(
                    anApiOwnedByTheIntegration(ApiFixtures.aProxyApiV2(), "api-v2"),
                    anApiOwnedByTheIntegration(ApiFixtures.aProxyApiV4(), "api-v4"),
                    anApiOwnedByTheIntegration(ApiFixtures.aFederatedApi(), "api-fed"),
                    anApiOwnedByTheIntegration(ApiFixtures.aFederatedAgent(), "api-agent"),
                    aLegacyApiWithoutDefinitionVersion("api-null-version")
                )
            );

            var page = cut.searchByIntegrationId(INTEGRATION_ID, requestedVersions, null, new PageableImpl(1, 10));

            assertThat(page.getContent()).extracting(Api::getId).containsExactlyInAnyOrderElementsOf(expectedApiIds);
        }

        private static Stream<Arguments> definitionVersionRequests() {
            return Stream.of(
                Arguments.of(
                    "a V2 request matches the legacy api with no definition version alongside the explicitly V2 one",
                    List.of(DefinitionVersion.V2),
                    List.of("api-v2", "api-null-version")
                ),
                Arguments.of(
                    "a FEDERATED_AGENT request matches only the federated agent api",
                    List.of(DefinitionVersion.FEDERATED_AGENT),
                    List.of("api-agent")
                ),
                Arguments.of(
                    "a request for two versions matches the api of each of them",
                    List.of(DefinitionVersion.FEDERATED, DefinitionVersion.FEDERATED_AGENT),
                    List.of("api-fed", "api-agent")
                ),
                Arguments.of("a null version list narrows nothing", null, EVERY_SEEDED_API),
                Arguments.of("an empty version list narrows nothing", List.of(), EVERY_SEEDED_API)
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("freeTextQueries")
        void should_match_the_query_as_a_case_insensitive_substring_of_one_matched_field(
            String caseName,
            String query,
            List<String> expectedApiIds
        ) {
            cut.initWith(
                List.of(
                    anApiOfTheIntegration("api-name", "Global Alpha v*1 Agent", null),
                    anApiOfTheIntegration("api-desc", "Beta Runner", "Handles vX1 invoice reconciliation"),
                    anAgentOfTheIntegration("api-org", "Beta Runner", null, aProviderOf("Acme Robotics")),
                    anAgentOfTheIntegration("api-org-less", "Beta Runner", null, aProviderOf(null)),
                    anAgentOfTheIntegration("api-bare", null, null, null)
                )
            );

            var page = cut.searchByIntegrationId(INTEGRATION_ID, null, query, new PageableImpl(1, 10));

            assertThat(page.getContent()).extracting(Api::getId).containsExactlyInAnyOrderElementsOf(expectedApiIds);
        }

        private static Stream<Arguments> freeTextQueries() {
            return Stream.of(
                Arguments.of("a text occurring only in a name matches that api alone", "alpha", List.of("api-name")),
                Arguments.of("a text occurring only in a description matches that api alone", "invoice", List.of("api-desc")),
                Arguments.of(
                    "a text occurring only in an agent card provider organization matches that api alone",
                    "robotics",
                    List.of("api-org")
                ),
                Arguments.of("a provider whose organization is null is simply non matching, never a failure", "acme", List.of("api-org")),
                Arguments.of("an upper case text matches the differently cased value", "ALPHA", List.of("api-name")),
                Arguments.of("a text starting and ending mid word matches that api", "lpha", List.of("api-name")),
                Arguments.of(
                    "a text padded with surrounding whitespace matches as its trimmed form does",
                    "  alpha  ",
                    List.of("api-name")
                ),
                Arguments.of("punctuation in the text is matched literally, never as a wildcard", "v*1", List.of("api-name")),
                Arguments.of("a text no matched field holds matches nothing", "globex", List.of()),
                Arguments.of(
                    "a multi word text whose two words live in two different fields of one api matches nothing",
                    "Runner Handles",
                    List.of()
                ),
                Arguments.of("a null text narrows nothing", null, EVERY_FREE_TEXT_API),
                Arguments.of("an empty text narrows nothing", "", EVERY_FREE_TEXT_API),
                Arguments.of("a whitespace only text narrows nothing", "   ", EVERY_FREE_TEXT_API)
            );
        }

        @Test
        void should_never_return_an_api_another_integration_owns() {
            cut.initWith(
                List.of(
                    anApiOwnedByTheIntegration(ApiFixtures.aFederatedAgent(), "api-agent"),
                    anApiOwnedBy(ApiFixtures.aFederatedAgent(), "api-agent-elsewhere", "int-b")
                )
            );

            var page = cut.searchByIntegrationId(INTEGRATION_ID, null, null, new PageableImpl(1, 10));

            assertThat(page.getContent()).extracting(Api::getId).containsExactly("api-agent");
        }

        private static Api anApiOfTheIntegration(String apiId, String name, String description) {
            return anApiOwnedByTheIntegration(ApiFixtures.aFederatedApi(), apiId).toBuilder().name(name).description(description).build();
        }

        private static Api anAgentOfTheIntegration(String apiId, String name, String description, FederatedAgent.Provider provider) {
            var agentCard = ((FederatedAgent) ApiFixtures.aFederatedAgent().getApiDefinitionValue()).toBuilder().provider(provider).build();
            return anApiOwnedByTheIntegration(ApiFixtures.aFederatedAgent(), apiId)
                .toBuilder()
                .name(name)
                .description(description)
                .apiDefinitionValue(agentCard)
                .build();
        }

        private static FederatedAgent.Provider aProviderOf(String organization) {
            return new FederatedAgent.Provider(organization, "https://example.net");
        }

        private static Api anApiOwnedByTheIntegration(Api api, String apiId) {
            return anApiOwnedBy(api, apiId, INTEGRATION_ID);
        }

        private static Api anApiOwnedBy(Api api, String apiId, String integrationId) {
            return api.toBuilder().id(apiId).originContext(new OriginContext.Integration(integrationId)).build();
        }

        private static Api aLegacyApiWithoutDefinitionVersion(String apiId) {
            // The api definition has to go with the version: Api.toBuilder() re-derives definitionVersion from it,
            // so an api that kept its definition would silently come back as V2 however this builder chain is ordered.
            return anApiOwnedByTheIntegration(ApiFixtures.aProxyApiV2(), apiId)
                .toBuilder()
                .apiDefinitionValue(null)
                .definitionVersion(null)
                .build();
        }
    }
}
