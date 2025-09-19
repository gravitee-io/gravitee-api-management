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

import static io.gravitee.rest.api.service.common.SecurityContextHelper.authenticateAsSystem;
import static java.util.stream.Collectors.toList;

import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.search.Indexer;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.node.api.initializer.Initializer;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.User;
import io.gravitee.repository.management.model.UserStatus;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserRoleEntity;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.UserMetadataService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.UserConverter;
import io.gravitee.rest.api.service.exceptions.PrimaryOwnerNotFoundException;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.mapper.GenericApiMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class SearchIndexInitializer implements Initializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchIndexInitializer.class);

    private final ApiRepository apiRepository;

    private final GenericApiMapper genericApiMapper;

    private final PageService pageService;

    private final UserRepository userRepository;

    private final SearchEngineService searchEngineService;

    private final EnvironmentRepository environmentRepository;

    private final ApiConverter apiConverter;

    private final UserConverter userConverter;

    private final Map<String, String> organizationIdByEnvironmentIdMap = new ConcurrentHashMap<>();
    private final PrimaryOwnerService primaryOwnerService;
    private final ApiIndexerDomainService apiIndexerDomainService;

    private final UserMetadataService userMetadataService;

    @Autowired
    public SearchIndexInitializer(
        @Lazy ApiRepository apiRepository,
        GenericApiMapper genericApiMapper,
        PageService pageService,
        @Lazy UserRepository userRepository,
        SearchEngineService searchEngineService,
        @Lazy EnvironmentRepository environmentRepository,
        ApiConverter apiConverter,
        UserConverter userConverter,
        final PrimaryOwnerService primaryOwnerService,
        ApiIndexerDomainService apiIndexerDomainService,
        UserMetadataService userMetadataService
    ) {
        this.apiRepository = apiRepository;
        this.genericApiMapper = genericApiMapper;
        this.pageService = pageService;
        this.userRepository = userRepository;
        this.searchEngineService = searchEngineService;
        this.environmentRepository = environmentRepository;
        this.apiConverter = apiConverter;
        this.userConverter = userConverter;
        this.primaryOwnerService = primaryOwnerService;
        this.apiIndexerDomainService = apiIndexerDomainService;
        this.userMetadataService = userMetadataService;
    }

    @Override
    public boolean initialize() {
        ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2,
            new ThreadFactory() {
                private final AtomicLong counter = new AtomicLong(0);

                @Override
                public Thread newThread(@NotNull Runnable r) {
                    return new Thread(r, "gio.search-indexer-upgrader-" + counter.getAndIncrement());
                }
            }
        );

        // index APIs
        List<CompletableFuture<?>> futures = new ArrayList<>();
        try {
            futures.addAll(runApisIndexationAsync(executorService));
        } catch (TechnicalException e) {
            LOGGER.error("failed to index APIs", e);
        }

        // index users
        try {
            futures.addAll(runUsersIndexationAsync(executorService));
        } catch (TechnicalException e) {
            LOGGER.error("failed to index users", e);
        }

        CompletableFuture<Void> future = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        future.whenCompleteAsync(
            (unused, throwable) -> {
                executorService.shutdown();
                searchEngineService.commit();
            },
            executorService
        );

        return true;
    }

    protected List<CompletableFuture<?>> runApisIndexationAsync(ExecutorService executorService) throws TechnicalException {
        return apiRepository
            .search(new ApiCriteria.Builder().build(), null, ApiFieldFilter.allFields())
            .map(api -> runApiIndexationAsync(executorService, api))
            .collect(toList());
    }

    private CompletableFuture<?> runApiIndexationAsync(ExecutorService executorService, Api api) {
        authenticateAsAdmin();

        String environmentId = api.getEnvironmentId();
        String organizationId = organizationIdByEnvironmentIdMap.computeIfAbsent(environmentId, envId -> {
            try {
                return environmentRepository.findById(environmentId).get().getOrganizationId();
            } catch (Exception e) {
                LOGGER.error("failed to find organization for environment {}", environmentId, e);
                return null;
            }
        });

        ExecutionContext executionContext = new ExecutionContext(organizationId, environmentId);
        Indexable indexable;
        PrimaryOwnerEntity primaryOwner = null;
        try {
            primaryOwner = primaryOwnerService.getPrimaryOwner(organizationId, api.getId());
        } catch (PrimaryOwnerNotFoundException e) {
            LOGGER.warn("Failed to retrieve API primary owner, API will we indexed without his primary owner", e);
        }
        try {
            // V2 APIs have a null definitionVersion attribute in the Repository
            if (api.getDefinitionVersion() == null) {
                indexable = apiConverter.toApiEntity(executionContext, api, primaryOwner, false);
                return runApiIndexationAsync(executionContext, api, primaryOwner, indexable, executorService);
            }

            indexable = apiIndexerDomainService.toIndexableApi(
                new Indexer.IndexationContext(organizationId, environmentId),
                ApiAdapter.INSTANCE.toCoreModel(api)
            );
            return runApiIndexationAsync(executionContext, api, primaryOwner, indexable, executorService);
        } catch (Exception e) {
            LOGGER.error("Failed to convert API {} to indexable", api.getId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<?> runApiIndexationAsync(
        ExecutionContext executionContext,
        Api api,
        PrimaryOwnerEntity primaryOwnerEntity,
        Indexable indexable,
        ExecutorService executorService
    ) {
        return CompletableFuture.runAsync(
            () -> {
                try {
                    // API
                    searchEngineService.index(executionContext, indexable, true, false);

                    // Pages
                    GenericApiEntity genericApiEntity = genericApiMapper.toGenericApi(api, primaryOwnerEntity);
                    List<PageEntity> apiPages = pageService.search(
                        executionContext.getEnvironmentId(),
                        new PageQuery.Builder().api(api.getId()).published(true).build(),
                        true
                    );
                    apiPages.forEach(page -> {
                        try {
                            if (
                                !PageType.FOLDER.name().equals(page.getType()) &&
                                !PageType.ROOT.name().equals(page.getType()) &&
                                !PageType.SYSTEM_FOLDER.name().equals(page.getType()) &&
                                !PageType.LINK.name().equals(page.getType())
                            ) {
                                pageService.transformSwagger(executionContext, page, genericApiEntity);
                                searchEngineService.index(executionContext, page, true, false);
                            }
                        } catch (Exception ignored) {
                            log.debug("Exception ignored in SearchIndexInitializer");
                        }
                    });
                } finally {
                    GraviteeContext.cleanContext();
                }
            },
            executorService
        );
    }

    protected List<CompletableFuture<?>> runUsersIndexationAsync(ExecutorService executorService) throws TechnicalException {
        return userRepository
            .search(
                new UserCriteria.Builder().statuses(UserStatus.ACTIVE).build(),
                new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
            )
            .getContent()
            .stream()
            .map(user -> runUserIndexationAsync(executorService, user))
            .collect(toList());
    }

    private CompletableFuture<?> runUserIndexationAsync(ExecutorService executorService, User user) {
        return CompletableFuture.runAsync(
            () -> {
                ExecutionContext executionContext = new ExecutionContext(user.getOrganizationId(), null);
                searchEngineService.index(
                    executionContext,
                    userConverter.toUserEntity(user, userMetadataService.findAllByUserId(user.getId())),
                    true,
                    false
                );
            },
            executorService
        );
    }

    @Override
    public int getOrder() {
        return InitializerOrder.SEARCH_INDEX_INITIALIZER;
    }

    private void authenticateAsAdmin() {
        UserRoleEntity adminRole = new UserRoleEntity();
        adminRole.setScope(RoleScope.ORGANIZATION);
        adminRole.setName(SystemRole.ADMIN.name());
        authenticateAsSystem("SearchIndexUpgrader", Set.of(adminRole));
    }
}
