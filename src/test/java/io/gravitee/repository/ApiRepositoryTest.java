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
package io.gravitee.repository;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.management.model.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.Set;

import static io.gravitee.repository.utils.DateUtils.parse;

public class ApiRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/api-tests/";
    }

    private User createUser(String userName) throws Exception {
        User user = new User();
        user.setUsername(userName);
        user.setEmail(userName + "@itest.test");
        return userRepository.create(user);
    }

    @Test
    public void createApiTest() throws Exception {
        String apiName = "sample-new";

        Api api = new Api();
        api.setId(apiName);
        api.setName(apiName);
        api.setVersion("1");
        api.setLifecycleState(LifecycleState.STOPPED);
        api.setVisibility(Visibility.PRIVATE);
        api.setDefinition("{}");
        api.setCreatedAt(parse("11/02/2016"));
        api.setUpdatedAt(parse("12/02/2016"));

        apiRepository.create(api);

        Optional<Api> optional = apiRepository.findById(apiName);
        Assert.assertTrue("Api saved not found", optional.isPresent());

        Api apiSaved = optional.get();
        Assert.assertEquals("Invalid saved api version.", api.getVersion(), apiSaved.getVersion());
        Assert.assertEquals("Invalid api lifecycle.", api.getLifecycleState(), apiSaved.getLifecycleState());
        Assert.assertEquals("Invalid api private api status.", api.getVisibility(), apiSaved.getVisibility());
        Assert.assertEquals("Invalid api definition.", api.getDefinition(), apiSaved.getDefinition());
        Assert.assertEquals("Invalid api createdAt.", api.getCreatedAt(), apiSaved.getCreatedAt());
        Assert.assertEquals("Invalid api updateAt.", api.getUpdatedAt(), apiSaved.getUpdatedAt());
    }

    @Test
    public void findByIdTest() throws Exception {
        Optional<Api> optional = apiRepository.findById("api1");
        Assert.assertTrue("Find api by name return no result ", optional.isPresent());
    }

    @Test
    public void findByIdMissingTest() throws Exception {
        Optional<Api> optional = apiRepository.findById("findByNameMissing");
        Assert.assertFalse("Find api by name on missing api return a result", optional.isPresent());
    }

    @Test
    public void findByMemberTest() throws Exception {
        Set<Api> apis = apiRepository.findByMember("toto", MembershipType.PRIMARY_OWNER, null);
        Assert.assertNotNull(apis);
        Assert.assertEquals("Invalid api result in findByMember", 2, apis.size());
    }

    @Test
    public void findByUserTestAndPrivateApi() throws Exception {
        Set<Api> apis = apiRepository.findByMember("toto", MembershipType.PRIMARY_OWNER, Visibility.PRIVATE);
        Assert.assertNotNull(apis);
        Assert.assertEquals("Invalid api result in findByMember", 1, apis.size());
    }

    @Test
    public void findAllTest() throws Exception {
        Set<Api> apis = apiRepository.findAll();

        Assert.assertNotNull(apis);
        Assert.assertFalse("Fail to resolve api in findAll", apis.isEmpty());
    }

    @Test
    public void deleteApiTest() throws Exception {
        int nbApiBefore = apiRepository.findAll().size();
        apiRepository.delete("api1");
        int nbApiAfter = apiRepository.findAll().size();

        Assert.assertEquals(nbApiBefore - 1, nbApiAfter);
    }
}
