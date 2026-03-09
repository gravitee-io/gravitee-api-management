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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.repository.ApiFixtures;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api_product.domain_service.ApiProductIndexerDomainService;
import io.gravitee.apim.core.search.Indexer;
import io.gravitee.apim.core.search.model.IndexableApi;
import io.gravitee.apim.core.search.model.IndexableApiProduct;
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
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.UserMetadataService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.UserConverter;
import io.gravitee.rest.api.service.exceptions.PrimaryOwnerNotFoundException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
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
        initializer = new SearchIndexInitializer(
            apiRepository,
            new GenericApiMapper(apiMapper, apiConverter),
            pageService,
            userRepository,
            searchEngineService,
            environmentRepository,
            apiConverter,
            new UserConverter(),
            primaryOwnerService,
            apiIndexerDomainService,
            apiProductIndexerDomainService,
            apiProductsRepository,
            userMetadataService
        );

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
