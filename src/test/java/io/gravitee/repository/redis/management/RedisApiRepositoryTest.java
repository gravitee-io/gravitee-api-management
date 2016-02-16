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
package io.gravitee.repository.redis.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.MembershipType;
import io.gravitee.repository.management.model.Visibility;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class RedisApiRepositoryTest extends AbstractRedisTest {

    @Autowired
    private ApiRepository apiRepository;

    @Test
    public void shouldCreateApi() throws TechnicalException {
        Api apiToCreate = api();

        apiRepository.create(apiToCreate);
    }

    @Test
    public void shouldFindApi() throws TechnicalException {
        Api apiToCreate = api();

        apiRepository.create(apiToCreate);

        Optional<Api> optApi = apiRepository.findById(apiToCreate.getId());
        Assert.assertTrue(optApi.isPresent());
        Assert.assertEquals(apiToCreate, optApi.get());
    }

    @Test
    public void shouldUpdateApi() throws TechnicalException {
        Api apiToCreate = api();

        apiRepository.create(apiToCreate);

        apiToCreate.setName("My API 2");
        apiRepository.update(apiToCreate);

        Optional<Api> optApi = apiRepository.findById(apiToCreate.getId());
        Assert.assertEquals(apiToCreate.getName(), optApi.get().getName());
    }

    @Test
    public void shouldDeleteApi() throws TechnicalException {
        Api apiToCreate = api();

        apiRepository.create(apiToCreate);

        apiRepository.delete(apiToCreate.getId());

        Optional<Api> optApi = apiRepository.findById(apiToCreate.getId());
        Assert.assertFalse(optApi.isPresent());
    }

    @Test
    public void shouldFindAll() throws TechnicalException {
        apiRepository.create(api());
        apiRepository.create(api2());

        Set<Api> apis = apiRepository.findAll();
        Assert.assertEquals(2, apis.size());
    }

    @Test
    public void shouldSaveMember() throws TechnicalException {
        Api apiToCreate = api();
        apiRepository.create(apiToCreate);
        apiRepository.saveMember(apiToCreate.getId(), "user@gravitee.io", MembershipType.PRIMARY_OWNER);

        Set<Api> apis = apiRepository.findAll();
        Assert.assertEquals(1, apis.size());
    }

    private Api api() {
        Api api = new Api();
        api.setId("my-api");
        api.setName("My API");
        api.setCreatedAt(new Date());
        api.setUpdatedAt(new Date());
        api.setDeployedAt(new Date());
        api.setLifecycleState(LifecycleState.STARTED);
        api.setVisibility(Visibility.PUBLIC);
        return api;
    }

    private Api api2() {
        Api api = new Api();
        api.setId("my-api2");
        api.setName("My API 2");
        api.setCreatedAt(new Date());
        api.setUpdatedAt(new Date());
        api.setDeployedAt(new Date());
        api.setLifecycleState(LifecycleState.STARTED);
        api.setVisibility(Visibility.PUBLIC);
        return api;
    }
}
