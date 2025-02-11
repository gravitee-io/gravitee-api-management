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
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.noop.AbstractNoOpRepositoryTest;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpApiKeyRepositoryTest extends AbstractNoOpRepositoryTest {

    @Autowired
    private NoOpApiKeyRepository cut;

    @Test
    public void findById() throws TechnicalException {
        assertNotNull(cut);

        Optional<ApiKey> apiKey = cut.findById("test_id");

        assertTrue(apiKey.isEmpty());
    }

    @Test
    public void findByKey() throws TechnicalException {
        List<ApiKey> apiKeys = cut.findByKey("test_key");

        assertNotNull(apiKeys);
        assertEquals(0, apiKeys.size());
    }

    @Test
    public void findByKeyAndEnvironmentId() throws TechnicalException {
        List<ApiKey> apiKeys = cut.findByKeyAndEnvironmentId("test_key", "environment_id");

        assertNotNull(apiKeys);
        assertEquals(0, apiKeys.size());
    }

    @Test
    public void findByKeyAndApi() throws TechnicalException {
        Optional<ApiKey> apiKey = cut.findByKeyAndApi("test_id", "test_key");
        assertTrue(apiKey.isEmpty());
    }

    @Test
    public void create() throws TechnicalException {
        ApiKey apiKey = cut.create(new ApiKey());
        assertNull(apiKey);
    }

    @Test
    public void update() throws TechnicalException {
        ApiKey apiKey = cut.update(new ApiKey());
        assertNull(apiKey);
    }

    @Test
    public void findBySubscription() throws TechnicalException {
        Set<ApiKey> subscription = cut.findBySubscription("test_subscription");

        assertNotNull(subscription);
        assertTrue(subscription.isEmpty());
    }

    @Test
    public void findByApplication() throws TechnicalException {
        List<ApiKey> applications = cut.findByApplication("test_application");

        assertNotNull(applications);
        assertTrue(applications.isEmpty());
    }

    @Test
    public void findByPlan() throws TechnicalException {
        Set<ApiKey> plans = cut.findByPlan("test+plan");

        assertNotNull(plans);
        assertTrue(plans.isEmpty());
    }

    @Test
    public void findByCriteria() throws TechnicalException {
        List<ApiKey> apiKeys = cut.findByCriteria(ApiKeyCriteria.builder().build());

        assertNotNull(apiKeys);
        assertTrue(apiKeys.isEmpty());
    }

    @Test
    public void findAll() throws TechnicalException {
        Set<ApiKey> apiKeys = cut.findAll();

        assertNotNull(apiKeys);
        assertTrue(apiKeys.isEmpty());
    }
}
