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
            when(unreachableIndex.searchByIntegrationId(any(), any())).thenThrow(indexFailure);

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
