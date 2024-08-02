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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.service.impl.search.lucene.searcher.ApiDocumentSearcher.FIELD_API_TYPE_VALUE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_DEFINITION_VERSION;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import inmemory.ApiCrudServiceInMemory;
import inmemory.PageCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.rest.api.model.ApiPageEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.service.CommandService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.impl.search.SearchEngineServiceImpl;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.impl.search.configuration.SearchEngineConfiguration;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentSearcher;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer;
import io.gravitee.rest.api.service.impl.search.lucene.searcher.ApiDocumentSearcher;
import io.gravitee.rest.api.service.impl.search.lucene.searcher.PageDocumentSearcher;
import io.gravitee.rest.api.service.impl.search.lucene.searcher.UserDocumentSearcher;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.PageDocumentTransformer;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * @author Guillaume Cusnieux (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { SearchEngineServiceTest.TestConfig.class }, loader = AnnotationConfigContextLoader.class)
public class SearchEngineServiceTest {

    private static final String ENV_1 = "env-1";

    private static final ExecutionContext ENV_1_CONTEXT = new ExecutionContext(GraviteeContext.getDefaultOrganization(), ENV_1);

    @Autowired
    private SearchEngineService searchEngineService;

    private static boolean isIndexed = false;

    @Test
    public void shouldFindBestResultsWithApiName() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("My api 1").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(5);
        assertThat(matches.getDocuments()).containsExactly("api-1", "api-3", "api-4", "api-0", "api-2");
    }

    @Test
    public void shouldFindBestResultsWithApiNameInfo() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("My 1").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(5);
        assertThat(matches.getDocuments()).containsExactly("api-1", "api-3", "api-4", "api-0", "api-2");
    }

    @Test
    public void shouldFindBestResultsWithDescription() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("field").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(1);

        assertThat(matches.getDocuments()).containsExactly("api-4");
    }

    @Test
    public void shouldFindBestResultsWithCategory() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("machine-learning").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(3);
        assertThat(matches.getDocuments()).containsExactly("api-0", "api-2", "api-4");
    }

    @Test
    public void shouldNotFoundWithOwnerEmail() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("foobar-3@gravitee.io").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isZero();

        matches =
            searchEngineService.search(
                GraviteeContext.getExecutionContext(),
                QueryBuilder.create(ApiEntity.class).setQuery("*@*").setFilters(filters).build()
            );
        assertThat(matches.getHits()).isZero();
    }

    @Test
    public void shouldFindBestResultsWithWildcard() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("My api *").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(5);
        assertThat(matches.getDocuments()).containsExactly("api-0", "api-4", "api-1", "api-3", "api-2");
    }

    @Test
    public void shouldFindBestResultsWithOwnerName() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("Owner 3").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(2);
        assertThat(matches.getDocuments()).containsExactly("api-3", "api-4");
    }

    @Test
    public void shouldFindWithExplicitNameFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("name:\"My Awesome api / 1\"").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(1);

        assertThat(matches.getDocuments()).containsExactly("api-1");
    }

    @Test
    public void should_delete_api_only_with_id() {
        Map<String, Object> filters = new HashMap<>();

        SearchResult beforeDeletion = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("name:\"My Awesome api / 1\"").setFilters(filters).build()
        );
        searchEngineService.delete(GraviteeContext.getExecutionContext(), ApiEntity.builder().id("api-1").build(), true);
        SearchResult afterDeletion = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("name:\"My Awesome api / 1\"").setFilters(filters).build()
        );

        assertThat(beforeDeletion.getHits()).isEqualTo(1);
        assertThat(beforeDeletion.getDocuments()).containsExactly("api-1");
        assertThat(afterDeletion.getHits()).isEqualTo(0);

        // force reindex after delete
        isIndexed = false;
    }

    @Test
    public void shouldFindWithExplicitNameUnSensitiveFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("name:\"my awesome api / 1\"").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(1);

        assertThat(matches.getDocuments()).containsExactly("api-1");
    }

    @Test
    public void shouldFindWithExplicitNameWildcardFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("name:\"my * api * 1\"").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(1);

        assertThat(matches.getDocuments()).containsExactly("api-1");
    }

    @Test
    public void shouldNotFindWithExplicitWrongNameFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("name:\"My api not found\"").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isZero();
    }

    @Test
    public void shouldFindWithExplicitDescriptionFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("description:\"Field Hockey\"").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(1);

        assertThat(matches.getDocuments()).containsExactly("api-4");
    }

    @Test
    public void shouldFindWithLabelsFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("labels: \"In Review 1\"").setFilters(filters).build()
        );

        assertThat(matches.getHits()).isEqualTo(4);
        assertThat(matches.getDocuments()).containsExactly("api-1", "api-2", "api-3", "api-4");

        matches =
            searchEngineService.search(
                GraviteeContext.getExecutionContext(),
                QueryBuilder
                    .create(ApiEntity.class)
                    .setQuery("labels: \"In Review 1\" AND labels: \"In Review 4\"")
                    .setFilters(filters)
                    .build()
            );
        assertThat(matches.getHits()).isEqualTo(1);

        assertThat(matches.getDocuments()).containsExactly("api-4");

        matches =
            searchEngineService.search(
                GraviteeContext.getExecutionContext(),
                QueryBuilder
                    .create(ApiEntity.class)
                    .setQuery("labels: \"In Review 3\" OR labels: \"In Review 4\"")
                    .setFilters(filters)
                    .build()
            );
        assertThat(matches.getHits()).isEqualTo(2);
        assertThat(matches.getDocuments()).containsExactly("api-3", "api-4");

        matches =
            searchEngineService.search(
                GraviteeContext.getExecutionContext(),
                QueryBuilder
                    .create(ApiEntity.class)
                    .setQuery("labels: \"in review 3\" OR labels: \"in review 4\"")
                    .setFilters(filters)
                    .build()
            );
        assertThat(matches.getHits()).isEqualTo(2);
        assertThat(matches.getDocuments()).containsExactly("api-3", "api-4");
    }

    @Test
    public void shouldFindWithLabelsAndPhraseFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("labels: \"in review 4\" foobar-3@gravitee.io").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(1);

        assertThat(matches.getDocuments()).containsExactly("api-4");
    }

    @Test
    public void shouldFindWithCategoriesFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("categories:\"Machine Learning\"").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(3);
        assertThat(matches.getDocuments()).containsExactly("api-0", "api-2", "api-4");
    }

    @Test
    public void shouldFindWithCategoriesFilterAndText() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("categories: Sports AND Hiking").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(1);

        assertThat(matches.getDocuments()).containsExactly("api-0");
    }

    @Test
    public void shouldFindWithCategoriesWildcardFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("categories: *").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(3);
        assertThat(matches.getDocuments()).containsExactly("api-0", "api-2", "api-4");
    }

    @Test
    public void shouldFindWithNameAndOwnerFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder
                .create(ApiEntity.class)
                .setQuery("name:\"http://localhost/api-2\" AND ownerName: \"Owner 2\"")
                .setFilters(filters)
                .build()
        );
        assertThat(matches.getHits()).isEqualTo(1);

        assertThat(matches.getDocuments()).containsExactly("api-2");
    }

    @Test
    public void shouldNotFindWithNameAndOwnerFilterIfApiIsExcluded() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("api", Collections.singleton("api-1"));
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("name:\"My api 2\" AND ownerName: \"Owner 2\"").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isZero();
    }

    @Test
    public void shouldNotFoundWithNameAndWrongOwnerFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("name:\"My api 1\" AND ownerName: \"Owner 2\"").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isZero();
    }

    @Test
    public void shouldFindText() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("Hiking").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(1);

        assertThat(matches.getDocuments()).containsExactly("api-0");
    }

    @Test
    public void shouldFindWithPageContent() {
        Map<String, Object> filters = new HashMap<>();
        filters.put(FIELD_API_TYPE_VALUE, Arrays.asList("api-1", "api-2"));
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("documentation").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(1);

        assertThat(matches.getDocuments()).containsExactly("api-1");
    }

    @Test
    public void shouldFindWithPageContentAndFilteredByEnv() {
        Map<String, Object> filters = new HashMap<>();
        filters.put(FIELD_API_TYPE_VALUE, Arrays.asList("api-1", "api-2", "api-5", "api-6"));
        SearchResult matches = searchEngineService.search(
            ENV_1_CONTEXT,
            QueryBuilder.create(ApiEntity.class).setQuery("documentation").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(1);

        assertThat(matches.getDocuments()).containsExactly("api-5");
    }

    @Test
    public void shouldFindWithApiIdAndFilteredByEnv() {
        Map<String, Object> filters = new HashMap<>();
        filters.put(FIELD_API_TYPE_VALUE, Arrays.asList("api-1", "api-2", "api-7"));
        SearchResult matches = searchEngineService.search(ENV_1_CONTEXT, QueryBuilder.create(ApiEntity.class).setFilters(filters).build());
        assertThat(matches.getHits()).isEqualTo(1);

        assertThat(matches.getDocuments()).containsExactly("api-7");
    }

    @Test
    public void shouldFindAll() {
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).build()
        );
        assertThat(matches.getHits()).isEqualTo(5);
        assertThat(matches.getDocuments()).containsExactly("api-0", "api-1", "api-2", "api-3", "api-4");
    }

    @Test
    public void shouldFindAllOnEnv1() {
        SearchResult matches = searchEngineService.search(ENV_1_CONTEXT, QueryBuilder.create(ApiEntity.class).build());
        assertThat(matches.getHits()).isEqualTo(5);
        assertThat(matches.getDocuments()).containsExactly("api-5", "api-6", "api-7", "api-8", "api-9");
    }

    @Test
    public void shouldFindWithContextPath() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("/path/api-2").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(1);

        assertThat(matches.getDocuments()).containsExactly("api-2");
    }

    @Test
    public void shouldFindByContextPath() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).addExplicitFilter("paths", "/path/api-2").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(1);

        assertThat(matches.getDocuments()).containsExactly("api-2");
    }

    @Test
    public void shouldFindByContextPathWildcard() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).addExplicitFilter("paths", "*th/api-2").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(1);

        assertThat(matches.getDocuments()).containsExactly("api-2");
    }

    @Test
    public void shouldFindWithTagWildcard() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("tag-api-").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(5);
        assertThat(matches.getDocuments()).containsExactly("api-0", "api-1", "api-2", "api-3", "api-4");
    }

    @Test
    public void shouldFindByTag() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).addExplicitFilter("tags", "tag-api-3").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(1);
        assertThat(matches.getDocuments()).containsExactly("api-3");
    }

    @Test
    public void shouldFindBestResultsWithCategorySortByNameAsc() {
        Map<String, Object> filters = new HashMap<>();
        Sortable sortByName = new SortableImpl("name", true);
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("machine learning").setFilters(filters).setSort(sortByName).build()
        );
        assertThat(matches.getHits()).isEqualTo(3);
        assertThat(matches.getDocuments()).containsExactly("api-2", "api-0", "api-4");
    }

    @Test
    public void shouldFindBestResultsWithCategorySortByNameDesc() {
        Map<String, Object> filters = new HashMap<>();
        Sortable sortByName = new SortableImpl("name", false);
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("machine learning").setFilters(filters).setSort(sortByName).build()
        );
        assertThat(matches.getHits()).isEqualTo(3);
        assertThat(matches.getDocuments()).containsExactly("api-4", "api-0", "api-2");
    }

    @Test
    public void shouldFindBestResultsWithPageContentAndSortByNameDesc() {
        Map<String, Object> filters = new HashMap<>();
        Sortable sortByName = new SortableImpl("name", false);
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("documentation").setFilters(filters).setSort(sortByName).build()
        );

        assertThat(matches.getHits()).isEqualTo(2);

        assertThat(matches.getDocuments()).containsExactly("api-3", "api-1");
    }

    @Test
    public void shouldFindBestResultsWithPageContentAndSortByPathsAsc() {
        Map<String, Object> filters = new HashMap<>();
        Sortable sortByPath = new SortableImpl("paths", true);
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("documentation").setFilters(filters).setSort(sortByPath).build()
        );

        assertThat(matches.getHits()).isEqualTo(2);

        assertThat(matches.getDocuments()).containsExactly("api-1", "api-3");
    }

    @Test
    public void shouldFindBestResultsWithPageContentAndSortByPathsDesc() {
        Map<String, Object> filters = new HashMap<>();
        Sortable sortByPath = new SortableImpl("paths", false);
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("documentation").setFilters(filters).setSort(sortByPath).build()
        );

        assertThat(matches.getHits()).isEqualTo(2);

        assertThat(matches.getDocuments()).containsExactly("api-3", "api-1");
    }

    @Test
    public void shouldFindWithoutEnvironmentId() {
        Map<String, Object> filters = new HashMap<>();
        filters.put(FIELD_API_TYPE_VALUE, Arrays.asList("api-1", "api-2"));
        SearchResult matches = searchEngineService.search(
            new ExecutionContext(GraviteeContext.getCurrentOrganization(), null),
            QueryBuilder.create(ApiEntity.class).setFilters(filters).build()
        );

        assertThat(matches.getHits()).isEqualTo(2);
        assertThat(matches.getDocuments()).containsExactly("api-1", "api-2");
    }

    @Test
    public void shouldFindWithExcludedFilters_definitionVersion() {
        Map<String, Object> filters = new HashMap<>();
        filters.put(FIELD_API_TYPE_VALUE, Arrays.asList("api-1", "api-2"));
        Map<String, Collection<String>> excludedFilters = new HashMap<>();
        excludedFilters.put(FIELD_DEFINITION_VERSION, Arrays.asList(DefinitionVersion.V1.getLabel(), DefinitionVersion.V4.getLabel()));
        QueryBuilder<ApiEntity> apiEntityQueryBuilder = QueryBuilder
            .create(ApiEntity.class)
            .setFilters(filters)
            .setExcludedFilters(excludedFilters);
        SearchResult matches = searchEngineService.search(
            new ExecutionContext(GraviteeContext.getCurrentOrganization(), null),
            apiEntityQueryBuilder.build()
        );

        assertThat(matches.getHits()).isEqualTo(1);

        assertThat(matches.getDocuments()).containsExactly("api-1");
    }

    @Test
    public void shouldFindWithExcludedFilters_multiple() {
        Map<String, Object> filters = new HashMap<>();
        filters.put(FIELD_API_TYPE_VALUE, Arrays.asList("api-1", "api-2"));
        Map<String, Collection<String>> excludedFilters = new HashMap<>();
        excludedFilters.put(FIELD_NAME, List.of("My Awesome api / 1"));
        excludedFilters.put(FIELD_DEFINITION_VERSION, Arrays.asList(DefinitionVersion.V1.getLabel(), DefinitionVersion.V4.getLabel()));
        QueryBuilder<ApiEntity> apiEntityQueryBuilder = QueryBuilder
            .create(ApiEntity.class)
            .setFilters(filters)
            .setExcludedFilters(excludedFilters);
        SearchResult matches = searchEngineService.search(
            new ExecutionContext(GraviteeContext.getCurrentOrganization(), null),
            apiEntityQueryBuilder.build()
        );

        assertThat(matches.getHits()).isZero();
    }

    @Before
    public void initIndexer() {
        // TODO: Remove this hack and use @BeforeAll when move to junit 5.x
        if (!isIndexed) {
            List<String> labels = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                labels.add("In Review " + i);
                ApiEntity apiEntity = createApiEntity(i, labels, GraviteeContext.getCurrentEnvironment());
                searchEngineService.index(GraviteeContext.getExecutionContext(), apiEntity, true, false);
            }

            for (int i = 5; i < 10; i++) {
                ApiEntity apiEntity = createApiEntity(i, labels, ENV_1);
                searchEngineService.index(GraviteeContext.getExecutionContext(), apiEntity, true, false);
            }

            searchEngineService.index(GraviteeContext.getExecutionContext(), completePage(new ApiPageEntity(), 1, true), true, false);
            searchEngineService.index(GraviteeContext.getExecutionContext(), completePage(new PageEntity(), 2, true), true, false);
            searchEngineService.index(GraviteeContext.getExecutionContext(), completePage(new ApiPageEntity(), 3, false), true, false);
            searchEngineService.index(GraviteeContext.getExecutionContext(), completePage(new ApiPageEntity(), 5, true), true, false);
            searchEngineService.commit();
            isIndexed = true;
        }
    }

    private static ApiEntity createApiEntity(int index, List<String> labels, String envId) {
        String apiName = index == 2 ? "http://localhost/api-" + index : "My Awesome api / " + index;
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId("api-" + index);
        apiEntity.setReferenceId(envId);
        apiEntity.setReferenceType(ReferenceContext.Type.ENVIRONMENT.name());
        apiEntity.setName(apiName);
        apiEntity.setUpdatedAt(new Date());
        apiEntity.setLabels(labels);
        apiEntity.setDescription(DESCRIPTIONS[index % DESCRIPTIONS.length]);

        Proxy proxy = new Proxy();
        List<VirtualHost> hosts = new ArrayList<>();
        VirtualHost host = new VirtualHost();
        host.setPath("/path/" + apiEntity.getId());
        hosts.add(host);
        proxy.setVirtualHosts(hosts);
        apiEntity.setProxy(proxy);
        PrimaryOwnerEntity owner = new PrimaryOwnerEntity();
        owner.setId("user-" + index);
        owner.setDisplayName("Owner " + index);
        owner.setEmail("foobar-" + index + "@gravitee.io");
        apiEntity.setPrimaryOwner(owner);
        if (index % 2 == 0) {
            // Actually we index hrid categories...
            apiEntity.setCategories(Set.of("sports", "game", "machine-learning"));
            apiEntity.setGraviteeDefinitionVersion(DefinitionVersion.V1.getLabel());
        } else {
            apiEntity.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        }

        apiEntity.setTags(Set.of("tag-" + apiEntity.getId()));
        return apiEntity;
    }

    public PageEntity completePage(PageEntity pageEntity, int i, boolean published) {
        pageEntity.setId("page-" + i);
        pageEntity.setName("Gravitee documentation");
        pageEntity.setContent("documentation");
        if (pageEntity instanceof ApiPageEntity) {
            pageEntity.setReferenceType("API");
            pageEntity.setReferenceId("api-" + i);
        } else {
            pageEntity.setReferenceId(GraviteeContext.getCurrentEnvironment());
            pageEntity.setReferenceType(ReferenceContext.Type.ENVIRONMENT.name());
        }

        pageEntity.setVisibility(Visibility.PUBLIC);
        pageEntity.setPublished(published);
        return pageEntity;
    }

    @Configuration
    @Import({ SearchEngineConfiguration.class }) // the actual configuration
    public static class TestConfig {

        @Bean
        public Directory indexDirectory() throws IOException {
            Path path = Path.of("target/" + SearchEngineServiceTest.class.getCanonicalName());
            if (!path.toFile().exists()) {
                path.toFile().mkdirs();
            }
            return FSDirectory.open(path);
        }

        @Bean
        public ApiDocumentSearcher apiDocumentSearcher() {
            return new ApiDocumentSearcher();
        }

        @Bean
        public PageDocumentSearcher pageDocumentSearcher() {
            return new PageDocumentSearcher();
        }

        @Bean
        public UserDocumentSearcher userDocumentSearcher() {
            return new UserDocumentSearcher();
        }

        @Bean
        public SearchEngineService searchEngineService() {
            return new SearchEngineServiceImpl();
        }

        @Bean
        public Collection<DocumentTransformer> transformers() {
            return Arrays.asList(new ApiDocumentTransformer(new ApiServiceImpl()), new PageDocumentTransformer());
        }

        @Bean
        public Collection<DocumentSearcher> searchers() {
            return Arrays.asList(new ApiDocumentSearcher(), new PageDocumentSearcher(), new UserDocumentSearcher());
        }

        @Bean
        public CommandService commandService() {
            return mock(CommandService.class);
        }

        @Bean
        public PageCrudService pageCrudService() {
            return new PageCrudServiceInMemory();
        }

        @Bean
        public ApiCrudServiceInMemory apiCrudService() {
            return new ApiCrudServiceInMemory();
        }

        @Bean
        public ApiIndexerDomainService apiIndexerDomainService() {
            return mock(ApiIndexerDomainService.class);
        }
    }

    private static final String[] DESCRIPTIONS = { "Hiking", "Biking", "Running", "", "Field Hockey" };
}
