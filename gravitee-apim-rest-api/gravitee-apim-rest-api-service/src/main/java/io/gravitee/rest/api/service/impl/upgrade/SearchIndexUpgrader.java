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
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.management.model.UserStatus;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.Upgrader;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
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

    @Autowired
    private ApiService apiService;

    @Autowired
    private PageService pageService;

    @Autowired
    private UserService userService;

    @Autowired
    private SearchEngineService searchEngineService;

    @Override
    public boolean upgrade() {
        // Index APIs
        final Set<ApiEntity> apis = apiService.findAllLight();

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
        apis
            .stream()
            .forEach(
                new Consumer<ApiEntity>() {
                    @Override
                    public void accept(ApiEntity apiEntity) {
                        futures.add(
                            CompletableFuture.runAsync(
                                () -> {
                                    // API
                                    searchEngineService.index(apiEntity, true, false);

                                    // Pages
                                    List<PageEntity> apiPages = pageService.search(
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
                                                    pageService.transformSwagger(page, apiEntity.getId());
                                                    searchEngineService.index(page, true, false);
                                                }
                                            } catch (Exception ignored) {}
                                        }
                                    );
                                },
                                executorService
                            )
                        );
                    }
                }
            );

        futures.add(
            CompletableFuture.runAsync(
                () -> {
                    // Index users
                    Page<UserEntity> users = userService.search(
                        new UserCriteria.Builder().statuses(UserStatus.ACTIVE).build(),
                        new PageableImpl(1, Integer.MAX_VALUE)
                    );

                    users.getContent().forEach(userEntity -> searchEngineService.index(userEntity, true, false));
                }
            )
        );

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

    @Override
    public int getOrder() {
        return 250;
    }
}
