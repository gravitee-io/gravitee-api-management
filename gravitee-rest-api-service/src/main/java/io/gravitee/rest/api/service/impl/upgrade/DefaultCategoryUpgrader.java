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

import io.gravitee.common.utils.IdGenerator;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.model.Category;
import io.gravitee.rest.api.service.Upgrader;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DefaultCategoryUpgrader implements Upgrader, Ordered {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(DefaultCategoryUpgrader.class);

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ApiRepository apiRepository;

    @Override
    public boolean upgrade() {
        // Initialize default category
        final Set<Category> categories;
        try {
            categories = categoryRepository.findAll();
            Optional<Category> optionalKeyLessCategory = categories
                .stream()
                .filter(v -> v.getKey() == null || v.getKey().isEmpty())
                .findFirst();
            if (optionalKeyLessCategory.isPresent()) {
                logger.info("Update categories to add field key");
                for (final Category category : categories) {
                    category.setKey(IdGenerator.generate(category.getName()));
                    categoryRepository.update(category);
                }
            }
        } catch (TechnicalException e) {
            logger.error("Error while upgrading categories : {}", e);
        }
        return true;
    }

    @Override
    public int getOrder() {
        return 200;
    }
}
