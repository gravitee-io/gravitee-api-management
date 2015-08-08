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
package io.gravitee.repositories.mongodb.internal;

import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.gravitee.repositories.mongodb.RepositoryConfiguration;
import io.gravitee.repositories.mongodb.internal.api.ApiMongoRepository;
import io.gravitee.repositories.mongodb.internal.model.ApiMongo;
import io.gravitee.repositories.mongodb.internal.model.TeamMongo;
import io.gravitee.repositories.mongodb.internal.model.UserMongo;
import io.gravitee.repositories.mongodb.internal.team.TeamMongoRepository;
import io.gravitee.repositories.mongodb.internal.user.UserMongoRepository;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { RepositoryConfiguration.class })
public class ApiRepositoryTest {

	@Autowired
	private ApiMongoRepository apiRepository;

	@Autowired
	private UserMongoRepository userRepository;
	
	@Autowired
	private TeamMongoRepository teamRepository;
	
	
	@Test
	public void createApiTest() {

		try {
			UserMongo creator = new UserMongo();
			creator.setMail("sample@mail.com");
			creator.setName("myusername");
			userRepository.save(creator);

			TeamMongo owner = new TeamMongo();
			owner.setName("myteamname");
			owner.setDescription("Team used for unit tests");
			teamRepository.save(owner);
			
			ApiMongo api = new ApiMongo();
			api.setName("sample");
			api.setVersion("1");
			api.setCreator(creator);
			api.setOwner(owner);

			apiRepository.save(api);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	@Test
	public void findByCreatorNameTest() {
		List<ApiMongo> apis = apiRepository.findByCreator("testcaseusername");
		System.out.println(apis);
	
		Assert.assertNotNull(apis);
	}
	
	
	@Test
	public void findByUserTest() {
		List<ApiMongo> apis = apiRepository.findByUser("user-350ea58f-e659-44b5-ba7a-a537900bf757");
		System.out.println(apis);
	
		Assert.assertNotNull(apis);
	}
	
	@Test
	public void findByTeamTest() {
		List<ApiMongo> apis = apiRepository.findByTeam("testcaseteamname");
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
		ApiMongo api = apiRepository.findOne("sample");
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
	}

}
