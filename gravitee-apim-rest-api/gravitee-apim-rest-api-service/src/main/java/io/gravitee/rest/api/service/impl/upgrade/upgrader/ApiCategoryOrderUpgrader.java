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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiCategoryOrderRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.ApiCategoryOrder;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ApiCategoryOrderUpgrader implements Upgrader {

    private final ApiRepository apiRepository;

    private final CategoryRepository categoryRepository;

    private final ApiCategoryOrderRepository apiCategoryOrderRepository;

    @Autowired
    public ApiCategoryOrderUpgrader(
        @Lazy ApiRepository apiRepository,
        @Lazy CategoryRepository categoryRepository,
        @Lazy ApiCategoryOrderRepository apiCategoryOrderRepository
    ) {
        this.apiRepository = apiRepository;
        this.categoryRepository = categoryRepository;
        this.apiCategoryOrderRepository = apiCategoryOrderRepository;
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.API_CATEGORY_ORDER_UPGRADER;
    }

    @Override
    public boolean upgrade() {
        try {
            fillApiCategoryOrderTable();
        } catch (Exception e) {
            log.error("Error applying upgrader", e);
            return false;
        }

        return true;
    }

    private void fillApiCategoryOrderTable() throws TechnicalException {
        this.categoryRepository.findAll().forEach(category -> {
            var order = new AtomicInteger(0);
            this.apiRepository.search(new ApiCriteria.Builder().category(category.getId()).build(), ApiFieldFilter.defaultFields()).forEach(
                api -> {
                    try {
                        this.apiCategoryOrderRepository.create(
                            ApiCategoryOrder.builder()
                                .apiId(api.getId())
                                .categoryId(category.getId())
                                .order(order.getAndIncrement())
                                .build()
                        );
                    } catch (TechnicalException e) {
                        log.error("Unable to create api category order for API [{}] and Category [{}]", api.getId(), category.getId(), e);
                        throw new RuntimeException(e);
                    }
                }
            );
        });
    }
}
