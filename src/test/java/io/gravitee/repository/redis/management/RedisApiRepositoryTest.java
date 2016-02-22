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
public class RedisApiRepositoryTest extends AbstractRedisTest {

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private UserRepository userRepository;

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

        User user = new User();
        user.setUsername("user@gravitee.io");
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());

        userRepository.create(user);
        apiRepository.saveMember(apiToCreate.getId(), user.getUsername(), MembershipType.PRIMARY_OWNER);

        Membership member = apiRepository.getMember(apiToCreate.getId(), user.getUsername());
        Assert.assertEquals(MembershipType.PRIMARY_OWNER, member.getMembershipType());
        Assert.assertEquals(user.getUsername(), member.getUser().getUsername());

        Collection<Membership> members = apiRepository.getMembers(apiToCreate.getId(), MembershipType.PRIMARY_OWNER);
        Assert.assertEquals(1, members.size());

        members = apiRepository.getMembers(apiToCreate.getId(), MembershipType.OWNER);
        Assert.assertEquals(0, members.size());
    }

    @Test
    public void shouldSaveMultipleMembers() throws TechnicalException {
        Api apiToCreate = api();
        apiRepository.create(apiToCreate);

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
        apiRepository.saveMember(apiToCreate.getId(), user.getUsername(), MembershipType.PRIMARY_OWNER);
        apiRepository.saveMember(apiToCreate.getId(), user2.getUsername(), MembershipType.OWNER);

        Membership member = apiRepository.getMember(apiToCreate.getId(), user.getUsername());
        Assert.assertEquals(MembershipType.PRIMARY_OWNER, member.getMembershipType());
        Assert.assertEquals(user.getUsername(), member.getUser().getUsername());

        Collection<Membership> members = apiRepository.getMembers(apiToCreate.getId(), MembershipType.PRIMARY_OWNER);
        Assert.assertEquals(1, members.size());

        members = apiRepository.getMembers(apiToCreate.getId(), MembershipType.OWNER);
        Assert.assertEquals(1, members.size());
    }

    @Test
    public void shouldUpdateMembershipRole() throws TechnicalException {
        Api apiToCreate = api();
        apiRepository.create(apiToCreate);

        User user = new User();
        user.setUsername("user@gravitee.io");
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());

        userRepository.create(user);
        apiRepository.saveMember(apiToCreate.getId(), user.getUsername(), MembershipType.PRIMARY_OWNER);

        apiRepository.saveMember(apiToCreate.getId(), user.getUsername(), MembershipType.USER);

        Membership member = apiRepository.getMember(apiToCreate.getId(), user.getUsername());
        Assert.assertEquals(MembershipType.USER, member.getMembershipType());
    }

    @Test
    public void shouldDeleteMember() throws TechnicalException {
        Api apiToCreate = api();
        apiRepository.create(apiToCreate);

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
        apiRepository.saveMember(apiToCreate.getId(), user.getUsername(), MembershipType.PRIMARY_OWNER);
        apiRepository.saveMember(apiToCreate.getId(), user2.getUsername(), MembershipType.OWNER);

        apiRepository.deleteMember(apiToCreate.getId(), user2.getUsername());

        Collection<Membership> members = apiRepository.getMembers(apiToCreate.getId(), MembershipType.PRIMARY_OWNER);
        Assert.assertEquals(1, members.size());

        members = apiRepository.getMembers(apiToCreate.getId(), MembershipType.OWNER);
        Assert.assertTrue(members.isEmpty());
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
