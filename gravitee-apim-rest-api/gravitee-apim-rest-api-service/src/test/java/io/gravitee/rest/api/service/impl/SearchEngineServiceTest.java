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
import static org.assertj.core.api.Assertions.in;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import inmemory.ApiCrudServiceInMemory;
import inmemory.PageCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_product.crud_service.ApiProductCrudService;
import io.gravitee.apim.core.api_product.domain_service.ApiProductIndexerDomainService;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.infra.query_service.api.ApiQueryServiceImpl;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.rest.api.model.ApiPageEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.context.OriginContext;
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
import java.util.stream.Stream;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * @author Guillaume Cusnieux (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SearchEngineServiceTest.TestConfig.class }, loader = AnnotationConfigContextLoader.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class SearchEngineServiceTest {

    private static final String ENV_1 = "env-1";

    private static final ExecutionContext ENV_1_CONTEXT = new ExecutionContext(GraviteeContext.getDefaultOrganization(), ENV_1);

    private static final String ENV_LEGACY_INTEGRATION = "env-legacy-integration";
    private static final String LEGACY_INTEGRATION_ID = "int-a";
    private static final String LEGACY_INTEGRATION_API_ID = "api-legacy-integration";

    @Autowired
    private SearchEngineService searchEngineService;

    @Autowired
    private ApiDocumentSearcher apiDocumentSearcher;

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
    public void shouldNotMatchDescriptionTypoWithoutTypoTolerance() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("Fild").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isZero();
    }

    @Test
    public void shouldMatchDescriptionTypoWithTypoToleranceEnabled() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("Fild").setFilters(filters).setTypoTolerance(true).build()
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

        matches = searchEngineService.search(
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
        assertThat(matches.getHits()).isEqualTo(5);
        List<String> results = new ArrayList<>(matches.getDocuments());
        assertThat(results.get(0)).isIn("api-3", "api-4");
        assertThat(results.get(1)).isIn("api-3", "api-4");
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
        ApiEntity api = ApiEntity.builder().id("api-1").lifecycleState(ApiLifecycleState.CREATED).visibility(Visibility.PUBLIC).build();
        searchEngineService.delete(GraviteeContext.getExecutionContext(), api, true);
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

        matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("labels: \"In Review 1\" AND labels: \"In Review 4\"").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(1);

        assertThat(matches.getDocuments()).containsExactly("api-4");

        matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("labels: \"In Review 3\" OR labels: \"In Review 4\"").setFilters(filters).build()
        );
        assertThat(matches.getHits()).isEqualTo(2);
        assertThat(matches.getDocuments()).containsExactly("api-3", "api-4");

        matches = searchEngineService.search(
            GraviteeContext.getExecutionContext(),
            QueryBuilder.create(ApiEntity.class).setQuery("labels: \"in review 3\" OR labels: \"in review 4\"").setFilters(filters).build()
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
            QueryBuilder.create(ApiEntity.class)
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("excludedFiltersCases")
    public void should_exclude_apis_matching_the_excluded_filters(
        String caseName,
        Map<String, Collection<String>> excludedFilters,
        int expectedHits,
        List<String> expectedDocuments
    ) {
        Map<String, Object> filters = new HashMap<>();
        filters.put(FIELD_API_TYPE_VALUE, Arrays.asList("api-1", "api-2"));
        QueryBuilder<ApiEntity> apiEntityQueryBuilder = QueryBuilder.create(ApiEntity.class)
            .setFilters(filters)
            .setExcludedFilters(excludedFilters);
        SearchResult matches = searchEngineService.search(
            new ExecutionContext(GraviteeContext.getCurrentOrganization(), null),
            apiEntityQueryBuilder.build()
        );

        assertThat(matches.getHits()).isEqualTo(expectedHits);
        assertThat(matches.getDocuments()).containsExactlyElementsOf(expectedDocuments);
    }

    private static Stream<Arguments> excludedFiltersCases() {
        Map<String, Collection<String>> bothDefinitionVersions = new HashMap<>();
        bothDefinitionVersions.put(
            FIELD_DEFINITION_VERSION,
            Arrays.asList(DefinitionVersion.V2.getLabel(), DefinitionVersion.V4.getLabel())
        );

        Map<String, Collection<String>> nameAndBothDefinitionVersions = new HashMap<>();
        nameAndBothDefinitionVersions.put(FIELD_NAME, List.of("My Awesome api / 1"));
        nameAndBothDefinitionVersions.put(
            FIELD_DEFINITION_VERSION,
            Arrays.asList(DefinitionVersion.V2.getLabel(), DefinitionVersion.V4.getLabel())
        );

        Map<String, Collection<String>> nameOnly = new HashMap<>();
        nameOnly.put(FIELD_NAME, List.of("My Awesome api / 1"));

        return Stream.of(
            // api-1 has a null graviteeDefinitionVersion, which the null-means-legacy-V2 rule indexes under the
            // 2.0.0 term, so a 2.0.0 exclusion now removes it alongside the explicitly-V2 api-2.
            Arguments.of("excluding_both_definition_versions_removes_every_api", bothDefinitionVersions, 0, List.of()),
            Arguments.of("excluding_a_name_and_both_definition_versions_removes_every_api", nameAndBothDefinitionVersions, 0, List.of()),
            Arguments.of("excluding_only_a_name_leaves_the_non_excluded_api", nameOnly, 1, List.of("api-2"))
        );
    }

    @Test
    public void should_narrow_a_v2_request_to_a_live_indexed_legacy_api_with_no_definition_version() {
        ApiRepository apiRepository = mock(ApiRepository.class);
        when(apiRepository.search(any(), any(), any(), any())).thenAnswer(invocation -> {
            var selectedIds = invocation.getArgument(0, ApiCriteria.class).getIds();
            var rows = selectedIds
                .stream()
                .map(id -> io.gravitee.repository.management.model.Api.builder().id(id).build())
                .toList();
            return new Page<>(rows, 1, rows.size(), rows.size());
        });

        var page = new ApiQueryServiceImpl(apiRepository, apiDocumentSearcher).searchByIntegrationId(
            LEGACY_INTEGRATION_ID,
            List.of(DefinitionVersion.V2),
            null,
            new PageableImpl(1, 10)
        );

        assertThat(page.getContent()).extracting(Api::getId).containsExactly(LEGACY_INTEGRATION_API_ID);
    }

    @BeforeEach
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
            searchEngineService.index(GraviteeContext.getExecutionContext(), aLegacyApiOwnedByAnIntegration(), true, false);
            searchEngineService.commit();
            isIndexed = true;
        }
    }

    private static io.gravitee.rest.api.model.v4.api.ApiEntity aLegacyApiOwnedByAnIntegration() {
        var apiEntity = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        apiEntity.setId(LEGACY_INTEGRATION_API_ID);
        apiEntity.setName("Legacy Integration Api");
        // No definition version: the legacy row shape the null-means-legacy-V2 rule is about.
        apiEntity.setState(Lifecycle.State.STOPPED);
        apiEntity.setVisibility(Visibility.PUBLIC);
        apiEntity.setLifecycleState(ApiLifecycleState.CREATED);
        // Its own environment keeps it out of the hit counts the environment-scoped fixture assertions pin.
        apiEntity.setReferenceId(ENV_LEGACY_INTEGRATION);
        apiEntity.setReferenceType(ReferenceContext.Type.ENVIRONMENT.name());
        apiEntity.setOriginContext(new OriginContext.Integration(LEGACY_INTEGRATION_ID));
        apiEntity.setUpdatedAt(new Date());
        return apiEntity;
    }

    private static ApiEntity createApiEntity(int index, List<String> labels, String envId) {
        String apiName = index == 2 ? "http://localhost/api-" + index : "My Awesome api / " + index;
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId("api-" + index);
        apiEntity.setLifecycleState(ApiLifecycleState.CREATED);
        apiEntity.setVisibility(Visibility.PUBLIC);
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
        public ApiDocumentSearcher apiDocumentSearcher(IndexWriter indexWriter) {
            return new ApiDocumentSearcher(indexWriter);
        }

        @Bean
        public PageDocumentSearcher pageDocumentSearcher(IndexWriter indexWriter) {
            return new PageDocumentSearcher(indexWriter);
        }

        @Bean
        public UserDocumentSearcher userDocumentSearcher(IndexWriter indexWriter) {
            return new UserDocumentSearcher(indexWriter);
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
        public Collection<DocumentSearcher> searchers(
            ApiDocumentSearcher apiDocumentSearcher,
            PageDocumentSearcher pageDocumentSearcher,
            UserDocumentSearcher userDocumentSearcher
        ) {
            return Arrays.asList(apiDocumentSearcher, pageDocumentSearcher, userDocumentSearcher);
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

        @Bean
        public ApiProductIndexerDomainService apiProductIndexerDomainService() {
            return mock(ApiProductIndexerDomainService.class);
        }

        @Bean
        public ApiProductCrudService apiProductCrudService() {
            return mock(ApiProductCrudService.class);
        }
    }

    private static final String[] DESCRIPTIONS = { "Hiking", "Biking", "Running", "", "Field Hockey" };
}
