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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.*;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

public class ApiRepositoryTest extends AbstractMongoDBTest {

	private final static Logger logger = LoggerFactory.getLogger(ApiRepositoryTest.class);
	
	@Autowired
	private ApiRepository apiRepository;
	
	@Autowired
	private UserRepository userRepository;

    @Override
    protected String getTestCasesPath() {
        return "/data/api-tests/";
    }
	
	private User createUser(String userName) throws Exception{
		User user = new User();
		user.setUsername(userName);
		user.setEmail(userName+"@itest.test");
		return userRepository.create(user);
	}
	
	@Test
	public void createApiTest() {
		try {
			String apiName = "sample-" + new Date().getTime();
			
			Api api = new Api();
			api.setId(apiName);
			api.setName(apiName);
			api.setVersion("1");
			api.setLifecycleState(LifecycleState.STOPPED);
			api.setVisibility(Visibility.PRIVATE);
			api.setDefinition("{}");
			api.setCreatedAt(new Date());
			api.setUpdatedAt(new Date());
			
			apiRepository.create(api);
					
			Optional<Api> optional = apiRepository.findById(apiName);
			Assert.assertTrue("Api saved not found", optional.isPresent());
			
			Api apiSaved = optional.get();
			Assert.assertEquals("Invalid saved api version.", 		api.getVersion(), apiSaved.getVersion());
			Assert.assertEquals("Invalid api lifecycle.", 			api.getLifecycleState(), apiSaved.getLifecycleState());
			Assert.assertEquals("Invalid api private api status.", 	api.getVisibility(), apiSaved.getVisibility());
			Assert.assertEquals("Invalid api definition.", 			api.getDefinition(), apiSaved.getDefinition());
			Assert.assertEquals("Invalid api createdAt.", 			api.getCreatedAt(), apiSaved.getCreatedAt());
			Assert.assertEquals("Invalid api updateAt.", 			api.getUpdatedAt(), apiSaved.getUpdatedAt());
			
		} catch (Exception e) {
			logger.error("Error while testing createApi", e);
			Assert.fail("Error while testing createApi");
		}
	}

	@Test
	public void findByIdTest() {
		try {
			Optional<Api> optional = apiRepository.findById("findByNameOk");
			Assert.assertTrue("Find api by name return no result ", optional.isPresent());
		} catch (Exception e) {
			logger.error("Error while calling findById", e);
			Assert.fail("Error while calling findById");
		}
	}
	
	@Test
	public void findByIdMissingTest() {
		try {
			Optional<Api> optional = apiRepository.findById("findByNameMissing");
			Assert.assertFalse("Find api by name on missing api return a result", optional.isPresent());
		} catch (Exception e) {
			logger.error("Error while calling findById on missing api", e);
			Assert.fail("Error while calling findById on missing api");
		}
	}
	
	@Test
	public void findByMemberTest() {
		try {
			Set<Api> apis = apiRepository.findByMember("findByUserTest", MembershipType.PRIMARY_OWNER, null);
			Assert.assertNotNull(apis);
			Assert.assertEquals("Invalid api result in findByMember", 2, apis.size());
		} catch (Exception e) {
			logger.error("Error while finding api by user", e);
			Assert.fail("Error while finding api by user");
		}
	}

	@Test
	public void findByUserTestAndPrivateApi() {
		try {
			Set<Api> apis = apiRepository.findByMember("findByUserTest", MembershipType.PRIMARY_OWNER, Visibility.PRIVATE);
			Assert.assertNotNull(apis);
			Assert.assertEquals("Invalid api result in findByMember", 1, apis.size());
		} catch (Exception e) {
			logger.error("Error while finding api by user", e);
			Assert.fail("Error while finding api by user");
		}
	}
	
	@Test
	public void findAllTest() {
		try {
			Set<Api> apis = apiRepository.findAll();
			
			Assert.assertNotNull(apis);
			Assert.assertFalse("Fail to resolve api in findAll", apis.isEmpty());
		} catch (Exception e) {
			logger.error("Error while finding all apis", e);
			Assert.fail("Error while finding all apise");
		}
	}

	@Test
	public void deleteApiTest() {
		try {
			int nbApiBefore = apiRepository.findAll().size();
			apiRepository.delete("findByNameOk");
			int nbApiAfter = apiRepository.findAll().size();
	
			Assert.assertEquals(nbApiBefore -1, nbApiAfter);
		} catch (Exception e) {
			logger.error("Error while deleting api", e);
			Assert.fail("Error while deleting api");
		}
	}
}
