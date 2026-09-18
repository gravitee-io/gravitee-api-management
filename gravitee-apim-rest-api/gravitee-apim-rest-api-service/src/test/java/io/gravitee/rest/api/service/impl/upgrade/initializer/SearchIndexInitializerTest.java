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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import static io.gravitee.repository.management.model.UserStatus.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.repository.ApiFixtures;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.api_product.domain_service.ApiProductIndexerDomainService;
import io.gravitee.apim.core.search.Indexer;
import io.gravitee.apim.core.search.model.IndexableApi;
import io.gravitee.apim.core.search.model.IndexableApiProduct;
import io.gravitee.apim.infra.query_service.api.ApiQueryServiceImpl;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.api.ApiProductsRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.User;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.UserMetadataService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.UserConverter;
import io.gravitee.rest.api.service.exceptions.PrimaryOwnerNotFoundException;
import io.gravitee.rest.api.service.impl.search.SearchEngineServiceImpl;
import io.gravitee.rest.api.service.impl.search.lucene.SearchEngineIndexer;
import io.gravitee.rest.api.service.impl.search.lucene.searcher.ApiDocumentSearcher;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.IndexableApiDocumentTransformer;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.IndexableApiProductDocumentTransformer;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.GenericApiMapper;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class SearchIndexInitializerTest {

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private PageService pageService;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private ApiMapper apiMapper;

    @Mock
    private ApiConverter apiConverter;

    @Mock
    private ApiIndexerDomainService apiIndexerDomainService;

    @Mock
    private ApiProductIndexerDomainService apiProductIndexerDomainService;

    @Mock
    private ApiProductsRepository apiProductsRepository;

    @Mock
    private UserMetadataService userMetadataService;

    private final PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity();

    private SearchIndexInitializer initializer;

    @BeforeEach
    public void setup() throws Exception {
        initializer = anInitializerIndexingWith(searchEngineService);

        givenExistingEnvironments(
            Environment.builder().id("env1").organizationId("org1").build(),
            Environment.builder().id("env2").organizationId("org2").build(),
            Environment.builder().id("env3").organizationId("org1").build()
        );

        lenient().when(primaryOwnerService.getPrimaryOwner(any(), any())).thenReturn(primaryOwnerEntity);
    }

    @Nested
    class RunApisIndexationAsync {

        @Test
        public void runApisIndexationAsync_should_index_every_api() throws Exception {
            givenExistingApis(
                ApiFixtures.aV2Api().toBuilder().id("api1").environmentId("env1").build(),
                ApiFixtures.aV2Api().toBuilder().id("api2").environmentId("env2").build(),
                ApiFixtures.aV4Api().toBuilder().id("api3").environmentId("env1").build(),
                ApiFixtures.aV4Api().toBuilder().id("api4").environmentId("env3").build()
            );

            initializer.runApisIndexationAsync(Executors.newSingleThreadExecutor()).forEach(CompletableFuture::join);

            verify(searchEngineService, times(1)).index(
                argThat(e -> e.hasEnvironmentId() && e.getEnvironmentId().equals("env1") && e.getOrganizationId().equals("org1")),
                argThat(api -> api.getId().equals("api1")),
                eq(true),
                eq(false)
            );
            verify(searchEngineService, times(1)).index(
                argThat(e -> e.hasEnvironmentId() && e.getEnvironmentId().equals("env2") && e.getOrganizationId().equals("org2")),
                argThat(api -> api.getId().equals("api2")),
                eq(true),
                eq(false)
            );
            verify(searchEngineService, times(1)).index(
                argThat(e -> e.hasEnvironmentId() && e.getEnvironmentId().equals("env1") && e.getOrganizationId().equals("org1")),
                argThat(api -> api.getId().equals("api3")),
                eq(true),
                eq(false)
            );
            verify(searchEngineService, times(1)).index(
                argThat(e -> e.hasEnvironmentId() && e.getEnvironmentId().equals("env3") && e.getOrganizationId().equals("org1")),
                argThat(api -> api.getId().equals("api4")),
                eq(true),
                eq(false)
            );
        }

        @Test
        public void runApisIndexationAsync_should_index_every_api_even_if_primaryOwner_not_found() throws Exception {
            givenExistingApis(
                ApiFixtures.aV2Api().toBuilder().id("api1").environmentId("env1").build(),
                ApiFixtures.aV4Api().toBuilder().id("api2").environmentId("env2").build()
            );
            when(primaryOwnerService.getPrimaryOwner(any(), any())).thenThrow(PrimaryOwnerNotFoundException.class);

            initializer.runApisIndexationAsync(Executors.newSingleThreadExecutor()).forEach(CompletableFuture::join);

            verify(searchEngineService, times(2)).index(any(ExecutionContext.class), any(Indexable.class), anyBoolean(), anyBoolean());
        }
    }

    @Nested
    class RunApisIndexationAsyncOverAnUnreadableAgentCard {

        private static final String INTEGRATION_ID = "int-a";

        private ByteBuffersDirectory indexDirectory;
        private IndexWriter indexWriter;
        private ApiQueryService apiSearch;
        private SearchEngineService luceneSearchEngine;

        @BeforeEach
        void openTheIndexTheRebuildWritesInto() throws Exception {
            indexDirectory = new ByteBuffersDirectory();
            indexWriter = new IndexWriter(indexDirectory, new IndexWriterConfig(new StandardAnalyzer()));
            apiSearch = new ApiQueryServiceImpl(apiRepository, new ApiDocumentSearcher(indexWriter));

            luceneSearchEngine = spy(aSearchEngineWritingInto(indexWriter));
            initializer = anInitializerIndexingWith(luceneSearchEngine);
        }

        @AfterEach
        void closeTheIndex() throws Exception {
            indexWriter.close();
        }

        private SearchEngineServiceImpl aSearchEngineWritingInto(IndexWriter writer) {
            var indexer = new SearchEngineIndexer(writer);

            var searchEngine = new SearchEngineServiceImpl();
            ReflectionTestUtils.setField(searchEngine, "indexer", indexer);
            ReflectionTestUtils.setField(
                searchEngine,
                "transformers",
                List.of(new IndexableApiProductDocumentTransformer(), new IndexableApiDocumentTransformer())
            );
            return searchEngine;
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("searchesOverTheRebuiltIndex")
        void should_index_the_readable_agent_of_a_rebuild_that_met_an_unreadable_one_first(String caseName, String query) throws Exception {
            givenAnAgentOfAcmeRoboticsBehindAnUnreadableOne();
            givenTheRepositoryHydratesTheApisTheIndexReturns();

            whenTheRebuildRunsToTheEndDespiteAFailingRow();

            var page = apiSearch.searchByIntegrationId("env1", INTEGRATION_ID, null, query, new PageableImpl(1, 10));
            assertThat(page.getContent())
                .extracting(api -> api.getId())
                .containsExactly("api-ok");
        }

        private static Stream<Arguments> searchesOverTheRebuiltIndex() {
            return Stream.of(
                Arguments.of("the readable agent answers a search for the organization its card carries", "acme"),
                Arguments.of("nothing of the unreadable agent answers a search narrowed by its integration alone", null)
            );
        }

        @Test
        void should_fail_the_indexation_of_the_unreadable_agent_alone_and_index_the_readable_one() throws Exception {
            givenAnAgentOfAcmeRoboticsBehindAnUnreadableOne();

            var futures = whenTheRebuildRunsToTheEndDespiteAFailingRow();

            assertThat(futures).filteredOn(CompletableFuture::isCompletedExceptionally).hasSize(1);
            verify(luceneSearchEngine, never()).index(
                any(ExecutionContext.class),
                argThat(indexable -> "api-broken".equals(indexable.getId())),
                anyBoolean(),
                anyBoolean()
            );
            verify(luceneSearchEngine, times(1)).index(
                any(ExecutionContext.class),
                argThat(indexable -> "api-ok".equals(indexable.getId())),
                anyBoolean(),
                anyBoolean()
            );
        }

        @Test
        void should_write_the_readable_agent_as_an_api_document_that_stays_uncommitted_until_the_rebuild_commits() throws Exception {
            givenAnAgentOfAcmeRoboticsBehindAnUnreadableOne();

            whenTheRebuildRunsToTheEndDespiteAFailingRow();

            assertThat(DirectoryReader.indexExists(indexDirectory)).isFalse();

            luceneSearchEngine.commit();

            try (var committedIndex = DirectoryReader.open(indexDirectory)) {
                assertThat(committedIndex.numDocs()).isEqualTo(1);
                var document = committedIndex.storedFields().document(0);
                assertThat(document.get("id")).isEqualTo("api-ok");
                assertThat(document.get("type")).isEqualTo("api");
            }
        }

        private List<CompletableFuture<?>> whenTheRebuildRunsToTheEndDespiteAFailingRow() throws Exception {
            var futures = initializer.runApisIndexationAsync(Executors.newSingleThreadExecutor());
            futures.forEach(future -> future.exceptionally(throwable -> null).join());
            return futures;
        }

        private void givenAnAgentOfAcmeRoboticsBehindAnUnreadableOne() {
            givenExistingAgents(
                anAgentRow("api-broken", "not-an-agent-card"),
                anAgentRow(
                    "api-ok",
                    """
                    {"name":"Task Management","description":"handles tasks","provider":{"organization":"Acme Robotics","url":"https://example.net"}}"""
                )
            );
        }

        private void givenExistingAgents(Api... apis) {
            when(apiRepository.search(any(ApiCriteria.class), eq(null), any(ApiFieldFilter.class))).thenReturn(Stream.of(apis));
            when(apiIndexerDomainService.toIndexableApi(any(Indexer.IndexationContext.class), any())).thenAnswer(invocation ->
                new IndexableApi(invocation.getArgument(1), null, Collections.emptyMap(), Collections.emptyList())
            );
        }

        private void givenTheRepositoryHydratesTheApisTheIndexReturns() {
            when(apiRepository.search(any(ApiCriteria.class), any(), any(), any())).thenAnswer(invocation -> {
                var selectedIds = invocation.getArgument(0, ApiCriteria.class).getIds();
                var rows = selectedIds
                    .stream()
                    .map(id -> Api.builder().id(id).build())
                    .toList();
                return new Page<>(rows, 0, rows.size(), rows.size());
            });
        }

        private Api anAgentRow(String apiId, String definition) {
            return ApiFixtures.aFederatedApi()
                .toBuilder()
                .id(apiId)
                .environmentId("env1")
                .definitionVersion(DefinitionVersion.FEDERATED_AGENT)
                .definition(definition)
                .origin("integration")
                .integrationId(INTEGRATION_ID)
                .visibility(Visibility.PUBLIC)
                .build();
        }
    }

    @Nested
    class RunApiProductsIndexation {

        @Test
        public void runApiProductsIndexationAsync_should_index_every_api_product() throws Exception {
            givenExistingApiProducts(
                io.gravitee.repository.management.model.ApiProduct.builder()
                    .id("product-1")
                    .environmentId("env1")
                    .name("Product 1")
                    .build(),
                io.gravitee.repository.management.model.ApiProduct.builder()
                    .id("product-2")
                    .environmentId("env2")
                    .name("Product 2")
                    .build(),
                io.gravitee.repository.management.model.ApiProduct.builder().id("product-3").environmentId("env1").name("Product 3").build()
            );

            initializer.runApiProductsIndexationAsync(Executors.newSingleThreadExecutor()).forEach(CompletableFuture::join);

            verify(searchEngineService, times(1)).index(
                argThat(e -> e.hasEnvironmentId() && e.getEnvironmentId().equals("env1") && e.getOrganizationId().equals("org1")),
                argThat(
                    indexable ->
                        indexable instanceof IndexableApiProduct &&
                        "product-1".equals(((IndexableApiProduct) indexable).getApiProduct().getId())
                ),
                eq(true),
                eq(false)
            );
            verify(searchEngineService, times(1)).index(
                argThat(e -> e.hasEnvironmentId() && e.getEnvironmentId().equals("env2") && e.getOrganizationId().equals("org2")),
                argThat(
                    indexable ->
                        indexable instanceof IndexableApiProduct &&
                        "product-2".equals(((IndexableApiProduct) indexable).getApiProduct().getId())
                ),
                eq(true),
                eq(false)
            );
            verify(searchEngineService, times(1)).index(
                argThat(e -> e.hasEnvironmentId() && e.getEnvironmentId().equals("env1") && e.getOrganizationId().equals("org1")),
                argThat(
                    indexable ->
                        indexable instanceof IndexableApiProduct &&
                        "product-3".equals(((IndexableApiProduct) indexable).getApiProduct().getId())
                ),
                eq(true),
                eq(false)
            );
        }
    }

    @Nested
    class RunUsersIndexationAsync {

        @Test
        public void runUsersIndexationAsync_should_index_every_user() throws Exception {
            givenExistingUsers(
                User.builder().id("user1").organizationId("org1").build(),
                User.builder().id("user2").organizationId("org2").build(),
                User.builder().id("user3").organizationId("org1").build(),
                User.builder().id("user4").organizationId("org3").build()
            );

            initializer.runUsersIndexationAsync(Executors.newSingleThreadExecutor()).forEach(CompletableFuture::join);

            verify(searchEngineService, times(1)).index(
                argThat(e -> !e.hasEnvironmentId() && e.getOrganizationId().equals("org1")),
                argThat(user -> user.getId().equals("user1")),
                eq(true),
                eq(false)
            );
            verify(searchEngineService, times(1)).index(
                argThat(e -> !e.hasEnvironmentId() && e.getOrganizationId().equals("org2")),
                argThat(user -> user.getId().equals("user2")),
                eq(true),
                eq(false)
            );
            verify(searchEngineService, times(1)).index(
                argThat(e -> !e.hasEnvironmentId() && e.getOrganizationId().equals("org1")),
                argThat(user -> user.getId().equals("user3")),
                eq(true),
                eq(false)
            );
            verify(searchEngineService, times(1)).index(
                argThat(e -> !e.hasEnvironmentId() && e.getOrganizationId().equals("org3")),
                argThat(user -> user.getId().equals("user4")),
                eq(true),
                eq(false)
            );
        }
    }

    @Test
    public void testOrder() {
        assertThat(initializer.getOrder()).isEqualTo(InitializerOrder.SEARCH_INDEX_INITIALIZER);
    }

    private SearchIndexInitializer anInitializerIndexingWith(SearchEngineService indexingService) {
        return new SearchIndexInitializer(
            apiRepository,
            new GenericApiMapper(apiMapper, apiConverter),
            pageService,
            userRepository,
            indexingService,
            environmentRepository,
            apiConverter,
            new UserConverter(),
            primaryOwnerService,
            apiIndexerDomainService,
            apiProductIndexerDomainService,
            apiProductsRepository,
            userMetadataService
        );
    }

    private void givenExistingApiProducts(io.gravitee.repository.management.model.ApiProduct... products) throws Exception {
        when(apiProductsRepository.findAll()).thenReturn(java.util.Set.of(products));
        lenient()
            .when(
                apiProductIndexerDomainService.toIndexableApiProduct(
                    any(Indexer.IndexationContext.class),
                    any(io.gravitee.apim.core.api_product.model.ApiProduct.class)
                )
            )
            .thenAnswer(invocation -> {
                io.gravitee.apim.core.api_product.model.ApiProduct core = invocation.getArgument(1);
                return IndexableApiProduct.builder().apiProduct(core).build();
            });
    }

    private void givenExistingApis(Api... apis) {
        when(apiRepository.search(any(ApiCriteria.class), eq(null), any(ApiFieldFilter.class))).thenReturn(Stream.of(apis));

        Stream.of(apis).forEach(api -> {
            if (api.getDefinitionVersion() == DefinitionVersion.V4) {
                lenient()
                    .when(apiIndexerDomainService.toIndexableApi(any(Indexer.IndexationContext.class), any()))
                    .thenAnswer(invocation ->
                        new IndexableApi(invocation.getArgument(1), null, Collections.emptyMap(), Collections.emptyList())
                    );
            } else if (api.getDefinitionVersion() == DefinitionVersion.V2) {
                lenient()
                    .when(apiConverter.toApiEntity(any(), any(), any(), eq(false), eq(false), eq(true)))
                    .thenReturn(
                        ApiEntity.builder().id(api.getId()).referenceId(api.getEnvironmentId()).referenceType("ENVIRONMENT").build()
                    );
                lenient()
                    .when(apiConverter.toApiEntity(any(), any(), eq(true)))
                    .thenReturn(
                        ApiEntity.builder().id(api.getId()).referenceId(api.getEnvironmentId()).referenceType("ENVIRONMENT").build()
                    );
            }
        });
    }

    private void givenExistingUsers(User... users) throws Exception {
        when(
            userRepository.search(argThat(criteria -> criteria.getStatuses().length == 1 && criteria.getStatuses()[0] == ACTIVE), any())
        ).thenReturn(new Page<>(List.of(users), 0, users.length, users.length));
    }

    @SneakyThrows
    private void givenExistingEnvironments(Environment... environments) {
        for (Environment environment : environments) {
            lenient().when(environmentRepository.findById(environment.getId())).thenReturn(Optional.of(environment));
        }
    }
}
