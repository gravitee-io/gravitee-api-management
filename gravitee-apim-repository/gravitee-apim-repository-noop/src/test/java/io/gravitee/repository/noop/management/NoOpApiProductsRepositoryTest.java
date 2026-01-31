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
package io.gravitee.repository.noop.management;

import static org.junit.Assert.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.apiproducts.ApiProductsRepository;
import io.gravitee.repository.management.model.ApiProduct;
import io.gravitee.repository.noop.AbstractNoOpRepositoryTest;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class NoOpApiProductsRepositoryTest extends AbstractNoOpRepositoryTest {

    @Autowired
    private ApiProductsRepository cut;

    @Test
    public void findByEnvironmentIdAndName() throws TechnicalException {
        Optional<ApiProduct> apiProduct = cut.findByEnvironmentIdAndName("DEFAULT", "test-product");

        assertNotNull(apiProduct);
        assertFalse(apiProduct.isPresent());
    }

    @Test
    public void findByEnvironmentId() throws TechnicalException {
        Set<ApiProduct> apiProducts = cut.findByEnvironmentId("DEFAULT");

        assertNotNull(apiProducts);
        assertTrue(apiProducts.isEmpty());
    }

    @Test
    public void findByApiId() throws TechnicalException {
        Set<ApiProduct> apiProducts = cut.findByApiId("test-api-id");

        assertNotNull(apiProducts);
        assertTrue(apiProducts.isEmpty());
    }
}
