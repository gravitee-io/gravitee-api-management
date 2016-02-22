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
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.*;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
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

    @Autowired
    private UserRepository userRepository;

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
        Application applicationToCreate = application();
        applicationRepository.create(applicationToCreate);

        User user = new User();
        user.setUsername("user@gravitee.io");
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());

        userRepository.create(user);
        applicationRepository.saveMember(applicationToCreate.getId(), user.getUsername(), MembershipType.PRIMARY_OWNER);

        Membership member = applicationRepository.getMember(applicationToCreate.getId(), user.getUsername());
        Assert.assertEquals(MembershipType.PRIMARY_OWNER, member.getMembershipType());
        Assert.assertEquals(user.getUsername(), member.getUser().getUsername());

        Collection<Membership> members = applicationRepository.getMembers(applicationToCreate.getId(), MembershipType.PRIMARY_OWNER);
        Assert.assertEquals(1, members.size());

        members = applicationRepository.getMembers(applicationToCreate.getId(), MembershipType.OWNER);
        Assert.assertEquals(0, members.size());
    }

    @Test
    public void shouldSaveMultipleMembers() throws TechnicalException {
        Application applicationToCreate = application();
        applicationRepository.create(applicationToCreate);

        User user = new User();
        user.setUsername("user@gravitee.io");
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());

        User user2 = new User();
        user2.setUsername("user2@gravitee.io");
        user2.setCreatedAt(new Date());
        user2.setUpdatedAt(new Date());

        userRepository.create(user);
        userRepository.create(user2);
        applicationRepository.saveMember(applicationToCreate.getId(), user.getUsername(), MembershipType.PRIMARY_OWNER);
        applicationRepository.saveMember(applicationToCreate.getId(), user2.getUsername(), MembershipType.OWNER);

        Membership member = applicationRepository.getMember(applicationToCreate.getId(), user.getUsername());
        Assert.assertEquals(MembershipType.PRIMARY_OWNER, member.getMembershipType());
        Assert.assertEquals(user.getUsername(), member.getUser().getUsername());

        Collection<Membership> members = applicationRepository.getMembers(applicationToCreate.getId(), MembershipType.PRIMARY_OWNER);
        Assert.assertEquals(1, members.size());

        members = applicationRepository.getMembers(applicationToCreate.getId(), MembershipType.OWNER);
        Assert.assertEquals(1, members.size());
    }

    @Test
    public void shouldUpdateMembershipRole() throws TechnicalException {
        Application applicationToCreate = application();
        applicationRepository.create(applicationToCreate);

        User user = new User();
        user.setUsername("user@gravitee.io");
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());

        userRepository.create(user);
        applicationRepository.saveMember(applicationToCreate.getId(), user.getUsername(), MembershipType.PRIMARY_OWNER);

        applicationRepository.saveMember(applicationToCreate.getId(), user.getUsername(), MembershipType.USER);

        Membership member = applicationRepository.getMember(applicationToCreate.getId(), user.getUsername());
        Assert.assertEquals(MembershipType.USER, member.getMembershipType());
    }

    @Test
    public void shouldDeleteMember() throws TechnicalException {
        Application applicationToCreate = application();
        applicationRepository.create(applicationToCreate);

        User user = new User();
        user.setUsername("user@gravitee.io");
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());

        User user2 = new User();
        user2.setUsername("user2@gravitee.io");
        user2.setCreatedAt(new Date());
        user2.setUpdatedAt(new Date());

        userRepository.create(user);
        userRepository.create(user2);
        applicationRepository.saveMember(applicationToCreate.getId(), user.getUsername(), MembershipType.PRIMARY_OWNER);
        applicationRepository.saveMember(applicationToCreate.getId(), user2.getUsername(), MembershipType.OWNER);

        applicationRepository.deleteMember(applicationToCreate.getId(), user2.getUsername());

        Collection<Membership> members = applicationRepository.getMembers(applicationToCreate.getId(), MembershipType.PRIMARY_OWNER);
        Assert.assertEquals(1, members.size());

        members = applicationRepository.getMembers(applicationToCreate.getId(), MembershipType.OWNER);
        Assert.assertTrue(members.isEmpty());
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
