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
package io.gravitee.rest.api.service.impl.upgrade;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.management.model.UserStatus;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.search.SearchEngineService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class SearchIndexUpgrader implements Upgrader, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchIndexUpgrader.class);

    @Autowired
    private ApiService apiService;

    @Autowired
    private PageService pageService;

    @Autowired
    private UserService userService;

    @Autowired
    private SearchEngineService searchEngineService;

    @Autowired
    private EnvironmentRepository environmentRepository;

    private final Map<String, String> organizationIdByEnvironmentIdMap = new ConcurrentHashMap<>();

    @Override
    public boolean upgrade(ExecutionContext executionContext) {
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

        List<CompletableFuture<?>> futures = new ArrayList<>();

        // index APIs
        apiService
            .findAllLight(executionContext)
            .stream()
            .forEach(
                apiEntity -> {
                    String environmentId = apiEntity.getReferenceId();
                    String organizationId = organizationIdByEnvironmentIdMap.computeIfAbsent(
                        environmentId,
                        envId -> {
                            try {
                                return environmentRepository.findById(environmentId).get().getOrganizationId();
                            } catch (Exception e) {
                                LOGGER.error("failed to find organization for environment {}", environmentId, e);
                                return null;
                            }
                        }
                    );

                    futures.add(runApiIndexationAsync(executionContext, apiEntity, environmentId, organizationId, executorService));
                }
            );

        // index users
        futures.add(runUsersIndexationAsync(executionContext));

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

    private CompletableFuture runApiIndexationAsync(
        ExecutionContext executionContext,
        ApiEntity apiEntity,
        String environmentId,
        String organizationId,
        ExecutorService executorService
    ) {
        return CompletableFuture.runAsync(
            () -> {
                try {
                    // API
                    searchEngineService.index(executionContext, apiEntity, true, false);

                    // Pages
                    List<PageEntity> apiPages = pageService.search(
                        environmentId,
                        new PageQuery.Builder().api(apiEntity.getId()).published(true).build(),
                        true
                    );
                    apiPages.forEach(
                        page -> {
                            try {
                                if (
                                    !PageType.FOLDER.name().equals(page.getType()) &&
                                    !PageType.ROOT.name().equals(page.getType()) &&
                                    !PageType.SYSTEM_FOLDER.name().equals(page.getType()) &&
                                    !PageType.LINK.name().equals(page.getType())
                                ) {
                                    pageService.transformSwagger(
                                        new ExecutionContext(environmentId, organizationId),
                                        page,
                                        apiEntity.getId()
                                    );
                                    searchEngineService.index(executionContext, page, true, false);
                                }
                            } catch (Exception ignored) {}
                        }
                    );
                } finally {
                    GraviteeContext.cleanContext();
                }
            },
            executorService
        );
    }

    private CompletableFuture runUsersIndexationAsync(ExecutionContext executionContext) {
        return CompletableFuture.runAsync(
            () -> {
                // Index users
                Page<UserEntity> users = userService.search(
                    executionContext,
                    new UserCriteria.Builder().statuses(UserStatus.ACTIVE).build(),
                    new PageableImpl(1, Integer.MAX_VALUE)
                );

                users.getContent().forEach(userEntity -> searchEngineService.index(executionContext, userEntity, true, false));
            }
        );
    }

    @Override
    public int getOrder() {
        return 250;
    }
}
