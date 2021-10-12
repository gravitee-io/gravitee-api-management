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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import io.gravitee.rest.api.model.ApiPageEntity;
import io.gravitee.rest.api.model.PageEntity;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
@ContextConfiguration(classes = { SearchEngineServiceSwaggerTest.TestConfig.class }, loader = AnnotationConfigContextLoader.class)
public class SearchEngineServiceSwaggerTest {

    @Autowired
    private SearchEngineService searchEngineService;

    private static boolean isIndexed = false;

    @Test
    public void shouldFindBestResultsWithApiName() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("api", Arrays.asList("api-1", "api-3", "api-5", "api-6"));
        SearchResult matches = searchEngineService.search(
            QueryBuilder.create(ApiEntity.class).setQuery("swagger").setFilters(filters).build()
        );
        assertNotNull(matches);
        assertEquals(matches.getDocuments().size(), 4);
        assertEquals(matches.getDocuments(), Arrays.asList("api-5", "api-1", "api-3", "api-6"));
    }

    @Before
    public void initIndexer() {
        // TODO: Remove this hack and use @BeforeAll when move to junit 5.x
        if (!isIndexed) {
            for (int i = 0; i < 10; i++) {
                String apiId = "api-" + i;
                String apiName;
                if (i == 5) {
                    apiName = "Swagger Api";
                } else {
                    apiName = "Api " + apiId;
                }
                ApiEntity apiEntity = new ApiEntity();
                apiEntity.setId(apiId);
                apiEntity.setReferenceId(GraviteeContext.getCurrentEnvironmentOrDefault());
                apiEntity.setReferenceType(GraviteeContext.ReferenceContextType.ENVIRONMENT.name());
                apiEntity.setName(apiName);

                apiEntity.setDescription(
                    "This is a sample server Petstore server." +
                    "You can find out more about Swagger at [http://swagger.io](http://swagger.io) " +
                    "or on [irc.freenode.net, #swagger](http://swagger.io/irc/).  For this sample, you " +
                    "can use the api key `special-key` to test the authorization filters."
                );
                searchEngineService.index(apiEntity, false);
                searchEngineService.index(completePage(new ApiPageEntity(), apiId, false), false);
            }
            isIndexed = true;
        }
    }

    public PageEntity completePage(PageEntity pageEntity, String apiId, boolean published) {
        pageEntity.setId("page");
        pageEntity.setName("Start of petstore");
        pageEntity.setContent(
            "This is a sample server Petstore server. You can find out more about Swagger at " +
            "http://swagger.io or on irc.freenode.net, #swagger. For this sample, you can use the api key " +
            "special-key to test the authorization filters."
        );
        if (pageEntity instanceof ApiPageEntity) {
            pageEntity.setReferenceType("API");
            pageEntity.setReferenceId(apiId);
        } else {
            pageEntity.setReferenceId(GraviteeContext.getCurrentEnvironmentOrDefault());
            pageEntity.setReferenceType(GraviteeContext.ReferenceContextType.ENVIRONMENT.name());
        }
        pageEntity.setType("swagger");
        pageEntity.setVisibility(Visibility.PUBLIC);
        pageEntity.setPublished(published);
        return pageEntity;
    }

    @Configuration
    @Import(SearchEngineConfiguration.class) // the actual configuration
    public static class TestConfig {

        @Bean
        public Directory indexDirectory() throws IOException {
            Path path = Path.of("target/" + SearchEngineServiceSwaggerTest.class.getCanonicalName());
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

    private static final String[] NAMES = { "Api", "Swagger" };
}
