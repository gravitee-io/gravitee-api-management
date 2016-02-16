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
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.MembershipType;
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
public class RedisApplicationRepositoryTest extends AbstractRedisTest {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Test
    public void shouldCreateApplication() throws TechnicalException {
        Application application = application();

        applicationRepository.create(application);
    }

    @Test
    public void shouldFindApplication() throws TechnicalException {
        Application application = application();

        applicationRepository.create(application);

        Optional<Application> optApplication = applicationRepository.findById(application.getId());
        Assert.assertTrue(optApplication.isPresent());
        Assert.assertEquals(application, optApplication.get());
    }

    @Test
    public void shouldUpdateApplication() throws TechnicalException {
        Application application = application();

        applicationRepository.create(application);

        application.setName("My application 2");
        applicationRepository.update(application);

        Optional<Application> optApplication = applicationRepository.findById(application.getId());
        Assert.assertEquals(application.getName(), optApplication.get().getName());
    }

    @Test
    public void shouldDeleteApi() throws TechnicalException {
        Application application = application();

        applicationRepository.create(application);

        applicationRepository.delete(application.getId());

        Optional<Application> optApplication = applicationRepository.findById(application.getId());
        Assert.assertFalse(optApplication.isPresent());
    }

    @Test
    public void shouldFindAll() throws TechnicalException {
        applicationRepository.create(application());
        applicationRepository.create(application2());

        Set<Application> apis = applicationRepository.findAll();
        Assert.assertEquals(2, apis.size());
    }

    @Test
    public void shouldSaveMember() throws TechnicalException {
        Application apiToCreate = application();
        applicationRepository.create(apiToCreate);
        applicationRepository.saveMember(apiToCreate.getId(), "user@gravitee.io", MembershipType.PRIMARY_OWNER);

        Set<Application> apis = applicationRepository.findAll();
        Assert.assertEquals(1, apis.size());
    }

    private Application application() {
        Application application = new Application();
        application.setId("my-app");
        application.setName("My Application");
        application.setType("App type");
        application.setCreatedAt(new Date());
        application.setUpdatedAt(new Date());
        return application;
    }

    private Application application2() {
        Application application = new Application();
        application.setId("my-app2");
        application.setName("My Application é");
        application.setType("App type");
        application.setCreatedAt(new Date());
        application.setUpdatedAt(new Date());
        return application;
    }
}
