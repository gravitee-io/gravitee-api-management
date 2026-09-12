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
package io.gravitee.apim.infra.query_service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.Sortable;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.search.model.IndexableApi;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.federation.FederatedAgent;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.search.lucene.searcher.ApiDocumentSearcher;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.IndexableApiDocumentTransformer;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiQueryServiceImplTest {

    ApiRepository apiRepository;
    IndexWriter indexWriter;
    ApiQueryService service;

    @BeforeEach
    void setUp() throws IOException {
        apiRepository = mock(ApiRepository.class);
        indexWriter = new IndexWriter(new ByteBuffersDirectory(), new IndexWriterConfig(new StandardAnalyzer()));
        service = new ApiQueryServiceImpl(apiRepository, new ApiDocumentSearcher(indexWriter));
    }

    @AfterEach
    void tearDown() throws IOException {
        indexWriter.close();
    }

    @Test
    void search_should_return_matching_api_entities() {
        Api api = anApi();
        givenMatchingApis(Stream.of(api));

        var res = service
            .search(ApiSearchCriteria.builder().build(), Sortable.builder().build(), ApiFieldFilter.builder().build())
            .toList();
        assertThat(res).hasSize(1).containsExactly(api);
    }

    private void givenMatchingApis(Stream<Api> apis) {
        when(apiRepository.search(any(), any(), any())).thenReturn(ApiAdapter.INSTANCE.toRepositoryStream(apis));
    }

    private Api anApi() {
        return ApiFixtures.aProxyApiV4();
    }

    @Test
    void should_list_apis_matching_integration_id() {
        //Given
        var integrationId = "integration-id";
        var pageable = new PageableImpl(1, 5);

        var expectedApis = List.of(fixtures.repository.ApiFixtures.aFederatedApi());
        var page = new Page<>(expectedApis, pageable.getPageNumber(), expectedApis.size(), expectedApis.size());
        when(apiRepository.search(any(), any(), any(), any())).thenReturn(page);

        //When
        Page<Api> responsePage = service.findByIntegrationId(integrationId, pageable);

        //Then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(responsePage).isNotNull();
            softly.assertThat(responsePage.getPageNumber()).isEqualTo(1);
            softly.assertThat(responsePage.getPageElements()).isEqualTo(1);
            softly.assertThat(responsePage.getTotalElements()).isEqualTo(1);
            softly.assertThat(responsePage.getContent().get(0).getId()).isEqualTo("api-id");
        });
    }

    @Nested
    class SearchByIntegrationId {

        private static final String UUID_INTEGRATION_ID = "3f7a1c2e-4b5d-11ee-be56-0242ac120002";
        private static final String MIXED_CASE_INTEGRATION_ID = "Int-A-2024";
        private static final String INTEGRATION_ID = "int-a";
        private static final String THE_LABEL_EVERY_SEEDED_API_CARRIES = "label-1";
        private static final List<String> EVERY_SEEDED_API = List.of("api-v2", "api-v4", "api-fed", "api-agent", "api-null-version");

        @ParameterizedTest(name = "{0}")
        @MethodSource("integrationIdRequests")
        void should_match_the_requested_integration_id_exactly(String caseName, String requestedId, List<String> expectedApiIds)
            throws IOException {
            // Given one api owned by a hyphenated uuid integration and one owned by a mixed case integration
            givenIndexedApis(
                anApiOwnedByIntegration("api-uuid", UUID_INTEGRATION_ID),
                anApiOwnedByIntegration("api-1", MIXED_CASE_INTEGRATION_ID)
            );
            givenTheRepositoryHydratesTheSelectedApis();

            // When the row's integration id is searched for, with no definition version narrowing
            var page = service.searchByIntegrationId(requestedId, null, null, new PageableImpl(1, 10));

            // Then only the apis that integration owns come back
            assertThat(page.getContent()).extracting(Api::getId).containsExactlyInAnyOrderElementsOf(expectedApiIds);
        }

        @Test
        void should_return_an_empty_page_without_querying_the_repository_when_the_index_matches_no_api() throws IOException {
            // Given the only indexed api is owned by another integration
            givenIndexedApis(anApiOwnedByIntegration("api-1", MIXED_CASE_INTEGRATION_ID));

            // When an integration owning no api is searched for
            var page = service.searchByIntegrationId("int-b", null, null, new PageableImpl(1, 10));

            // Then the repository is never asked to hydrate an empty id set, which it would read as no id restriction at all
            assertThat(page.getContent()).isEmpty();
            verifyNoInteractions(apiRepository);
        }

        @Test
        void should_fail_rather_than_report_an_empty_integration_when_the_index_is_unreachable() throws TechnicalException {
            // Given a search index that cannot be read
            var unreachableIndex = mock(ApiDocumentSearcher.class);
            var indexFailure = new TechnicalException("index unreachable");
            when(unreachableIndex.searchByIntegrationId(any(), any(), any())).thenThrow(indexFailure);

            // When an integration is searched for
            var serviceOverAnUnreachableIndex = new ApiQueryServiceImpl(apiRepository, unreachableIndex);
            var search = catchThrowable(() ->
                serviceOverAnUnreachableIndex.searchByIntegrationId(INTEGRATION_ID, null, null, new PageableImpl(1, 10))
            );

            // Then the failure surfaces instead of being flattened into a page indistinguishable from an empty integration
            assertThat(search).isInstanceOf(TechnicalManagementException.class).hasCause(indexFailure);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("definitionVersionRequests")
        void should_narrow_the_result_to_the_requested_definition_versions(
            String caseName,
            List<DefinitionVersion> requestedVersions,
            List<String> expectedApiIds
        ) throws IOException {
            // Given one api of each definition version, plus a legacy one whose version was never stored, owned by the same integration
            givenIndexedApis(
                anApiOwnedByIntegration(ApiFixtures.aProxyApiV2(), "api-v2", INTEGRATION_ID),
                anApiOwnedByIntegration(ApiFixtures.aProxyApiV4(), "api-v4", INTEGRATION_ID),
                anApiOwnedByIntegration(ApiFixtures.aFederatedApi(), "api-fed", INTEGRATION_ID),
                anApiOwnedByIntegration(ApiFixtures.aFederatedAgent(), "api-agent", INTEGRATION_ID),
                aLegacyApiWithoutDefinitionVersion("api-null-version")
            );
            givenTheRepositoryHydratesTheSelectedApis();

            // When that integration is searched for with the row's definition version narrowing
            var page = service.searchByIntegrationId(INTEGRATION_ID, requestedVersions, null, new PageableImpl(1, 10));

            // Then only the apis indexed under a requested definition version label come back
            assertThat(page.getContent()).extracting(Api::getId).containsExactlyInAnyOrderElementsOf(expectedApiIds);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("freeTextQueries")
        void should_match_the_query_against_the_name_the_description_and_the_agent_provider_organization(
            String caseName,
            String query,
            List<String> expectedApiIds
        ) throws IOException {
            // Given three apis of the same integration, each carrying the searched text in a different matched field
            givenIndexedApis(
                anApiNamed("api-name", "Alpha Billing Agent"),
                anApiDescribed("api-desc", "Handles invoice reconciliation"),
                anAgentOfOrganization("api-org", "Acme Robotics")
            );
            givenTheRepositoryHydratesTheSelectedApis();

            // When that integration is searched with the row's free text query
            var page = service.searchByIntegrationId(INTEGRATION_ID, null, query, new PageableImpl(1, 10));

            // Then only the api whose field carries that text comes back, and the two siblings it shares the integration with do not
            assertThat(page.getContent()).extracting(Api::getId).containsExactlyInAnyOrderElementsOf(expectedApiIds);
        }

        @Test
        void should_never_match_an_api_the_searched_integration_does_not_own() throws IOException {
            // Given the only api carrying the searched text is owned by another integration
            givenIndexedApis(
                anApiOwnedByIntegration("api-b1", "int-b").toBuilder().name("Alpha Agent").build(),
                anApiNamed("api-a1", "Beta Runner")
            );
            givenTheRepositoryHydratesTheSelectedApis();

            // When the integration owning no api of that text is searched for it
            var page = service.searchByIntegrationId(INTEGRATION_ID, null, "alpha", new PageableImpl(1, 10));

            // Then the page is empty, the integration filter still narrowing the free text clause
            assertThat(page.getContent()).isEmpty();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("singleAgentQueries")
        void should_match_only_when_one_matched_field_the_agent_carries_contains_the_query(
            String caseName,
            String name,
            String description,
            FederatedAgent.Provider provider,
            String query,
            List<String> expectedApiIds
        ) throws IOException {
            // Given one agent of the integration carrying the row's name, description and agent card provider
            givenIndexedApis(anAgent("api-1", name, description, provider));
            givenTheRepositoryHydratesTheSelectedApis();

            // When that integration is searched with the row's free text query
            var page = service.searchByIntegrationId(INTEGRATION_ID, null, query, new PageableImpl(1, 10));

            // Then only one of the three matched fields brings it back, another indexed field never does, and an absent one indexed fine
            assertThat(page.getContent()).extracting(Api::getId).containsExactlyInAnyOrderElementsOf(expectedApiIds);
        }

        @Test
        void should_match_an_upper_case_query_against_every_matched_field() throws IOException {
            // Given three apis of the integration, each carrying a capitalised Alpha in a different matched field
            givenIndexedApis(
                anApiNamed("api-name", "Alpha Agent"),
                anApiDescribed("api-desc", "Alpha workload handler"),
                anAgentOfOrganization("api-org", "Alpha Robotics")
            );
            givenTheRepositoryHydratesTheSelectedApis();

            // When the integration is searched with the upper case form of that text
            var page = service.searchByIntegrationId(INTEGRATION_ID, null, "ALPHA", new PageableImpl(1, 10));

            // Then all three come back, query and indexed field having been folded to the same case
            assertThat(page.getContent()).extracting(Api::getId).containsExactlyInAnyOrder("api-name", "api-desc", "api-org");
        }

        @Test
        void should_narrow_by_the_requested_definition_versions_and_the_query_together() throws IOException {
            // Given two identically named apis of the integration differing only in definition version
            givenIndexedApis(
                anApiOwnedByIntegration(ApiFixtures.aFederatedApi(), "api-fed", INTEGRATION_ID).toBuilder().name("Alpha").build(),
                anApiOwnedByIntegration(ApiFixtures.aFederatedAgent(), "api-agent", INTEGRATION_ID).toBuilder().name("Alpha").build()
            );
            givenTheRepositoryHydratesTheSelectedApis();

            // When the integration is searched for that name under one of the two definition versions
            var page = service.searchByIntegrationId(
                INTEGRATION_ID,
                List.of(DefinitionVersion.FEDERATED_AGENT),
                "alpha",
                new PageableImpl(1, 10)
            );

            // Then only the api satisfying both narrowings comes back, the two being combined rather than alternative
            assertThat(page.getContent()).extracting(Api::getId).containsExactly("api-agent");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("blankQueries")
        void should_narrow_nothing_when_the_query_carries_no_text(String caseName, String query) throws IOException {
            // Given four apis of the integration, one of them carrying none of the three matched fields
            givenIndexedApis(
                anApiNamed("api-name", "Alpha Billing Agent"),
                anApiDescribed("api-desc", "Handles invoice reconciliation"),
                anAgentOfOrganization("api-org", "Acme Robotics"),
                anAgent("api-bare", null, null, null)
            );
            givenTheRepositoryHydratesTheSelectedApis();

            // When the integration is searched with the row's blank query
            var page = service.searchByIntegrationId(INTEGRATION_ID, null, query, new PageableImpl(1, 10));

            // Then every api comes back, including the one no wildcard term could have matched
            assertThat(page.getContent()).extracting(Api::getId).containsExactlyInAnyOrder("api-name", "api-desc", "api-org", "api-bare");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("punctuationQueries")
        void should_match_every_character_of_the_query_as_literal_text(String caseName, String literalName, String decoyName, String query)
            throws IOException {
            // Given one api whose name holds the row's punctuation literally and one the row's query only reaches if it is interpreted
            givenIndexedApis(anApiNamed("query-literal", literalName), anApiNamed("query-decoy", decoyName));
            givenTheRepositoryHydratesTheSelectedApis();

            // When the integration is searched for the row's query
            var page = service.searchByIntegrationId(INTEGRATION_ID, null, query, new PageableImpl(1, 10));

            // Then only the api holding that punctuation literally comes back, never the decoy
            assertThat(page.getContent()).extracting(Api::getId).containsExactly("query-literal");
        }

        private static Stream<Arguments> punctuationQueries() {
            return Stream.of(
                Arguments.of("an asterisk never expands to a run of characters", "v*1 Agent", "vX1 Agent", "v*1"),
                Arguments.of("a question mark never expands to a single character", "v?1 Agent", "vX1 Agent", "v?1"),
                Arguments.of("a backslash never escapes the character behind it", "C:\\Agent", "C:Agent", "C:\\Agent"),
                Arguments.of("a percent sign is ordinary text", "50% Complete Agent", "500 Complete Agent", "50%"),
                Arguments.of("an underscore is ordinary text", "v_1 Agent", "vX1 Agent", "v_1"),
                Arguments.of("a bracketed character class is ordinary text", "tier[a] Agent", "tierXaX Agent", "tier[a]"),
                Arguments.of("a dot is ordinary text", "Delta.One Agent", "DeltaXOne Agent", "delta.one"),
                Arguments.of("a dollar sign is ordinary text", "cost$ Agent", "cost Agent", "cost$")
            );
        }

        private static Stream<Arguments> blankQueries() {
            return Stream.of(
                Arguments.of("an empty query narrows nothing", ""),
                Arguments.of("a whitespace only query narrows nothing", "   ")
            );
        }

        private static Stream<Arguments> singleAgentQueries() {
            return Stream.of(
                Arguments.of(
                    "a null name leaves the description free to match",
                    null,
                    "Alpha workload handler",
                    aProviderOf("Globex"),
                    "alpha",
                    List.of("api-1")
                ),
                Arguments.of(
                    "a null description leaves the name free to match",
                    "Alpha Agent",
                    null,
                    aProviderOf("Globex"),
                    "alpha",
                    List.of("api-1")
                ),
                Arguments.of(
                    "an agent card with no provider at all leaves the name free to match",
                    "Alpha Agent",
                    "handles nothing of note",
                    null,
                    "alpha",
                    List.of("api-1")
                ),
                Arguments.of(
                    "a provider whose organization is null indexes without error and matches nothing",
                    "Beta Runner",
                    "handles nothing of note",
                    aProviderOf(null),
                    "acme",
                    List.of()
                ),
                Arguments.of(
                    "a provider whose organization is empty indexes without error and matches nothing",
                    "Beta Runner",
                    "handles nothing of note",
                    aProviderOf(""),
                    "acme",
                    List.of()
                ),
                Arguments.of(
                    "a provider whose organization is whitespace only indexes without error and matches nothing",
                    "Beta Runner",
                    "handles nothing of note",
                    aProviderOf("   "),
                    "acme",
                    List.of()
                ),
                Arguments.of(
                    "a text occurring in none of the three matched fields matches nothing",
                    "Beta Runner",
                    "handles nothing of note",
                    aProviderOf("Globex"),
                    "acme",
                    List.of()
                ),
                Arguments.of(
                    "a text held only by an indexed field outside the three matched ones matches nothing",
                    "Beta Runner",
                    "handles nothing of note",
                    aProviderOf("Globex"),
                    THE_LABEL_EVERY_SEEDED_API_CARRIES,
                    List.of()
                ),
                Arguments.of(
                    "a multi word text whose two words live in two different fields of one api matches nothing",
                    "Alpha Billing",
                    "Agent onboarding notes",
                    null,
                    "Alpha Agent",
                    List.of()
                ),
                Arguments.of(
                    "a text starting and ending mid word inside a name matches that api",
                    "Global Alpha Agent",
                    "handles nothing of note",
                    aProviderOf("Globex"),
                    "lpha",
                    List.of("api-1")
                ),
                Arguments.of(
                    "a multi word text one field holds contiguously matches that api",
                    "Global Alpha Agent",
                    "handles nothing of note",
                    aProviderOf("Globex"),
                    "Alpha Agent",
                    List.of("api-1")
                ),
                Arguments.of(
                    "a text occurring only in the provider organization matches that agent",
                    "Beta Runner",
                    "handles nothing of note",
                    aProviderOf("Acme Robotics"),
                    "acme",
                    List.of("api-1")
                ),
                Arguments.of(
                    "a text starting mid word and spanning the space of a provider organization matches that agent",
                    "Beta Runner",
                    "handles nothing of note",
                    aProviderOf("Acme Robotics"),
                    "me Robo",
                    List.of("api-1")
                ),
                Arguments.of(
                    "a text padded with surrounding whitespace matches as its trimmed form does",
                    "Beta Runner",
                    "handles nothing of note",
                    aProviderOf("Acme Robotics"),
                    "  acme  ",
                    List.of("api-1")
                ),
                Arguments.of(
                    "an agent card with no provider at all indexes without error and matches nothing",
                    "Beta Runner",
                    "handles nothing of note",
                    null,
                    "acme",
                    List.of()
                )
            );
        }

        private static Stream<Arguments> freeTextQueries() {
            return Stream.of(
                Arguments.of("a text occurring only in an api name matches that api alone", "billing", List.of("api-name")),
                Arguments.of("a text occurring only in an api description matches that api alone", "invoice", List.of("api-desc")),
                Arguments.of(
                    "a text occurring only in an agent card provider organization matches that api alone",
                    "robotics",
                    List.of("api-org")
                )
            );
        }

        private static Stream<Arguments> definitionVersionRequests() {
            return Stream.of(
                Arguments.of(
                    "a V2 request matches the legacy api with no definition version alongside the explicitly V2 one",
                    List.of(DefinitionVersion.V2),
                    List.of("api-v2", "api-null-version")
                ),
                Arguments.of(
                    "a V4 request matches only the api indexed under the 4.0.0 label",
                    List.of(DefinitionVersion.V4),
                    List.of("api-v4")
                ),
                Arguments.of(
                    "a FEDERATED request never matches the FEDERATED_AGENT api whose label it is a strict prefix of",
                    List.of(DefinitionVersion.FEDERATED),
                    List.of("api-fed")
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

        private static Stream<Arguments> integrationIdRequests() {
            return Stream.of(
                Arguments.of("a hyphenated uuid integration id matches the api it owns", UUID_INTEGRATION_ID, List.of("api-uuid")),
                Arguments.of("a mixed case integration id matches the api it owns", MIXED_CASE_INTEGRATION_ID, List.of("api-1")),
                Arguments.of("an all lower case variant of an existing integration id matches nothing", "int-a-2024", List.of())
            );
        }

        private void givenIndexedApis(Api... apis) throws IOException {
            var transformer = new IndexableApiDocumentTransformer();
            for (Api api : apis) {
                indexWriter.addDocument(transformer.transform(IndexableApi.builder().api(api).build()));
            }
            indexWriter.commit();
        }

        private Api anApiOwnedByIntegration(String apiId, String integrationId) {
            return anApiOwnedByIntegration(ApiFixtures.aFederatedApi(), apiId, integrationId);
        }

        private Api anApiOwnedByIntegration(Api api, String apiId, String integrationId) {
            return api.toBuilder().id(apiId).originContext(new OriginContext.Integration(integrationId)).build();
        }

        private Api anApiNamed(String apiId, String name) {
            return anApiOwnedByIntegration(apiId, INTEGRATION_ID).toBuilder().name(name).build();
        }

        private Api anApiDescribed(String apiId, String description) {
            return anApiOwnedByIntegration(apiId, INTEGRATION_ID).toBuilder().description(description).build();
        }

        private Api anAgentOfOrganization(String apiId, String organization) {
            var base = ApiFixtures.aFederatedAgent();
            return anAgent(apiId, base.getName(), base.getDescription(), aProviderOf(organization));
        }

        private Api anAgent(String apiId, String name, String description, FederatedAgent.Provider provider) {
            var agentCard = ((FederatedAgent) ApiFixtures.aFederatedAgent().getApiDefinitionValue()).toBuilder().provider(provider).build();
            return anApiOwnedByIntegration(ApiFixtures.aFederatedAgent(), apiId, INTEGRATION_ID)
                .toBuilder()
                .name(name)
                .description(description)
                .apiDefinitionValue(agentCard)
                .build();
        }

        private static FederatedAgent.Provider aProviderOf(String organization) {
            return new FederatedAgent.Provider(organization, "https://example.net");
        }

        private Api aLegacyApiWithoutDefinitionVersion(String apiId) {
            // The api definition has to go with the version: Api.toBuilder() re-derives definitionVersion from it,
            // so an api that kept its definition would silently come back as V2 however this builder chain is ordered.
            return anApiOwnedByIntegration(ApiFixtures.aProxyApiV2(), apiId, INTEGRATION_ID)
                .toBuilder()
                .apiDefinitionValue(null)
                .definitionVersion(null)
                .build();
        }

        private void givenTheRepositoryHydratesTheSelectedApis() {
            when(apiRepository.search(any(), any(), any(), any())).thenAnswer(invocation -> {
                var selectedIds = invocation.getArgument(0, ApiCriteria.class).getIds();
                var rows = selectedIds
                    .stream()
                    .map(id -> fixtures.repository.ApiFixtures.aFederatedApi().toBuilder().id(id).build())
                    .toList();
                return new Page<>(rows, 1, rows.size(), rows.size());
            });
        }
    }
}
