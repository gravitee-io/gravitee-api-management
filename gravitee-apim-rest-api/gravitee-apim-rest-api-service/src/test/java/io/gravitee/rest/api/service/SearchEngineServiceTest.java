/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service;

import static io.gravitee.rest.api.service.impl.search.lucene.searcher.ApiDocumentSearcher.FIELD_API_TYPE_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.rest.api.model.ApiPageEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.impl.search.SearchEngineServiceImpl;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.impl.search.configuration.SearchEngineConfiguration;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentSearcher;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer;
import io.gravitee.rest.api.service.impl.search.lucene.searcher.ApiDocumentSearcher;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.PageDocumentTransformer;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
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

    @Autowired
    private SearchEngineService searchEngineService;

    private static boolean isIndexed = false;

    @Test
    public void shouldFindBestResultsWithApiName() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("My api 1").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(7, matches.getHits());
        assertEquals(Arrays.asList("api-1", "api-2", "api-3", "api-4", "api-0"), matches.getDocuments());
    }

    @Test
    public void shouldFindBestResultsWithDescription() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("field").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(1, matches.getHits());
        assertEquals(Arrays.asList("api-4"), matches.getDocuments());
    }

    @Test
    public void shouldFindBestResultsWithCategory() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("machine learning").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(3, matches.getHits(), 3);
        assertEquals(Arrays.asList("api-0", "api-2", "api-4"), matches.getDocuments());
    }

    @Test
    public void shouldNotFoundWithOwnerEmail() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("foobar-3@gravitee.io").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(0, matches.getHits());

        matches = searchEngineService.search(QueryBuilder.create(ApiEntity.class).setQuery("*@*").setFilters(filters).build());
        assertNotNull(matches);
        assertEquals(0, matches.getHits());
    }

    @Test
    public void shouldFindBestResultsWithWildcard() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("My api *").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(7, matches.getHits());
        assertEquals(Arrays.asList("api-0", "api-1", "api-2", "api-3", "api-4"), matches.getDocuments());
    }

    @Test
    public void shouldFindBestResultsWithOwnerName() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("Owner 3").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(5, matches.getHits());
        assertEquals(Arrays.asList("api-3", "api-4", "api-0", "api-1", "api-2"), matches.getDocuments());
    }

    @Test
    public void shouldFindWithExplicitNameFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("name:\"My api 1\"").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(1, matches.getHits(), 1);
        assertEquals(Arrays.asList("api-1"), matches.getDocuments());
    }

    @Test
    public void shouldFindWithExplicitNameUnSensitiveFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("name:\"my api 1\"").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(1, matches.getHits());
        assertEquals(Arrays.asList("api-1"), matches.getDocuments());
    }

    @Test
    public void shouldNotFindWithExplicitWrongNameFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("name:\"My api not found\"").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(0, matches.getHits());
    }

    @Test
    public void shouldFindWithExplicitDescriptionFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("description:\"Field Hockey\"").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(1, matches.getHits());
        assertEquals(Arrays.asList("api-4"), matches.getDocuments());
    }

    @Test
    public void shouldFindWithLabelsFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("labels: \"In Review 1\"").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(4, matches.getHits());
        assertEquals(Arrays.asList("api-1", "api-2", "api-3", "api-4"), matches.getDocuments());

        matches =
            searchEngineService.search(
                QueryBuilder
                    .create(ApiEntity.class)
                    .setQuery("labels: \"In Review 1\" AND labels: \"In Review 4\"")
                    .setFilters(filters)
                    .build()
            );
        assertNotNull(matches);
        assertEquals(1, matches.getHits());
        assertEquals(Arrays.asList("api-4"), matches.getDocuments());

        matches =
            searchEngineService.search(
                QueryBuilder
                    .create(ApiEntity.class)
                    .setQuery("labels: \"In Review 3\" OR labels: \"In Review 4\"")
                    .setFilters(filters)
                    .build()
            );
        assertNotNull(matches);
        assertEquals(2, matches.getHits());
        assertEquals(Arrays.asList("api-3", "api-4"), matches.getDocuments());

        matches =
            searchEngineService.search(
                QueryBuilder
                    .create(ApiEntity.class)
                    .setQuery("labels: \"in review 3\" OR labels: \"in review 4\"")
                    .setFilters(filters)
                    .build()
            );
        assertNotNull(matches);
        assertEquals(2, matches.getHits());
        assertEquals(Arrays.asList("api-3", "api-4"), matches.getDocuments());
    }

    @Test
    public void shouldFindWithLabelsAndPhraseFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("labels: \"in review 4\" foobar-3@gravitee.io").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(1, matches.getHits(), 1);
        assertEquals(Arrays.asList("api-4"), matches.getDocuments());
    }

    @Test
    public void shouldFindWithCategoriesFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("categories:\"Machine Learning\"").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(3, matches.getHits(), 3);
        assertEquals(Arrays.asList("api-0", "api-2", "api-4"), matches.getDocuments());
    }

    @Test
    public void shouldFindWithCategoriesFilterAndText() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("categories: Sports AND Hiking").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(1, matches.getHits(), 1);
        assertEquals(Arrays.asList("api-0"), matches.getDocuments());
    }

    @Test
    public void shouldFindWithCategoriesWildcardFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("categories: *").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(3, matches.getHits());
        assertEquals(Arrays.asList("api-0", "api-2", "api-4"), matches.getDocuments());
    }

    @Test
    public void shouldFindWithNameAndOwnerFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("name:\"My api 2\" AND ownerName: \"Owner 2\"").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(1, matches.getHits());
        assertEquals(Arrays.asList("api-2"), matches.getDocuments());
    }

    @Test
    public void shouldNotFindWithNameAndOwnerFilterIfApiIsExcluded() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("api", Collections.singleton("api-1"));
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("name:\"My api 2\" AND ownerName: \"Owner 2\"").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(0, matches.getHits());
    }

    @Test
    public void shouldNotFoundWithNameAndWrongOwnerFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("name:\"My api 1\" AND ownerName: \"Owner 2\"").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(0, matches.getHits());
    }

    @Test
    public void shouldFindText() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("Hiking").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(1, matches.getHits());
        assertEquals(Arrays.asList("api-0"), matches.getDocuments());
    }

    @Test
    public void shouldFindWithPageContent() {
        Map<String, Object> filters = new HashMap<>();
        filters.put(FIELD_API_TYPE_VALUE, Arrays.asList("api-1", "api-2"));
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("documentation").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(1, matches.getHits());
        assertEquals(Arrays.asList("api-1"), matches.getDocuments());
    }

    @Test
    public void shouldFindAll() {
        SearchResult matches = searchEngineService.search(QueryBuilder.create(ApiEntity.class).build());
        assertNotNull(matches);
        assertEquals(7, matches.getHits(), 7);
        assertEquals(Arrays.asList("api-1", "api-3", "api-0", "api-2", "api-4"), matches.getDocuments());
    }

    @Test
    public void shouldFindWithContextPath() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("/path/api-2").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(1, matches.getHits());
        assertEquals(Arrays.asList("api-2"), matches.getDocuments());
    }

    @Test
    public void shouldFindByContextPath() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).addExplicitFilter("paths", "/path/api-2").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(1, matches.getHits());
        assertEquals(Arrays.asList("api-2"), matches.getDocuments());
    }

    @Test
    public void shouldFindByContextPathWildcard() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).addExplicitFilter("paths", "*th/api-2").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(1, matches.getHits());
        assertEquals(Arrays.asList("api-2"), matches.getDocuments());
    }

    @Test
    public void shouldFindWithTag() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("tag-api").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(5, matches.getHits());
        assertEquals(Arrays.asList("api-0", "api-1", "api-2", "api-3", "api-4"), matches.getDocuments());
    }

    @Test
    public void shouldFindByTag() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).addExplicitFilter("tags", "tag-api-3").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(1, matches.getHits());
        assertEquals(Arrays.asList("api-3"), matches.getDocuments());
    }

    @Before
    public void initIndexer() {
        // TODO: Remove this hack and use @BeforeAll when move to junit 5.x
        if (!isIndexed) {
            List<String> labels = new ArrayList();
            for (int i = 0; i < 5; i++) {
                String apiName = "My api " + i;
                ApiEntity apiEntity = new ApiEntity();
                apiEntity.setId("api-" + i);
                labels.add("In Review " + i);
                apiEntity.setReferenceId(GraviteeContext.getCurrentEnvironmentOrDefault());
                apiEntity.setReferenceType(GraviteeContext.ReferenceContextType.ENVIRONMENT.name());
                apiEntity.setName(apiName);
                apiEntity.setUpdatedAt(new Date());
                apiEntity.setLabels(labels);
                apiEntity.setDescription(DESCRIPTIONS[i]);

                Proxy proxy = new Proxy();
                List<VirtualHost> hosts = new ArrayList<>();
                VirtualHost host = new VirtualHost();
                host.setPath("/path/" + apiEntity.getId());
                hosts.add(host);
                proxy.setVirtualHosts(hosts);
                apiEntity.setProxy(proxy);
                PrimaryOwnerEntity owner = new PrimaryOwnerEntity();
                owner.setId("user-" + i);
                owner.setDisplayName("Owner " + i);
                owner.setEmail("foobar-" + i + "@gravitee.io");
                apiEntity.setPrimaryOwner(owner);
                if (i % 2 == 0) {
                    // Actually we index hrid categories...
                    apiEntity.setCategories(Set.of("sports", "game", "machine-learning"));
                }
                apiEntity.setTags(Set.of("tag-" + apiEntity.getId()));
                searchEngineService.index(apiEntity, false);
            }
            searchEngineService.index(completePage(new ApiPageEntity(), 1, true), false);
            searchEngineService.index(completePage(new PageEntity(), 2, true), false);
            searchEngineService.index(completePage(new ApiPageEntity(), 3, false), false);

            isIndexed = true;
        }
    }

    public PageEntity completePage(PageEntity pageEntity, int i, boolean published) {
        pageEntity.setId("page-" + i);
        pageEntity.setName("Gravitee documentation");
        pageEntity.setContent("documentation");
        if (pageEntity instanceof ApiPageEntity) {
            pageEntity.setReferenceType("API");
            pageEntity.setReferenceId("api-" + i);
        } else {
            pageEntity.setReferenceId(GraviteeContext.getCurrentEnvironmentOrDefault());
            pageEntity.setReferenceType(GraviteeContext.ReferenceContextType.ENVIRONMENT.name());
        }

        pageEntity.setVisibility(Visibility.PUBLIC);
        pageEntity.setPublished(published);
        return pageEntity;
    }

    @Configuration
    @Import(SearchEngineConfiguration.class) // the actual configuration
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
        public SearchEngineService searchEngineService() {
            return new SearchEngineServiceImpl();
        }

        @Bean
        public Collection<DocumentTransformer> transformers() {
            return Arrays.asList(new ApiDocumentTransformer(), new PageDocumentTransformer());
        }

        @Bean
        public Collection<DocumentSearcher> searchers() {
            return Arrays.asList(new ApiDocumentSearcher());
        }

        @Bean
        public CommandService commandService() {
            return mock(CommandService.class);
        }
    }

    private static final String[] DESCRIPTIONS = { "Hiking", "Biking", "Running", "", "Field Hockey" };
}
