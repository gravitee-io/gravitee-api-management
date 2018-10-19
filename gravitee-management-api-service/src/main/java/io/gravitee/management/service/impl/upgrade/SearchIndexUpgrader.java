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
package io.gravitee.management.service.impl.upgrade;

import io.gravitee.common.data.domain.Page;
import io.gravitee.management.model.PageEntity;
import io.gravitee.management.model.PageListItem;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.common.PageableImpl;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.PageService;
import io.gravitee.management.service.Upgrader;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.search.SearchEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
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
        Set<ApiEntity> apis = apiService.findAll();
        apis.stream()
                .forEach(new Consumer<ApiEntity>() {
                    @Override
                    public void accept(ApiEntity apiEntity) {
                        searchEngineService.index(apiEntity);

                        List<PageListItem> apiPages = pageService.findApiPagesByApi(apiEntity.getId());
                        apiPages.stream().forEach(new Consumer<PageListItem>() {
                            @Override
                            public void accept(PageListItem pageListItem) {
                                try {
                                    PageEntity page = pageService.findById(pageListItem.getId(), true);
                                    searchEngineService.index(page);
                                } catch (Exception ex) {

                                }
                            }
                        });
                    }
                });

        // Index users
        Page<UserEntity> users = userService.search(null, new PageableImpl(1, Integer.MAX_VALUE));
        users.getContent().stream()
                .forEach(new Consumer<UserEntity>() {
                    @Override
                    public void accept(UserEntity userEntity) {
                        searchEngineService.index(userEntity);
                    }
                });

        return true;
    }

    @Override
    public int getOrder() {
        return 250;
    }
}
