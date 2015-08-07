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
package io.gravitee.repositories.mongodb;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.gravitee.repository.api.ApiRepository;
import io.gravitee.repository.api.UserRepository;
import io.gravitee.repository.model.Api;
import io.gravitee.repository.model.LifecycleState;
import io.gravitee.repository.model.OwnerType;
import io.gravitee.repository.model.User;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { RepositoryConfiguration.class })
public class ApiRepositoryTest {

	@Autowired
	private ApiRepository apiRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	private Logger Logger = LoggerFactory.getLogger(ApiRepositoryTest.class);

	private User createUser(String userName){

		User user = new User();
		user.setUsername(userName);
		user.setMail(userName+"@itest.test");
		return userRepository.create(user);
	}
	
	@Test
	public void createApiTest() {

		try {
			User owner = createUser("user-"+UUID.randomUUID());

			Api api = new Api();
			api.setName("sample");
			api.setVersion("1");
			api.setLifecycleState(LifecycleState.STOPPED);
			api.setPrivate(false);
			api.setPublicURI(new URI("/public/sample/"));
			api.setTargetURI(new URI("/target/sample/"));
			api.setCreatedAt(new Date());
			api.setUpdatedAt(new Date());
			api.setOwner(owner.getUsername());
			api.setOwnerType(OwnerType.USER);
			
			apiRepository.create(api);
			
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("Error creating api", e);
			Assert.fail("API_CREATION_TEST_ERROR");
		}
	}
	
/*
	@Test
	public void findByCreatorIdTest() {
		List<ApiMongo> apis = apiRepository.findByCreatorId(new ObjectId("55c346a8d4c60e0dd348183d"));
		System.out.println(apis);
	
		Assert.assertNotNull(apis);
	}

	@Test
	public void findByCreatorNameTest() {
		Set<ApiMongo> apis = apiRepository.findByCreator("testcaseusername");
		System.out.println(apis);
	
		Assert.assertNotNull(apis);
	}
	
	@Test
	public void findByTeamNameTest() {
		Set<ApiMongo> apis = apiRepository.findByTeam("testcaseteamname");
		System.out.println(apis);
	
		Assert.assertNotNull(apis);
	}
	
	@Test
	public void findAllTest() {
		List<ApiMongo> apis = apiRepository.findAll();
		
		Assert.assertNotNull(apis);
		Assert.assertFalse("Fail to resolve api in findAll", apis.isEmpty());
	}	
	
	@Test
	public void findByNameTest() {
		ApiMongo api = apiRepository.findByName("sample");
		Assert.assertNotNull(api);
	}


	@Test
	public void findAll() {
		List<ApiMongo> apis = apiRepository.findAll();
		Assert.assertNotNull(apis);
	}

	@Test
	public void deleteApiTest() {
		apiRepository.delete("sample");
	}*/

}
