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

import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.impl.search.SearchEngineServiceImpl;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.impl.search.configuration.SearchEngineConfiguration;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentSearcher;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer;
import io.gravitee.rest.api.service.impl.search.lucene.searcher.ApiDocumentSearcher;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

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
        assertEquals(matches.getHits(), 5);
        assertEquals(matches.getDocuments(), Arrays.asList("api-1", "api-2", "api-3", "api-4", "api-0"));
    }

    @Test
    public void shouldFindBestResultsWithWildcard() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("My api *").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(matches.getHits(), 5);
        assertEquals(matches.getDocuments(), Arrays.asList("api-0", "api-1", "api-2", "api-3", "api-4"));
    }

    @Test
    public void shouldFindBestResultsWithOwnerName() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("Owner 1").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(matches.getHits(), 5);
        assertEquals(matches.getDocuments(), Arrays.asList("api-3", "api-4", "api-0", "api-1", "api-2"));
    }

    @Test
    public void shouldFindWithExplicitNameFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("name:\"My api 1\"").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(matches.getHits(), 1);
        assertEquals(matches.getDocuments(), Arrays.asList("api-1"));
    }

    @Test
    public void shouldNotFindWithExplicitWrongNameFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("name:\"My api not found\"").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(matches.getHits(), 0);
    }

    @Test
    public void shouldFindWithLabelsFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("labels: \"Label 1\"").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(matches.getHits(), 4);
        assertEquals(matches.getDocuments(), Arrays.asList("api-1", "api-2", "api-3", "api-4"));

        matches =
            searchEngineService.search(
                QueryBuilder.create(ApiEntity.class).setQuery("labels: \"Label 1\" AND labels: \"Label 4\"").setFilters(filters).build()
            );
        assertNotNull(matches);
        assertEquals(matches.getHits(), 1);
        assertEquals(matches.getDocuments(), Arrays.asList("api-4"));

        matches =
            searchEngineService.search(
                QueryBuilder.create(ApiEntity.class).setQuery("labels: \"Label 3\" OR labels: \"Label 4\"").setFilters(filters).build()
            );
        assertNotNull(matches);
        assertEquals(matches.getHits(), 2);
        assertEquals(matches.getDocuments(), Arrays.asList("api-3", "api-4"));
    }

    @Test
    public void shouldFindWithLabelsAndPhraseFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("labels: \"Label 4\" foobar-3@gravitee.io").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(matches.getHits(), 1);
        assertEquals(matches.getDocuments(), Arrays.asList("api-4"));
    }

    @Test
    public void shouldFindWithCategoriesFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("categories: Sports").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(matches.getHits(), 3);
        assertEquals(matches.getDocuments(), Arrays.asList("api-0", "api-2", "api-4"));
    }

    @Test
    public void shouldFindWithCategoriesFilterAndText() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("categories: Sports AND Hiking").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(matches.getHits(), 1);
        assertEquals(matches.getDocuments(), Arrays.asList("api-0"));
    }

    @Test
    public void shouldFindWithCategoriesWildcardFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("categories: *").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(matches.getHits(), 3);
        assertEquals(matches.getDocuments(), Arrays.asList("api-0", "api-2", "api-4"));
    }

    @Test
    public void shouldFindWithNameAndOwnerFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("name:\"My api 2\" AND ownerName: \"Owner 2\"").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(matches.getHits(), 1);
        String[] ids = matches.getDocuments().toArray(new String[0]);
        assertEquals(ids[0], "api-2");
    }

    @Test
    public void shouldNotFoundWithNameAndWrongOwnerFilter() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("name:\"My api 1\" AND ownerName: \"Owner 2\"").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(matches.getHits(), 0);
    }

    @Test
    public void shouldFindText() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("Hiking").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(matches.getHits(), 1);
        assertEquals(matches.getDocuments(), Arrays.asList("api-0"));
    }

    @Test
    @Ignore
    // FIXME: should work ?
    public void shouldFindWithPageContent() {
        Map<String, Object> filters = new HashMap<>();
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("documentation").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(matches.getHits(), 1);
        assertEquals(matches.getDocuments(), Arrays.asList("api-1"));
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
            return Arrays.asList(new ApiDocumentTransformer());
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

    @Before
    public void initIndexer() {
        // TODO: Remove this hack and use @BeforeAll when move to junit 5.x
        if (!isIndexed) {
            List<String> labels = new ArrayList();
            for (int i = 0; i < 5; i++) {
                ApiEntity apiEntity = new ApiEntity();
                apiEntity.setId("api-" + i);
                labels.add("Label " + i);
                apiEntity.setReferenceId(GraviteeContext.getCurrentEnvironmentOrDefault());
                apiEntity.setReferenceType(GraviteeContext.ReferenceContextType.ENVIRONMENT.name());
                apiEntity.setName("My api " + i);
                apiEntity.setUpdatedAt(new Date());
                apiEntity.setLabels(labels);
                PrimaryOwnerEntity owner = new PrimaryOwnerEntity();
                owner.setId("user-" + i);
                owner.setDisplayName("Owner " + i);
                owner.setEmail("foobar-" + i + "@gravitee.io");
                apiEntity.setPrimaryOwner(owner);
                if (i % 2 == 0) {
                    apiEntity.setCategories(Set.of("Sports", "Hiking"));
                }
                searchEngineService.index(apiEntity, false);
            }
            isIndexed = true;
        }
    }
}
